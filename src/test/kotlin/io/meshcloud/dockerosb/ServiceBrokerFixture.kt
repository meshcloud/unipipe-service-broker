package io.meshcloud.dockerosb


import io.meshcloud.dockerosb.config.GitConfig
import io.meshcloud.dockerosb.config.RetryConfig
import io.meshcloud.dockerosb.persistence.GitOperationContextFactory
import io.meshcloud.dockerosb.persistence.RetryingGitHandler
import io.meshcloud.dockerosb.persistence.YamlHandler
import io.meshcloud.dockerosb.service.GenericCatalogService
import org.apache.commons.io.FileUtils
import java.io.Closeable
import java.io.File

/**
 * A fixture for providing common context objects for tests
 */
class ServiceBrokerFixture(catalogPath: String) : Closeable {

  val yamlHandler: YamlHandler = YamlHandler()

  val localGitPath = "tmp/test/git"

  val gitConfig = GitConfig(
      localPath = localGitPath,
      remote = null,
      remoteBranch = "master",
      sshKey = null,
      username = null,
      password = null
  )

  val retryConfig = RetryConfig(
    remoteWriteAttempts = 1,
    remoteWriteBackOffDelay = 0
  )

  // note: it's important we place the initializer before the constructors below since we need to seed the repo with a
  // catalog before we access it internally
  init {
    FileUtils.copyFile(File(catalogPath), File("$localGitPath/catalog.yml"))
  }

  val gitHandler = RetryingGitHandler(gitConfig, retryConfig)

  val contextFactory = GitOperationContextFactory(gitHandler, yamlHandler)

  val catalogService: GenericCatalogService = GenericCatalogService(contextFactory)


  override fun close() {
    FileUtils.deleteDirectory(File(localGitPath))
  }
}