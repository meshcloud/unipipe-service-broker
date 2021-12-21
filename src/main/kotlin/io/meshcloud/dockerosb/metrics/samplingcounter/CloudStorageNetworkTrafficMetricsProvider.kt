package io.meshcloud.dockerosb.metrics.samplingcounter

import io.meshcloud.dockerosb.findServiceByName
import io.meshcloud.dockerosb.metrics.MetricsProvider
import io.meshcloud.dockerosb.metrics.ServiceInstanceDatapoints
import io.meshcloud.dockerosb.persistence.ServiceInstanceRepository
import org.springframework.cloud.servicebroker.model.catalog.Catalog
import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import java.time.temporal.ChronoUnit.MINUTES

@Service
class CloudStorageNetworkTrafficMetricsProvider(
    private val catalog: Catalog,
    private val serviceInstanceRepository: ServiceInstanceRepository
) : SamplingCounterMetricsProvider {

  override fun getMetrics(from: Instant, to: Instant): List<ServiceInstanceDatapoints<SamplingCounterMetricModel>> {
    val service = catalog.findServiceByName(serviceName)
    val instances = serviceInstanceRepository.findInstancesByServiceId(service.id)
    val firstTimestamp = LocalDateTime.ofInstant(from, MetricsProvider.utcZoneId).truncatedTo(ChronoUnit.HOURS)
    val lastTimestamp = LocalDateTime.ofInstant(to, MetricsProvider.utcZoneId).truncatedTo(ChronoUnit.HOURS)
    // at max go back 35 days in providing metrics, as meshstack will request from 1970 on for the first call
    val earliestStartTimestamp = lastTimestamp.minusDays(35L)
    val limitedFirstTimestamp = listOf(firstTimestamp, earliestStartTimestamp).maxOrNull()

    return instances.map { i ->
      ServiceInstanceDatapoints(
          serviceInstanceId = i.serviceInstanceId,
          resource = "gigabytes_transferred",
          values = generateSequence(limitedFirstTimestamp) { prev ->
            prev.plusHours(1).takeIf { !it.isAfter(lastTimestamp) }
          }.toList().map {
            val randomCounterValue =
                MINUTES.between(LocalDate.of(2020, 10, 1).atStartOfDay(), it) + (0..99).random() / 100.0
            SamplingCounterMetricModel(
                writtenAt = to,
                observedAt = it.atZone(MetricsProvider.utcZoneId).toInstant(),
                value = BigDecimal.valueOf(randomCounterValue)
            )
          }
      )
    }
  }

  override fun canHandle(serviceDefinitionId: String): Boolean {
    val service = catalog.findServiceByName(serviceName)
    return service.id == serviceDefinitionId
  }


  companion object {
    const val serviceName = "Amazon S3"
    const val serviceId = "k4013377-8373-4c25-8014-fde98f38a728"
    const val planId = "k13edce8-eb54-44d3-8902-8f24d5acb07e"
  }
}
