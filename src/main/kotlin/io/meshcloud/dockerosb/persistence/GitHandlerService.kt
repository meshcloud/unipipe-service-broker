package io.meshcloud.dockerosb.persistence

import io.meshcloud.dockerosb.config.CustomSshSessionFactory
import io.meshcloud.dockerosb.config.GitConfig
import mu.KotlinLogging
import org.eclipse.jgit.api.*
import org.eclipse.jgit.merge.MergeStrategy
import org.eclipse.jgit.revwalk.RevCommit
import org.eclipse.jgit.transport.RefSpec
import org.eclipse.jgit.transport.SshSessionFactory
import org.eclipse.jgit.transport.URIish
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider
import org.springframework.stereotype.Service
import java.io.File

private val log = KotlinLogging.logger {}

/**
 * Note: consumers should use this only via a [GitOperationContext] to manage concurrent access to the git repo
 */
open class GitHandlerService(
    private val gitConfig: GitConfig
) : GitHandler {

  private val git = initGit(gitConfig)

  override fun instancesDirectory(): File {
    return fileInRepo("instances")
  }

  override fun fileInRepo(path: String): File {
    return File(gitConfig.localPath, path)
  }

  override fun filesInRepo(path: String): List<File> {
    return File(gitConfig.localPath, path).walk().toList()
  }

  override fun getLastCommitMessage(): String {
    return git.log().setMaxCount(1).call().single().fullMessage
  }

  override fun instanceYmlRelativePath(instanceId: String): String {
    return "${instanceDirectoryRelativePath(instanceId)}/instance.yml"
  }

  override fun instanceDirectoryRelativePath(instanceId: String): String {
    return "instances/$instanceId"
  }

  override fun bindingDirectoryRelativePath(instanceId: String, bindingId: String): String {
    return "instances/$instanceId/bindings/$bindingId"
  }

  override fun bindingYmlRelativePathInRepo(instanceId: String, bindingId: String): String {
    return "${bindingDirectoryRelativePath(instanceId, bindingId)}/binding.yml"
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
  }

  override fun synchronizeWithRemoteRepository() {
    if (!gitConfig.hasRemoteConfigured()) {
      log.info { "synchronizeWithRemoteRepository called, but no remote is configured - skipping." }
      return
    }

    if (!hasLocalCommits()) {
      // note: we do also not execute a fetch in this case because if the unipipe-osb is just idling
      // there's no sense in us fetching from the remote all the time. Consumers can explicitly call
      // pullFastForwardOnly if they want an up to date copy
      log.info { "synchronizeWithRemoteRepository called, but no new local commits found - skipping." }
      return
    }

    log.info { "Starting synchronizeWithRemoteRepository" }

    try {
      fetchMergePush()

      log.info { "Completed synchronizeWithRemoteRepository" }
    } catch (ex: Exception) {
      log.error(ex) { "Failed synchronizeWithRemoteRepository" }
    }
  }

  private fun fetchMergePush(): Boolean {
    // fetch from the remote
    git.fetch()
        .apply {              
          remote = "origin" 

          gitConfig.username?.let {
            setCredentialsProvider(UsernamePasswordCredentialsProvider(gitConfig.username, gitConfig.password))
          }
        }
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

  fun hasLocalCommits(): Boolean {
    val origin = git.repository.resolve("origin/${gitConfig.remoteBranch}")
    val head = git.repository.resolve("HEAD")

    val range = git.log().addRange(origin, head).call()
    val count = range.count()

    if (count > 0) {
      log.info { "Your branch is ahead of 'origin/${gitConfig.remoteBranch}' by $count commit(s)." }
    }

    return count > 0
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

  protected open fun push() {
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
      try {
        gitConfig.sshKey?.let {
          SshSessionFactory.setInstance(CustomSshSessionFactory(it))
        }

        val git = Git.init().setDirectory(File(gitConfig.localPath)).call()

        // setup default user info (could make this configurable)
        setupDefaultUser(git)

        gitConfig.remote?.let {
          ensureRemoteIsAdded(git, gitConfig)

          // fetch remote
          git.fetch()
              .apply {
                refSpecs = listOf(
                    RefSpec("refs/heads/${gitConfig.remoteBranch}:refs/remotes/origin/${gitConfig.remoteBranch}")
                )

                gitConfig.username?.let {
                  setCredentialsProvider(UsernamePasswordCredentialsProvider(gitConfig.username, gitConfig.password))
                }
              }
              .call()

          // checkout a branch
          switchToBranchAndCreateIfMissing(git, gitConfig.remoteBranch)
        }

        return git

      } catch (ex: org.eclipse.jgit.api.errors.InvalidRemoteException) {
        // Log an explicit error for the operator, so it does not get lost deep inside a spring stacktrace
        log.error(ex) { "Could not pull from the origin repository: ${gitConfig.remote}. Review the configured repository URL is correct and the credential settings are authorized on the remote repository(e.g. the deploy key, username-pass)." }
        throw ex
      }
    }

    private fun setupDefaultUser(git: Git) {
      git.repository.config.apply {
        setString("user", null, "name", "OSB API")
        setString("user", null, "email", "unipipe@meshcloud.io")
        save()
      }
    }

    private fun ensureRemoteIsAdded(git: Git, gitConfig: GitConfig) {
      if (git.remoteList().call().isNotEmpty()) {
        return
      }

      git.remoteAdd()
          .apply {
            setName("origin")
            setUri(URIish(gitConfig.remote))
          }
          .call()
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
