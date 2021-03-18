package io.meshcloud.dockerosb.service

import io.meshcloud.dockerosb.persistence.YamlHandler
import mu.KotlinLogging
import org.springframework.cloud.servicebroker.model.catalog.Catalog
import org.springframework.cloud.servicebroker.model.catalog.ServiceDefinition
import org.springframework.cloud.servicebroker.service.CatalogService
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono

private val log = KotlinLogging.logger { }

@Service
class GenericCatalogService(
  private val yamlHandler: YamlHandler,
  private val gitHandler: GitHandler
) : CatalogService {
  private var catalog: Catalog = parseCatalog(gitHandler, yamlHandler)

  private class YamlCatalog(
    val services: List<ServiceDefinition>
  )

  companion object {

    fun parseCatalog(gitHandler: GitHandler, yamlHandler: YamlHandler): Catalog {
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
  }

  fun getLatestCatalog() {
    gitHandler.pull()
    this.catalog = parseCatalog(gitHandler, yamlHandler)
  }

  /**
   * When ever the endpoint /v2/catalog is accessed  it pulls the catalog from the git repo
   */
  override fun getCatalog(): Mono<Catalog> {
    getLatestCatalog()
    return Mono.just(this.catalog)
  }

  /**
   * used to provide Catalog object to be used by other services internally
   */
  fun getCatalogInternal(): Catalog {
    getLatestCatalog()
    return this.catalog
  }

  override fun getServiceDefinition(serviceId: String?): Mono<ServiceDefinition> {

    return Mono.just(this.catalog.serviceDefinitions.singleOrNull { it.id == serviceId }!!)
  }
}