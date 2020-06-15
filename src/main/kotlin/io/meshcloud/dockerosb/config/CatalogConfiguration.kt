package io.meshcloud.dockerosb.config

import io.meshcloud.dockerosb.persistence.GitHandler
import io.meshcloud.dockerosb.persistence.YamlHandler
import mu.KotlinLogging
import org.springframework.cloud.servicebroker.model.catalog.Catalog
import org.springframework.cloud.servicebroker.model.catalog.ServiceDefinition
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

private val log = KotlinLogging.logger { }

@Configuration
class CatalogConfiguration(
    private val yamlHandler: YamlHandler,
    private val gitHandler: GitHandler
) {

  @Bean
  fun catalog(): Catalog {
    gitHandler.pull()

    val statusYml = gitHandler.fileInRepo("catalog.yml")

    if (!statusYml.isFile) {
      log.error { "Could not read catalog.yml file from '${statusYml.absolutePath}'. Will start with an empty catalog." }
      return Catalog.builder().build()
    }


    val catalog = yamlHandler.readObject(statusYml, YamlCatalog::class.java)

    return Catalog.builder()
        .serviceDefinitions(catalog.services)
        .build()
  }

  private class YamlCatalog(
      val services: List<ServiceDefinition>
  )
}