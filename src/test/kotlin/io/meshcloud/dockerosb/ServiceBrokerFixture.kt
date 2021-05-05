package io.meshcloud.dockerosb


import io.meshcloud.dockerosb.config.GitConfig
import io.meshcloud.dockerosb.persistence.GitOperationContextFactory
import io.meshcloud.dockerosb.persistence.GitHandlerService
import io.meshcloud.dockerosb.persistence.YamlHandler
import io.meshcloud.dockerosb.service.GenericCatalogService
import org.junit.rules.TemporaryFolder
import java.io.Closeable

/**
 * A fixture for providing common context objects for tests
 */
class ServiceBrokerFixture(catalogPath: String) : Closeable {
  private val tmp = TemporaryFolder().apply {
    create()
  }

  val yamlHandler: YamlHandler = YamlHandler()

  val localGitPath = tmp.newFolder("git-local").absolutePath
  val remoteGitPath = tmp.newFolder("git-remote").absolutePath

  val gitConfig = GitConfig(
      localPath = localGitPath,
      remote = remoteGitPath,
      remoteBranch = "master",
      sshKey = null,
      username = null,
      password = null
  )

  // note: it's important we place the initializer before the constructors below since we need to seed the repo with a
  // catalog before we access it internally
  val remote = RemoteGitFixture(remoteGitPath).apply {
    initWithCatalog(catalogPath)
  }

  val gitHandler = GitHandlerService(gitConfig)
  val contextFactory = GitOperationContextFactory(gitHandler, yamlHandler)

  val catalogService: GenericCatalogService = GenericCatalogService(contextFactory)

  override fun close() {
    this.tmp.delete()
  }
}

