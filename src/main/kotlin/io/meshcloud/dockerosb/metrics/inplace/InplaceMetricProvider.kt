package io.meshcloud.dockerosb.metrics.inplace

import io.meshcloud.dockerosb.findServiceByName
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

  override fun totalInstanceCount(): Int {
    val service = catalog.findServiceByName(serviceName)
    return serviceInstanceRepository.findInstancesByServiceId(service.id).count()
  }

  override fun getMetrics(serviceDefinitionId: String, from: Instant, to: Instant, index: Int): List<ServiceInstanceDatapoints<InplaceMetricModel>> {
    //val service = catalog.findServiceByName(serviceName)
    val instances = serviceInstanceRepository.findInstancesByServiceId(serviceDefinitionId)
    val metrics = serviceInstanceRepository.getServiceInstanceMetrics(instances[0].serviceInstanceId)

    return if (instances.size > index) {
      val firstTimestamp = LocalDateTime.ofInstant(from, utcZoneId).truncatedTo(ChronoUnit.HOURS)
      val lastTimestamp = LocalDateTime.ofInstant(to, utcZoneId).truncatedTo(ChronoUnit.HOURS)
      // at max go back 5 days in providing metrics, as meshstack will request from 1970 on for the first call

      listOf(
         // serviceInstanceRepository.getServiceInstanceMetrics(instances[index].serviceInstanceId, ServiceInstanceDatapoints<InplaceMetricModel>)
      )
    } else {
      listOf()
    }
  }

  override fun canHandle(serviceDefinitionId: String): Boolean {
    val service = catalog.findServiceByName(serviceName)
    return service.id == serviceDefinitionId
  }

  companion object {
    const val serviceName = "DBaaS"
    const val serviceId = "j40133dd-8373-4c25-8014-fc298f38a728"
    const val planId = "j13ed56f-eb54-44d3-8902-8f24d5acb07e"
  }
}