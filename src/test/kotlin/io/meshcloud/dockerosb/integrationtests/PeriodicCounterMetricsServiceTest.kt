package io.meshcloud.dockerosb.integrationtests

import io.meshcloud.dockerosb.metrics.MetricsResponse
import io.meshcloud.dockerosb.metrics.periodiccounter.ApiRequestCountMetricsProvider
import io.meshcloud.dockerosb.metrics.perodiccounter.FakeThirdPartyServiceMetricsProvider
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Instant
import java.time.temporal.ChronoUnit

class PeriodicCounterMetricsServiceTest : BaseMetricsServiceTest() {

  @Test
  fun testThirdPartyInvoiceMetricsEndpoint() {
    val instanceId = "b92c0ca7-c162-4029-b567-0d92978c0a99"
    provisionServiceInstance(
        instanceId = instanceId,
        serviceId = FakeThirdPartyServiceMetricsProvider.serviceId,
        planId = FakeThirdPartyServiceMetricsProvider.planId
    )
    val response = restTemplateWithBasicAuth()
        .getForEntity(
            """${baseUrl()}metrics/periodicCounters/${FakeThirdPartyServiceMetricsProvider.serviceId}?from=${Instant.now().minus(1, ChronoUnit.DAYS)}&to=${Instant.now()}""",
            MetricsResponse::class.java
        )
    assertEquals(1, response.body?.dataPoints?.size)
    assertEquals(instanceId, response.body?.dataPoints?.get(0)?.serviceInstanceId)
    assertEquals(1, response.body?.dataPoints?.get(0)?.values?.size)
  }

  @Test
  fun testApiGatewayMetricsEndpoint() {
    val instanceId = "g92c0ca7-c162-4029-b567-0d92978c0a98"
    provisionServiceInstance(
        instanceId = instanceId,
        serviceId = ApiRequestCountMetricsProvider.serviceId,
        planId = ApiRequestCountMetricsProvider.planId
    )
    val response = restTemplateWithBasicAuth()
        .getForEntity(
            """${baseUrl()}metrics/periodicCounters/${ApiRequestCountMetricsProvider.serviceId}?from=2020-11-01T00:00:00.000Z&to=2020-11-02T00:30:00.000Z""",
            MetricsResponse::class.java
        )
    assertEquals(1, response.body?.dataPoints?.size)
    assertEquals(25, response.body?.dataPoints?.get(0)?.values?.size)
    assertEquals(instanceId, response.body?.dataPoints?.get(0)?.serviceInstanceId)
  }
}
