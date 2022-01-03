package io.meshcloud.dockerosb.metrics.samplingcounter

import io.meshcloud.dockerosb.findServiceByDefinitionId
import io.meshcloud.dockerosb.metrics.MetricType
import io.meshcloud.dockerosb.metrics.ServiceInstanceDatapoints
import io.meshcloud.dockerosb.persistence.ServiceInstanceRepository
import org.springframework.cloud.servicebroker.model.catalog.Catalog
import org.springframework.stereotype.Service
import java.time.Instant

@Service
class SamplingCounterMetricProvider(
    private val catalog: Catalog,
    private val serviceInstanceRepository: ServiceInstanceRepository
) : SamplingCounterMetricProviderInterface {

  override fun getMetrics(serviceDefinitionId: String, from: Instant, to: Instant, index: Int): List<ServiceInstanceDatapoints<SamplingCounterMetricModel>> {
    val instances = serviceInstanceRepository.findInstancesByServiceId(serviceDefinitionId)

    return if (instances.size > index) {
      @Suppress("UNCHECKED_CAST")
      serviceInstanceRepository.tryGetServiceInstanceMetrics(instances[index].serviceInstanceId, MetricType.SAMPLING, from, to) as List<ServiceInstanceDatapoints<SamplingCounterMetricModel>>
    } else {
      listOf()
    }
  }

  override fun canHandle(serviceDefinitionId: String): Boolean {
    return catalog.findServiceByDefinitionId(serviceDefinitionId) != null
  }

  override fun totalInstanceCount(serviceDefinitionId: String): Int {
    return serviceInstanceRepository.findInstancesByServiceId(serviceDefinitionId).count()
  }
}
