package io.meshcloud.dockerosb.metrics.inplace

import io.meshcloud.dockerosb.findServiceByDefinitionId
import io.meshcloud.dockerosb.metrics.MetricType
import io.meshcloud.dockerosb.metrics.MetricsProvider.Companion.utcZoneId
import io.meshcloud.dockerosb.metrics.ServiceInstanceDatapoints
import io.meshcloud.dockerosb.persistence.ServiceInstanceRepository
import org.springframework.cloud.servicebroker.model.catalog.Catalog
import org.springframework.stereotype.Service
import java.time.Instant
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

/**
 * Sends the metrics of each instance in a paged fashion
 */
@Service
class InplaceMetricProvider(
    val catalog: Catalog,
    val serviceInstanceRepository: ServiceInstanceRepository
) : PaginatedInplaceMetricsProvider {

  override fun totalInstanceCount(serviceDefinitionId: String): Int {
    return serviceInstanceRepository.findInstancesByServiceId(serviceDefinitionId).count()
  }

  override fun getMetrics(serviceDefinitionId: String, from: Instant, to: Instant, index: Int): List<ServiceInstanceDatapoints<InplaceMetricModel>> {
    val instances = serviceInstanceRepository.findInstancesByServiceId(serviceDefinitionId)

    return if (instances.size > index) {
          @Suppress("UNCHECKED_CAST")
          serviceInstanceRepository.tryGetServiceInstanceMetrics(instances[index].serviceInstanceId, MetricType.INPLACE, from, to) as List<ServiceInstanceDatapoints<InplaceMetricModel>>
    } else {
      listOf()
    }
  }

  override fun canHandle(serviceDefinitionId: String): Boolean {
    return (catalog.findServiceByDefinitionId(serviceDefinitionId) != null)
  }
}