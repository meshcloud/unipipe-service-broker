package io.meshcloud.dockerosb.persistence

import io.meshcloud.dockerosb.config.CustomSshSessionFactory
import io.meshcloud.dockerosb.config.GitConfig
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.transport.SshSessionFactory
import org.eclipse.jgit.transport.URIish
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider
import java.io.File

interface GitHandler {

  fun pull()

  fun commit(filePaths: List<String>, commitMessage: String)

  fun rebaseAndPushAllCommittedChanges()

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

      gitConfig.remote?.let {
        ensureRemoteIsAdded(git, gitConfig)
        git.pull().setCredentialsProvider(UsernamePasswordCredentialsProvider(gitConfig.username, gitConfig.password)).call()
        git.checkout().setName(gitConfig.remoteBranch).call()
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
  }
}