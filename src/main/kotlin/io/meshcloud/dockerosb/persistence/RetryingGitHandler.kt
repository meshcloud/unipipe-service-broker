package io.meshcloud.dockerosb.persistence

import io.meshcloud.dockerosb.config.GitConfig
import io.meshcloud.dockerosb.config.RetryConfig
import io.meshcloud.dockerosb.persistence.GitHandler.Companion.getGit
import mu.KotlinLogging
import org.eclipse.jgit.api.*
import org.eclipse.jgit.lib.Ref
import org.eclipse.jgit.merge.MergeStrategy
import org.eclipse.jgit.revwalk.RevCommit
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider
import org.springframework.retry.backoff.FixedBackOffPolicy
import org.springframework.retry.policy.SimpleRetryPolicy
import org.springframework.retry.support.RetryTemplate
import org.springframework.stereotype.Service
import java.io.File
import java.util.concurrent.atomic.AtomicBoolean

private val log = KotlinLogging.logger {}

@Service
class RetryingGitHandler(
    private val gitConfig: GitConfig,
    private val retryConfig: RetryConfig
) : GitHandler {

  /**
   * We assume there are new commits in git, in case there are some.
   * This prevents commits "laying around" at start time until a new
   * commit would set this flag.
   *
   * TODO we could determine this exactly here instead.
   */
  private val hasLocalCommits = AtomicBoolean(true)

  private val remoteWriteRetryTemplate = RetryTemplate().apply {
    setBackOffPolicy(FixedBackOffPolicy().apply { backOffPeriod = retryConfig.remoteWriteBackOffDelay })
    setRetryPolicy(SimpleRetryPolicy().apply { maxAttempts = retryConfig.remoteWriteAttempts })
  }

  private val git = getGit(gitConfig)

  override fun pullFastForwardOnly() {
    if (!gitConfig.hasRemoteConfigured()) {
      return
    }

    val pullResult = git.pull()
        .apply {
          remote = "origin"
          remoteBranchName = gitConfig.remoteBranch
          setFastForward(MergeCommand.FastForwardMode.FF_ONLY)

          gitConfig.username?.let {
            setCredentialsProvider(UsernamePasswordCredentialsProvider(gitConfig.username, gitConfig.password))
          }
        }
        .call()

    if (!pullResult.isSuccessful) {
      log.warn { "git pull --ff-only failed - operating on potentially stale state until next periodic sync" }
    }
  }

  /**
   * git add .
   * git commit -m "$commitMessage"
   */
  override fun commit(filePaths: List<String>, commitMessage: String) {
    internalAdd(filePaths)
    internalCommit(commitMessage)

    hasLocalCommits.set(true)
  }

  override fun synchronizeWithRemoteRepository() {
    if (!gitConfig.hasRemoteConfigured()) {
      log.info { "synchronizeWithRemoteRepository called, but no remote is configured." }
      return
    }

    log.info { "Executing synchronizeWithRemoteRepository:" }

    try {
      if (!hasLocalCommits.get()) {
        log.info { "No new local commits found - skipping." }
      } else {
        synchronizeWithRemoteRepositoryUsingRetryStrategy()
        log.info { "Completed synchronizeWithRemoteRepository" }
      }
    } catch (ex: Exception) {
      log.error(ex) { "Failed to synchronize with remote git repository" }

    }
  }

  override fun fileInRepo(path: String): File {
    return File(gitConfig.localPath, path)
  }

  override fun getLastCommitMessage(): String {
    return git.log().setMaxCount(1).call().single().fullMessage
  }

  private fun synchronizeWithRemoteRepositoryUsingRetryStrategy() {


    /**
     * Retry merge command execution, in case something goes wrong, not in terms of conflicts or similar,
     * but in case the rebase command itself could be properly executed, e.g. remote not reachable etc.
     */
    val merge = remoteWriteRetryTemplate.execute<MergeResult, Exception> { ctx ->
      try {
        git.fetch()
            .apply { remote = "origin" }
            .call()

        git.merge()
            .include(git.repository.resolve("origin/${gitConfig.remoteBranch}"))
            .apply {
              setFastForward(MergeCommand.FastForwardMode.FF) // fast-forward if possible, otherwise merge
              setCommit(true)
              setMessage("Auto-merged by UniPipe OSB\nattempt #${ctx.retryCount}")

              // unfortunately jgit does not support "git merge --recursive -X ours"... yet but it will
              // https://github.com/eclipse/jgit/commit/8210f29fe43ccd35e7d2ed3ed45a84a75b2717c4
              setStrategy(MergeStrategy.RECURSIVE) // we are
            }
            .call()


      } catch (ex: Exception) {
        log.error { "Merge attempt #${ctx.retryCount}: ${ex.message}." }
        throw ex
      }
    }

    when (merge.mergeStatus) {

      MergeResult.MergeStatus.CONFLICTING -> {
        val files = merge.conflicts.entries.map { it.key }
        log.warn { "Encountered conflicts in files $files. Will attempt to auto-resolve conflicts, preferring local changes." }

        // just check out our version of conflicting files
        git.checkout()
            .addPaths(files)
            .setStage(CheckoutCommand.Stage.OURS)
            .call()

        // stage everything
        git.add()
            .addFilepattern(".")
            .call()

        // commit, this does not need anny messages because they are already present
        git.commit()
            .call()
      }
      MergeResult.MergeStatus.CHECKOUT_CONFLICT,
      MergeResult.MergeStatus.FAILED -> {
        log.warn { "Merge failed with status ${merge.mergeStatus.name}. Will retry on next periodic sync." }
        cleanUp()
      }

      MergeResult.MergeStatus.ALREADY_UP_TO_DATE,
      MergeResult.MergeStatus.FAST_FORWARD,
      MergeResult.MergeStatus.FAST_FORWARD_SQUASHED,
      MergeResult.MergeStatus.MERGED,
      MergeResult.MergeStatus.MERGED_NOT_COMMITTED,
      MergeResult.MergeStatus.MERGED_SQUASHED,
      MergeResult.MergeStatus.MERGED_SQUASHED_NOT_COMMITTED -> {
        log.info { "Merge succeeded with status ${merge.mergeStatus.name}." }
      }

      MergeResult.MergeStatus.ABORTED,
      MergeResult.MergeStatus.NOT_SUPPORTED ->
        log.error { "Merge failed with unexpected status ${merge.mergeStatus.name}. Taking no further action. An operator needs to resolve conflicts on the remote repository." }

      null -> throw KotlinNullPointerException("merge.mergeStatus")
    }


    if (merge.mergeStatus.isSuccessful) {
      /**
       * Retry push command execution, in case something goes wrong, e.g. remote not reachable etc.
       * In case the push fails for all attempts an exception is thrown and we won't continue.
       */
      remoteWriteRetryTemplate.execute<Unit, Exception> { ctx ->
        try {
          push()
          hasLocalCommits.set(false)
          log.info { "Successfully pushed all commits." }
        } catch (ex: Exception) {
          log.error { "Push attempt #${ctx.retryCount}: ${ex.message}." }
          throw ex
        }
      }
    }
  }

  private fun internalAdd(filePaths: List<String>) {
    val addCommand = git.add()
    filePaths.forEach { addCommand.addFilepattern(it) }
    addCommand.call()
  }

  private fun internalCommit(commitMessage: String) {
    git.commit()
        .setMessage("OSB API: $commitMessage")
        .setAuthor("UniPipe OSB API", "osb@meshcloud.io")
        .call()
  }

  private fun push() {
    val pushCommand = git.push()

    gitConfig.username?.let {
      pushCommand.setCredentialsProvider(UsernamePasswordCredentialsProvider(gitConfig.username, gitConfig.password))
    }

    pushCommand.call()
  }

  private fun cleanUp() {
    git.clean()
        .setForce(true)
        .setCleanDirectories(true)
        .call()

    git.reset()
        .setMode(ResetCommand.ResetType.HARD)
        .setRef("HEAD")
        .call()
  }

  fun getLog(): MutableIterable<RevCommit> {
    return git.log().call()
  }
}