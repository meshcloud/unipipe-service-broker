package io.meshcloud.dockerosb.metrics.gauge

import io.meshcloud.dockerosb.findServiceByName
import io.meshcloud.dockerosb.metrics.MetricType
import io.meshcloud.dockerosb.metrics.MetricsProvider.Companion.utcZoneId
import io.meshcloud.dockerosb.metrics.ServiceInstanceDatapoints
import io.meshcloud.dockerosb.persistence.ServiceInstanceRepository
import org.springframework.cloud.servicebroker.model.catalog.Catalog
import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

/**
 * Sends the metrics of each instance in a paged fashion
 */
@Service
class RunningVmsMetricProvider(
    val catalog: Catalog,
    val serviceInstanceRepository: ServiceInstanceRepository
) : PaginatedGaugeMetricsProvider {

  override fun totalInstanceCount(): Int {
    val service = catalog.findServiceByName(serviceName)
    return serviceInstanceRepository.findInstancesByServiceId(service.id).count()
  }

  override fun getMetrics(serviceDefinitionId: String, from: Instant, to: Instant, index: Int): List<ServiceInstanceDatapoints<GaugeMetricModel>> {
    val instances = serviceInstanceRepository.findInstancesByServiceId(serviceDefinitionId)

    return if (instances.size > index) {
      val firstTimestamp = LocalDateTime.ofInstant(from, utcZoneId).truncatedTo(ChronoUnit.HOURS)
      val lastTimestamp = LocalDateTime.ofInstant(to, utcZoneId).truncatedTo(ChronoUnit.HOURS)
      // at max go back 5 days in providing metrics, as meshstack will request from 1970 on for the first call
      val earliestStartTimestamp = lastTimestamp.minusDays(5L)
      val limitedFirstTimestamp = listOf(firstTimestamp, earliestStartTimestamp).maxOrNull()

      listOf(ServiceInstanceDatapoints(
          serviceInstanceId = instances[index].serviceInstanceId,
          resource = "small_vm_count",
          values = generateSequence(limitedFirstTimestamp) { prev ->
            prev.plusHours(1).takeIf { !it.isAfter(lastTimestamp) }
          }.toList().map {
            GaugeMetricModel(
                writtenAt = to,
                observedAt = it.atZone(utcZoneId).toInstant(),
                value = BigDecimal.valueOf(it.dayOfMonth / 10 + 1L)
            )
          }))
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
