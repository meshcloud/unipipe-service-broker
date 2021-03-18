package io.meshcloud.dockerosb


import io.meshcloud.dockerosb.config.GitConfig
import io.meshcloud.dockerosb.persistence.GitHandler
import io.meshcloud.dockerosb.persistence.SynchronizedGitHandlerWrapper
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
      remote = "fake-remote",
      remoteBranch = "master",
      sshKey = null,
      username = null,
      password = null
  )

  val gitHandler = SynchronizedGitHandlerWrapper(gitConfig)

  val catalogService: GenericCatalogService = GenericCatalogService(yamlHandler, gitHandler)

  init {
    FileUtils.copyFile(File(catalogPath), File("$localGitPath/catalog.yml"))
  }

  override fun close() {
    FileUtils.deleteDirectory(File(localGitPath))
  }
}