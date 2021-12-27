package io.meshcloud.dockerosb.metrics.gauge

import io.meshcloud.dockerosb.findServiceByDefinitionId
import io.meshcloud.dockerosb.findServiceByName
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
class GaugeMetricProvider(
    val catalog: Catalog,
    val serviceInstanceRepository: ServiceInstanceRepository
) : PaginatedGaugeMetricsProvider {

  override fun getMetrics(serviceDefinitionId: String, from: Instant, to: Instant, index: Int): List<ServiceInstanceDatapoints<GaugeMetricModel>> {
    val instances = serviceInstanceRepository.findInstancesByServiceId(serviceDefinitionId)

    return if (instances.size > index) {
      listOf(
          @Suppress("UNCHECKED_CAST")
          serviceInstanceRepository.tryGetServiceInstanceMetrics(instances[index].serviceInstanceId, MetricType.GAUGE, from, to) as ServiceInstanceDatapoints<GaugeMetricModel>
      )
    } else {
      listOf()
    }
  }

  override fun canHandle(serviceDefinitionId: String): Boolean {
    return (catalog.findServiceByDefinitionId(serviceDefinitionId) != null)
  }

  override fun totalInstanceCount(serviceDefinitionId: String): Int {
    return serviceInstanceRepository.findInstancesByServiceId(serviceDefinitionId).count()
  }
}