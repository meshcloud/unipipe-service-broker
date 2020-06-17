package io.meshcloud.dockerosb

import io.meshcloud.dockerosb.config.CatalogConfiguration
import io.meshcloud.dockerosb.config.GitConfig
import io.meshcloud.dockerosb.persistence.GitHandler
import io.meshcloud.dockerosb.persistence.YamlHandler
import org.apache.commons.io.FileUtils
import org.springframework.cloud.servicebroker.model.catalog.Catalog
import java.io.Closeable
import java.io.File

class ServiceBrokerFixture : Closeable {

  val yamlHandler: YamlHandler = YamlHandler()

  val localGitPath = "tmp/test/git"

  val gitConfig = GitConfig(
      localPath = localGitPath,
      remote = null,
      sshKey = null,
      username = null,
      password = null
  )

  val gitHandler = GitHandler(gitConfig)

  val catalog: Catalog

  init {
    FileUtils.copyFile(File("src/test/resources/catalog.yml"), File("$localGitPath/catalog.yml"))

    catalog = CatalogConfiguration.parseCatalog(gitHandler, yamlHandler)
  }

  override fun close() {
    FileUtils.deleteDirectory(File(localGitPath))
  }
}