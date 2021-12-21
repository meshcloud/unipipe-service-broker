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
import java.time.ZoneOffset
import java.time.temporal.ChronoUnit
import java.time.temporal.TemporalAdjusters

/**
 * Simulates the invoice from a third party service as a periodicMetric
 */
@Service
class FakeThirdPartyServiceMetricsProvider(
    val catalog: Catalog,
    val serviceInstanceRepository: ServiceInstanceRepository
) : PeriodicCounterMetricsProvider {

  /**
   * Ignores from and to parameters because this is a monthly invoice
   */
  override fun getMetrics(from: Instant, to: Instant): List<ServiceInstanceDatapoints<PeriodicCounterMetricModel>> {
    val service = catalog.findServiceByName(serviceName)
    val instances = serviceInstanceRepository.findInstancesByServiceId(service.id)

    val now = LocalDateTime.now(ZoneOffset.UTC)
    val monthStart = now.with(TemporalAdjusters.firstDayOfMonth()).toLocalDate().atStartOfDay()
    val monthEnd = monthStart.with(TemporalAdjusters.firstDayOfNextMonth()).toLocalDate().atStartOfDay()

    // create a generally increasing random value
    val hoursSinceMonthStart = ChronoUnit.HOURS.between(monthStart, now)
    return instances.map {
      ServiceInstanceDatapoints(
          serviceInstanceId = it.serviceInstanceId,
          resource = "third_party_invoice",
          values = listOf(
              PeriodicCounterMetricModel(
                  writtenAt = now.atZone(utcZoneId).toInstant(),
                  periodStart = monthStart.atZone(utcZoneId).toInstant(),
                  periodEnd = monthEnd.atZone(utcZoneId).toInstant(),
                  countedValue = BigDecimal.valueOf((hoursSinceMonthStart * 100..hoursSinceMonthStart * 101).random() + (1..99).random() / 100.0)
              )
          )
      )
    }
  }

  override fun canHandle(serviceDefinitionId: String): Boolean {
    val service = catalog.findServiceByName(serviceName)
    return service.id == serviceDefinitionId
  }

  companion object {
    const val serviceName = "Datadog"
    const val serviceId = "d40133dd-8373-4c25-8014-fde98f38a728"
    const val planId = "a13edcdf-eb54-44d3-8902-8f24d5acb07e"
  }

}
