package io.meshcloud.dockerosb.metrics

import io.meshcloud.dockerosb.findServiceByDefinitionId
import io.meshcloud.dockerosb.persistence.ServiceInstanceRepository
import org.springframework.cloud.servicebroker.model.catalog.Catalog
import java.time.Instant

abstract class MetricsProvider<T>(
  val catalog: Catalog,
  val serviceInstanceRepository: ServiceInstanceRepository
) {

  abstract fun getMetrics(serviceDefinitionId: String, from: Instant, to: Instant, index: Int): List<ServiceInstanceDatapoints<T>>

  fun canHandle(serviceDefinitionId: String): Boolean {
    return catalog.findServiceByDefinitionId(serviceDefinitionId) != null
  }

  /**
   * Total count of instances for which this provider provides metrics
   */
  fun totalInstanceCount(serviceDefinitionId: String): Int {
    return serviceInstanceRepository.findInstancesByServiceId(serviceDefinitionId).count()
  }
}