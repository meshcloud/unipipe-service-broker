package io.meshcloud.dockerosb.service

import io.meshcloud.dockerosb.RemoteGitFixture
import io.meshcloud.dockerosb.config.GitConfig
import io.meshcloud.dockerosb.persistence.GitHandlerService
import org.eclipse.jgit.revwalk.RevCommit
import org.junit.Assert
import org.junit.Test
import org.junit.rules.TemporaryFolder

class GitHandlerServiceTest {

  @Test
  fun `can use main as a remote branch`() {
    val tmp = TemporaryFolder().apply {
      create()
    }

    try {
      val localGitPath = tmp.newFolder("git-local").absolutePath
      val remoteGitPath = tmp.newFolder("git-remote").absolutePath

      val gitConfig = GitConfig(
          localPath = localGitPath,
          remote = remoteGitPath,
          remoteBranch = "main",
          sshKey = null,
          username = null,
          password = null
      )

      RemoteGitFixture(remoteGitPath, gitConfig.remoteBranch).apply {
        initWithCatalog("src/test/resources/catalog.yml")
      }

      val sut = GitHandlerService(gitConfig)

      Assert.assertNotEquals(emptyList<RevCommit>(), sut.getLog());
    } finally {
      tmp.delete()
    }
  }
}