package io.meshcloud.dockerosb.persistence

import mu.KotlinLogging
import org.springframework.cloud.servicebroker.model.catalog.Catalog
import org.springframework.cloud.servicebroker.model.catalog.ServiceDefinition

private val log = KotlinLogging.logger { }

class CatalogRepository(
    private val yamlHandler: YamlHandler,
    private val gitHandler: GitHandler
) {

  fun getCatalog(): Catalog {
    val catalogYml = gitHandler.fileInRepo("catalog.yml")

    if (!catalogYml.isFile) {
      log.error { "Could not read catalog.yml file from '${catalogYml.absolutePath}'. Will start with an empty catalog." }
      return Catalog.builder().build()
    }

    val catalog = yamlHandler.readObject(catalogYml, YamlCatalog::class.java)

    return Catalog.builder()
        .serviceDefinitions(catalog.services)
        .build()
  }

  private class YamlCatalog(
      val services: List<ServiceDefinition>
  )
}
