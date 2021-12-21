package io.meshcloud.dockerosb.metrics.periodiccounter

import io.meshcloud.dockerosb.findServiceByName
import io.meshcloud.dockerosb.metrics.MetricsProvider.Companion.utcZoneId
import io.meshcloud.dockerosb.metrics.ServiceInstanceDatapoints
import io.meshcloud.dockerosb.persistence.ServiceInstanceRepository
import org.springframework.cloud.servicebroker.model.catalog.Catalog
import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

@Service
class ApiRequestCountMetricsProvider(
    val catalog: Catalog,
    val serviceInstanceRepository: ServiceInstanceRepository
) : PeriodicCounterMetricsProvider {
  override fun getMetrics(from: Instant, to: Instant): List<ServiceInstanceDatapoints<PeriodicCounterMetricModel>> {
    val service = catalog.findServiceByName(serviceName)
    val instances = serviceInstanceRepository.findInstancesByServiceId(service.id)

    val firstPeriodStart = LocalDateTime.ofInstant(from, utcZoneId).truncatedTo(ChronoUnit.HOURS)
    val lastPeriodStart = LocalDateTime.ofInstant(to, utcZoneId).truncatedTo(ChronoUnit.HOURS)

    // at max go back 35 days in providing metrics, as meshstack will request from 1970 on for the first call
    val earliestStart = lastPeriodStart.minusDays(35L)
    val limitedFirstStart = listOf(firstPeriodStart, earliestStart).maxOrNull()

    return instances.map { instance ->
      ServiceInstanceDatapoints(
          serviceInstanceId = instance.serviceInstanceId,
          resource = "api_request_count",
          values = generateSequence(limitedFirstStart) { prev ->
            prev.plusHours(1).takeIf { x -> !x.isAfter(lastPeriodStart) }
          }.toList().map {
            PeriodicCounterMetricModel(
                writtenAt = to,
                periodStart = it.atZone(utcZoneId).toInstant(),
                periodEnd = it.plusHours(1).atZone(utcZoneId).toInstant(),
                countedValue = BigDecimal.valueOf((1_000L..10_000L).random())
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
    const val serviceName = "API Gateway Service"
    const val serviceId = "g40133dd-8373-4c25-8014-f3398f38a728"
    const val planId = "g13edcdf-eb54-44d3-8902-8fe6d5acb07e"
  }
}
