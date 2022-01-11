package io.meshcloud.dockerosb.metrics.gauge

import io.meshcloud.dockerosb.findServiceByDefinitionId
import io.meshcloud.dockerosb.metrics.MetricType
import io.meshcloud.dockerosb.metrics.MetricsProvider
import io.meshcloud.dockerosb.metrics.ServiceInstanceDatapoints
import io.meshcloud.dockerosb.persistence.ServiceInstanceRepository
import org.springframework.cloud.servicebroker.model.catalog.Catalog
import org.springframework.stereotype.Service
import java.time.Instant

/**
 * Sends the metrics of each instance in a paged fashion
 */
@Service
class GaugeMetricProvider(
    catalog: Catalog,
    serviceInstanceRepository: ServiceInstanceRepository
) : MetricsProvider<GaugeMetricModel>(catalog, serviceInstanceRepository) {

  override fun getMetrics(serviceDefinitionId: String, from: Instant, to: Instant, index: Int): List<ServiceInstanceDatapoints<GaugeMetricModel>> {
    val instances = serviceInstanceRepository.findInstancesByServiceId(serviceDefinitionId)

    return if (instances.size > index) {
      serviceInstanceRepository.tryGetServiceInstanceGaugeMetrics(instances[index].serviceInstanceId, from, to)
    } else {
      listOf()
    }
  }
}