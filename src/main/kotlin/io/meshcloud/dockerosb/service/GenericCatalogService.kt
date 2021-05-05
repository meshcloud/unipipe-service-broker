package io.meshcloud.dockerosb.service

import io.meshcloud.dockerosb.persistence.GitOperationContextFactory
import mu.KotlinLogging
import org.springframework.cloud.servicebroker.model.catalog.Catalog
import org.springframework.cloud.servicebroker.model.catalog.ServiceDefinition
import org.springframework.cloud.servicebroker.service.CatalogService
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono

@Service
class GenericCatalogService(
    private val contextFactory: GitOperationContextFactory
) : CatalogService {

  private lateinit var cachedCatalog: Catalog

  init {
    contextFactory.acquireContext().use { context ->
      val repository = context.buildCatalogRepository()

      this.cachedCatalog = repository.getCatalog()
    }
  }

  /**
   * When ever the endpoint /v2/catalog is accessed  it pulls the catalog from the git repo
   */
  override fun getCatalog(): Mono<Catalog> {
    contextFactory.acquireContext().use { context ->
      context.attemptToRefreshRemoteChanges()

      val repository = context.buildCatalogRepository()
      val catalog = repository.getCatalog()

      this.cachedCatalog = catalog

      return Mono.just(catalog)
    }
  }

  /**
   * Used to provide Catalog object to be used by other services internally.
   * Uses the cached catalog.
   */
  fun cachedServiceDefinitions(): List<ServiceDefinition> {
    return cachedCatalog.serviceDefinitions
  }

  override fun getServiceDefinition(serviceId: String?): Mono<ServiceDefinition> {
    val serviceDefinition = this.cachedCatalog.serviceDefinitions.singleOrNull { it.id == serviceId }!!

    return Mono.just(serviceDefinition)
  }
}