package io.meshcloud.dockerosb.persistence

import io.meshcloud.dockerosb.config.CustomSshSessionFactory
import io.meshcloud.dockerosb.config.GitConfig
import mu.KotlinLogging
import org.eclipse.jgit.api.CreateBranchCommand
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.transport.SshSessionFactory
import org.eclipse.jgit.transport.URIish
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider
import java.io.File

private val log = KotlinLogging.logger {}

interface GitHandler {

  fun pullFastForwardOnly()

  fun commitAllChanges(commitMessage: String)

  fun synchronizeWithRemoteRepository()

  fun fileInRepo(path: String): File

  fun getLastCommitMessage(): String

  companion object {

    /**
     * It might be that the created git object has no remote configured. (e.g. in tests)
     * So we need to check for gitConfig.hasRemoteConfigured() before we do git operations
     * that require remote access.
     */
    fun getGit(gitConfig: GitConfig): Git {
      gitConfig.sshKey?.let {
        SshSessionFactory.setInstance(CustomSshSessionFactory(it))
      }

      val git = Git.init().setDirectory(File(gitConfig.localPath)).call()

      // setup default user info (could make this configurable)
      git.repository.config.apply {
        setString("user", null, "name", "OSB API")
        setString("user", null, "email", "unipipe@meshcloud.io")
        save()
      }

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
  }
}
