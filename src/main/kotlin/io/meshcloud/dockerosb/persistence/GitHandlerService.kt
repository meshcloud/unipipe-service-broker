package io.meshcloud.dockerosb.persistence

import io.meshcloud.dockerosb.config.CustomSshSessionFactory
import io.meshcloud.dockerosb.config.GitConfig
import mu.KotlinLogging
import org.eclipse.jgit.api.*
import org.eclipse.jgit.merge.MergeStrategy
import org.eclipse.jgit.revwalk.RevCommit
import org.eclipse.jgit.transport.SshSessionFactory
import org.eclipse.jgit.transport.URIish
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider
import org.springframework.stereotype.Service
import java.io.File
import java.util.concurrent.atomic.AtomicBoolean

private val log = KotlinLogging.logger {}

@Service
class GitHandlerService(
    private val gitConfig: GitConfig
) : GitHandler {

  /**
   * Signal if we have local commits that we want to sync or not.
   *
   * At startup we just assume there are new commits in git.
   * This prevents commits "laying around" at start time until a new
   * commit would set this flag.
   */
  private val hasLocalCommits = AtomicBoolean(true)

  private val git = initGit(gitConfig)

  override fun fileInRepo(path: String): File {
    return File(gitConfig.localPath, path)
  }

  override fun getLastCommitMessage(): String {
    return git.log().setMaxCount(1).call().single().fullMessage
  }

  /**
   * git pull --ff-only
   *
   * Consumers can call this as an best effort attempt for retrieving a state that is "as fresh as possible".
   * This will not update the repository in case of pending local changes.
   */
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
  override fun commitAllChanges(commitMessage: String) {
    addAllChanges()
    commitAsOsbApi(commitMessage)

    hasLocalCommits.set(true)
  }

  override fun synchronizeWithRemoteRepository() {
    if (!gitConfig.hasRemoteConfigured()) {
      log.info { "synchronizeWithRemoteRepository called, but no remote is configured - skipping." }
      return
    }

    if (!hasLocalCommits.get()) {
      // note: we do also not execute a fetch in this case because if the unipipe-osb is just idling
      // there's no sense in us fetching from the remote all the time. Consumers can explicitly call
      // pullFastForwardOnly if they want an up to date copy
      log.info { "synchronizeWithRemoteRepository called, but no new local commits found - skipping." }
      return
    }

    log.info { "Starting synchronizeWithRemoteRepository" }

    try {
      val pushedSuccessfully = fetchMergePush()
      if (pushedSuccessfully) {
        hasLocalCommits.set(false)
      }

      log.info { "Completed synchronizeWithRemoteRepository" }
    } catch (ex: Exception) {
      log.error(ex) { "Failed synchronizeWithRemoteRepository" }
    }
  }

  private fun fetchMergePush(): Boolean {
    // fetch from the remote
    git.fetch()
        .apply { remote = "origin" }
        .call()

    // merge changes - this happens locally in our repository
    val merge = git.merge()
        .include(git.repository.resolve("origin/${gitConfig.remoteBranch}"))
        .apply {
          setFastForward(MergeCommand.FastForwardMode.FF) // fast-forward if possible, otherwise merge
          setCommit(true)
          setMessage("OSB API: auto-merging remote changes")

          /**
           * Recursive is the default merge strategy when running git on the command line.
           * Recursive strategy recursively merges the two trees paths and also file contents.
           * For our purposes, we'd ideally want semantics like "git merge --recursive -X ours" but configuring that
           * is not yet supported by jgit https://github.com/eclipse/jgit/commit/8210f29fe43ccd35e7d2ed3ed45a84a75b2717c4
           * Instead we provide our own merge resolution logic in [resolveMergeConflictsUsingOurs] below
           */
          setStrategy(MergeStrategy.RECURSIVE)
        }
        .call()

    when (merge.mergeStatus) {
      MergeResult.MergeStatus.CONFLICTING -> {
        val files = merge.conflicts.entries.map { it.key }
        resolveMergeConflictsUsingOurs(files)
      }
      MergeResult.MergeStatus.CHECKOUT_CONFLICT,
      MergeResult.MergeStatus.FAILED -> {
        log.warn { "Merge failed with status ${merge.mergeStatus.name}. Will retry on next periodic sync." }
        logFailedMergeDetails(merge)
        recoverFromFailedMerge()
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
      MergeResult.MergeStatus.NOT_SUPPORTED -> {
        log.error { "Merge failed with unexpected status ${merge.mergeStatus.name}. Taking no further action. An operator needs to resolve conflicts on the remote repository." }
        recoverFromFailedMerge()
      }
      null -> throw IllegalStateException("merge.mergeStatus was null")
    }

    // when successful, push back to the remote repo
    // pushing to the remote repository can fail if the remote repo sees changes in the meantime
    if (merge.mergeStatus.isSuccessful) {
      try {
        push()
        log.info { "Successfully pushed all commits." }
        return true
      } catch (ex: Exception) {
        log.error(ex) { "Failed to push to remote. Will fetch and merge remote changes on next periodic sync." }
      }
    }

    return false
  }

  private fun resolveMergeConflictsUsingOurs(files: List<String>) {
    log.warn { "Encountered conflicts in files $files. Will attempt to auto-resolve conflicts, preferring local changes." }

    // just check out our version of conflicting files
    git.checkout()
        .addPaths(files)
        .setStage(CheckoutCommand.Stage.OURS)
        .call()

    // stage everything
    addAllChanges()

    // commit, this does not need anny messages because they are already present
    git.commit()
        .call()
  }

  private fun addAllChanges() {
    // note: jgit is a little quirky here and does not work exactly like the git command line
    git.add()
        .apply {
          isUpdate = false    // add modified + new files
          addFilepattern(".")
        }
        .call()

    git.add()
        .apply {
          isUpdate = true   // add modified + removed files
          addFilepattern(".")
        }
        .call()

  }

  private fun commitAsOsbApi(commitMessage: String) {
    git.commit()
        .setMessage("OSB API: $commitMessage")
        .call()
  }

  private fun push() {
    val pushCommand = git.push()

    gitConfig.username?.let {
      pushCommand.setCredentialsProvider(UsernamePasswordCredentialsProvider(gitConfig.username, gitConfig.password))
    }

    pushCommand.call()
  }

  private fun recoverFromFailedMerge() {
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

  companion object {

    /**
     * It might be that the created git object has no remote configured. (e.g. in tests)
     * So we need to check for gitConfig.hasRemoteConfigured() before we do git operations
     * that require remote access.
     */
    fun initGit(gitConfig: GitConfig): Git {
      gitConfig.sshKey?.let {
        SshSessionFactory.setInstance(CustomSshSessionFactory(it))
      }

      val git = Git.init().setDirectory(File(gitConfig.localPath)).call()

      // setup default user info (could make this configurable)
      setupDefaultUser(git)

      gitConfig.remote?.let {
        ensureRemoteIsAdded(git, gitConfig)
        val pull = git.pull()

        gitConfig.username?.let {
          pull.setCredentialsProvider(UsernamePasswordCredentialsProvider(gitConfig.username, gitConfig.password))
        }

        pull.call()

        switchToBranchAndCreateIfMissing(git, gitConfig.remoteBranch)
      }

      return git
    }

    private fun setupDefaultUser(git: Git) {
      git.repository.config.apply {
        setString("user", null, "name", "OSB API")
        setString("user", null, "email", "unipipe@meshcloud.io")
        save()
      }
    }

    private fun ensureRemoteIsAdded(git: Git, gitConfig: GitConfig) {
      if (git.remoteList().call().isEmpty()) {
        val remoteAddCommand = git.remoteAdd()
        remoteAddCommand.setName("origin")
        remoteAddCommand.setUri(URIish(gitConfig.remote))
        remoteAddCommand.call()
      }
    }

    private fun switchToBranchAndCreateIfMissing(git: Git, branchName: String) {
      val exists = git.repository.refDatabase.refs.map { it.name }.contains("refs/heads/$branchName")
      if (exists) {
        log.info { "Branch $branchName exists." }
        git.checkout()
            .setName(branchName)
            .call()
      } else {
        log.info { "Branch $branchName does not exist locally. Creating it." }
        git.checkout()
            .setCreateBranch(true)
            .setName(branchName)
            .setUpstreamMode(CreateBranchCommand.SetupUpstreamMode.TRACK)
            .setStartPoint("origin/$branchName")
            .call()
      }
    }

    private fun logFailedMergeDetails(merge: MergeResult) {
      log.warn {
        " - failing paths: ${merge.failingPaths.map { "${it.key}: ${it.value}" }}"
      }
      log.warn {
        " - conflicts: ${merge.conflicts.map { it.key }}"
      }
      log.warn {
        " - checkout conflicts: ${merge.checkoutConflicts}"
      }
    }
  }
}