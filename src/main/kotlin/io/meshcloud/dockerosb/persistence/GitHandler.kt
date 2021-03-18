package io.meshcloud.dockerosb.persistence

import io.meshcloud.dockerosb.config.CustomSshSessionFactory
import io.meshcloud.dockerosb.config.GitConfig
import io.meshcloud.dockerosb.exceptions.GitCommandException
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.api.RebaseResult
import org.eclipse.jgit.api.ResetCommand
import org.eclipse.jgit.transport.SshSessionFactory
import org.eclipse.jgit.transport.URIish
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider
import java.io.File

open class GitHandler(
  private val gitConfig: GitConfig
) {

  open fun commit(
    filePaths: List<String>,
    commitMessage: String
  ) {
    getGit(gitConfig).use { git ->
      val addCommand = git.add()
      filePaths.forEach { addCommand.addFilepattern(it) }
      addCommand.call()
      git.commit()
        .setMessage("OSB API: $commitMessage")
        .setAuthor("Generic OSB API", "osb@meshcloud.io")
        .call()
    }
  }

  open fun push() {
    gitConfig.remote?.let {
      getGit(gitConfig).use { push(it) }
    }
  }

  private fun push(git: Git) {
    gitConfig.remote?.let {
      val pushCommand = git.push()
      gitConfig.username?.let {
        pushCommand.setCredentialsProvider(UsernamePasswordCredentialsProvider(gitConfig.username, gitConfig.password))
      }
      pushCommand.call()
    }
  }

  open fun rebase(): RebaseResult {

    return getGit(gitConfig).use { git ->
      gitConfig.remote.let {
        git.rebase()
          .setUpstream(gitConfig.remoteBranch)
          .call()
      }
    }
  }

  open fun pull() {

    if (gitConfig.remote == null) {
      return
    }

    getGit(gitConfig).use {
      val pullCommand = it.pull()
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
  }

  fun getLastCommitMessage(): String {
    return getGit(gitConfig).use {
      it.log().setMaxCount(1).call().single().fullMessage
    }
  }

  fun cleanUp() {
    getGit(gitConfig).use {
      it.clean().setForce(true).setCleanDirectories(true).call()
      it.reset().setMode(ResetCommand.ResetType.HARD).setRef("HEAD").call()
    }
  }

  fun fileInRepo(path: String): File {
    return File(gitConfig.localPath, path)
  }

  companion object {

    fun getGit(gitConfig: GitConfig): Git {
      gitConfig.sshKey?.let {
        SshSessionFactory.setInstance(CustomSshSessionFactory(it))
      }

      val git = Git.init().setDirectory(File(gitConfig.localPath)).call()

      gitConfig.remote?.let {
        ensureRemoteIsAdded(git, gitConfig)
      }

      // TODO
      //  do we need to do
      //  git branch --set-upstream-to=origin/${gitConfig.remoteBranch}
      //  to ensure tracking of the correct remote branch here as well?

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