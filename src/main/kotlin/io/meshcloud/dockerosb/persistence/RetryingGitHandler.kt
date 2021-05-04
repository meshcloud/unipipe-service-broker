package io.meshcloud.dockerosb.persistence

import io.meshcloud.dockerosb.config.GitConfig
import io.meshcloud.dockerosb.config.RetryConfig
import io.meshcloud.dockerosb.exceptions.GitCommandException
import io.meshcloud.dockerosb.persistence.GitHandler.Companion.getGit
import mu.KotlinLogging
import org.eclipse.jgit.api.RebaseResult
import org.eclipse.jgit.api.ResetCommand
import org.eclipse.jgit.errors.LockFailedException
import org.eclipse.jgit.revwalk.RevCommit
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider
import org.springframework.retry.backoff.FixedBackOffPolicy
import org.springframework.retry.policy.SimpleRetryPolicy
import org.springframework.retry.support.RetryTemplate
import org.springframework.stereotype.Service
import java.io.File
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.locks.ReentrantLock
import java.util.concurrent.locks.ReentrantReadWriteLock

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
   * TODO we could determine this exactly here instead.
   */
  private val hasNewCommits = AtomicBoolean(true)

  private val remoteWriteRetryTemplate = RetryTemplate().apply {
    setBackOffPolicy(FixedBackOffPolicy().apply { backOffPeriod = retryConfig.remoteWriteBackOffDelay })
    setRetryPolicy(SimpleRetryPolicy().apply { maxAttempts = retryConfig.remoteWriteAttempts })
  }

  private val git = getGit(gitConfig)

  /**
   * Acquire readLock to be sure that we don't conflict with the writeLock.
   */
  override fun pull() {
    if (!gitConfig.hasRemoteConfigured()) {
      return
    }

    internalPull()
  }

  /**
   * git add .
   * git commit -m "$commitMessage"
   */
  override fun commit(filePaths: List<String>, commitMessage: String) {
    internalAdd(filePaths)
    internalCommit(commitMessage)

    hasNewCommits.set(true)
  }

  /**
   * This will acquire the writeLock so all parallel access to
   * the writeLock and any access to the readLock will be delayed.
   *
   * If there are no new commits, we do nothing to keep to amount
   * of unnecessary calls to git small.
   */
  override fun rebaseAndPushAllCommittedChanges() {
    if (!gitConfig.hasRemoteConfigured()) {
      log.info { "Rebase/push called, but no remote is configured." }
      return
    }

    log.info { "Executing rebase/push:" }

    try {
      if (!hasNewCommits.get()) {
        log.info { "No commits found. Nothing has been pushed to remote git." }
      } else {
        internalRebaseAndPushAllCommittedChanges()
        log.info { "All recent commits have been pushed to remote git." }
      }
    } catch (ex: Exception) {
      log.error { "Failed to rebase and push changes." }

    }
  }

  override fun fileInRepo(path: String): File {
    return File(gitConfig.localPath, path)
  }

  override fun getLastCommitMessage(): String {
    return git.log().setMaxCount(1).call().single().fullMessage
  }

  private fun internalRebaseAndPushAllCommittedChanges() {
    var rebaseResult: RebaseResult
    var retryRebase: Boolean

    do {
      retryRebase = false

      /**
       * Retry rebase command execution, in case something goes wrong, not in terms of conflicts or similar,
       * but in case the rebase command itself could be properly executed, e.g. remote not reachable etc.
       * In case the rebase fails for all attempts an exception is thrown and we won't continue.
       */
      rebaseResult = remoteWriteRetryTemplate.execute<RebaseResult, Exception> { ctx ->
        try {
          git.rebase()
              .setUpstream(gitConfig.remoteBranch)
              .call()
        } catch (ex: Exception) {
          log.error { "Rebase attempt #${ctx.retryCount}: ${ex.message}." }
          throw ex
        }
      }

      if (!rebaseResult.status.isSuccessful) {
        when (rebaseResult.status) {

          // This should never occur, because we should never have uncomitted local changes
          // However we can resolve it easily.
          RebaseResult.Status.UNCOMMITTED_CHANGES -> {
            log.warn { "Rebase failed due to uncommitted changes: " }
            rebaseResult.uncommittedChanges.forEach { log.info { "  $it" } }

            log.warn { "Cleaning up repository and retry rebase." }
            cleanUp()
            retryRebase = true
          }

          RebaseResult.Status.FAILED -> {
            log.warn { "Rebase failed due to unknown reasons." }
            retryRebase = true //just retry we know that the original HEAD was restored already.
          }

          RebaseResult.Status.OK,
          RebaseResult.Status.UP_TO_DATE,
          RebaseResult.Status.FAST_FORWARD,
          RebaseResult.Status.STOPPED,
          RebaseResult.Status.CONFLICTS -> {
            log.info { "Rebase status: ${rebaseResult.status}. Taking no further action." }
          }

          RebaseResult.Status.ABORTED,
          RebaseResult.Status.INTERACTIVE_PREPARED,
          RebaseResult.Status.NOTHING_TO_COMMIT,
          RebaseResult.Status.STASH_APPLY_CONFLICTS,
          RebaseResult.Status.EDIT -> {
            log.warn { "Encountered unexpected rebase status: ${rebaseResult.status}. Taking no action." }
          }

          // make kotlin happy
          null -> {
            throw NullPointerException("rebaseResult.status was null")
          }
        }
      }
    } while (retryRebase)

    if (rebaseResult.status.isSuccessful) {
      log.info { "Successfully rebased." }

      /**
       * Retry push command execution, in case something goes wrong, e.g. remote not reachable etc.
       * In case the push fails for all attempts an exception is thrown and we won't continue.
       */
      remoteWriteRetryTemplate.execute<Unit, Exception> { ctx ->
        try {
          push()
          hasNewCommits.set(false)
          log.info { "Successfully pushed all commits." }
        } catch (ex: Exception) {
          log.error { "Push attempt #${ctx.retryCount}: ${ex.message}." }
          throw ex
        }
      }
    } else {
      /**
       * We know the rebase cannot be done automatically here.
       * So there are conflicts that need to be resolved.
       * TODO is there any strategy to resolve this?
       */
      log.error { "Could not rebase and push commits." }
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

  private fun internalPull() {
    val pullCommand = git.pull()
        .setRemote("origin")
        .setRemoteBranchName(gitConfig.remoteBranch)

    gitConfig.username?.let {
      pullCommand.setCredentialsProvider(UsernamePasswordCredentialsProvider(gitConfig.username, gitConfig.password))
    }
    val pullResult = pullCommand.call()

    if (!pullResult.isSuccessful) {
      throw GitCommandException("Git Pull failed.", null)
    }
  }

  fun getLog(): MutableIterable<RevCommit> {
    return git.log().call()
  }
}