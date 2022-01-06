package io.meshcloud.dockerosb.metrics.samplingcounter

import io.meshcloud.dockerosb.metrics.MetricsProvider
import io.meshcloud.dockerosb.metrics.ServiceInstanceDatapoints
import io.meshcloud.dockerosb.persistence.ServiceInstanceRepository
import org.springframework.cloud.servicebroker.model.catalog.Catalog
import org.springframework.stereotype.Service
import java.time.Instant

@Service
class SamplingCounterMetricProvider(
    catalog: Catalog,
    serviceInstanceRepository: ServiceInstanceRepository
) : MetricsProvider<SamplingCounterMetricModel>(catalog, serviceInstanceRepository) {

  override fun getMetrics(serviceDefinitionId: String, from: Instant, to: Instant, index: Int): List<ServiceInstanceDatapoints<SamplingCounterMetricModel>> {
    val instances = serviceInstanceRepository.findInstancesByServiceId(serviceDefinitionId)

    return if (instances.size > index) {
      serviceInstanceRepository.tryGetServiceInstanceSamplingCounterMetrics(instances[index].serviceInstanceId, from, to)
    } else {
      listOf()
    }
  }
}
