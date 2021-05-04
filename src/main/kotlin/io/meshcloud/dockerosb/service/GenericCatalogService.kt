package io.meshcloud.dockerosb.service

import io.meshcloud.dockerosb.persistence.GitOperationContext
import io.meshcloud.dockerosb.persistence.GitOperationContextFactory
import mu.KotlinLogging
import org.springframework.cloud.servicebroker.model.catalog.Catalog
import org.springframework.cloud.servicebroker.model.catalog.ServiceDefinition
import org.springframework.cloud.servicebroker.service.CatalogService
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono

private val log = KotlinLogging.logger { }

@Service
class GenericCatalogService(
    private val contextFactory: GitOperationContextFactory
) : CatalogService {

  private lateinit var cachedCatalog: Catalog

  init {
    contextFactory.acquireContext().use { context ->
      this.cachedCatalog = fetchAndCacheCatalog(context)
    }
  }

  private class YamlCatalog(
      val services: List<ServiceDefinition>
  )

  /**
   * Fetches the latest service catalog from git
   */
  private fun fetchAndCacheCatalog(context: GitOperationContext): Catalog {
    context.gitHandler.pull()

    val catalog = parseCatalog(context)
    this.cachedCatalog = catalog

    return catalog
  }

  /**
   * When ever the endpoint /v2/catalog is accessed  it pulls the catalog from the git repo
   */
  override fun getCatalog(): Mono<Catalog> {
    contextFactory.acquireContext().use { context ->
      val catalog = fetchAndCacheCatalog(context)

      return Mono.just(catalog)
    }
  }

  /**
   * Used to provide Catalog object to be used by other services internally.
   * Uses the cached catalog.
   */
  fun getCatalogInternal(): Catalog {
    return cachedCatalog
  }

  override fun getServiceDefinition(serviceId: String?): Mono<ServiceDefinition> {
    val serviceDefinition = this.cachedCatalog.serviceDefinitions.singleOrNull { it.id == serviceId }!!

    return Mono.just(serviceDefinition)
  }

  companion object {
    fun parseCatalog(context: GitOperationContext): Catalog {
      val statusYml = context.gitHandler.fileInRepo("catalog.yml")

      if (!statusYml.isFile) {
        log.error { "Could not read catalog.yml file from '${statusYml.absolutePath}'. Will start with an empty catalog." }
        return Catalog.builder().build()
      }

      val catalog = context.yamlHandler.readObject(statusYml, YamlCatalog::class.java)

      return Catalog.builder()
          .serviceDefinitions(catalog.services)
          .build()
    }
  }

}