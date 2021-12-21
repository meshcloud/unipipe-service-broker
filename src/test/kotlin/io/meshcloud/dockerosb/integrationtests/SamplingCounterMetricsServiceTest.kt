package io.meshcloud.dockerosb.integrationtests

import io.meshcloud.dockerosb.metrics.MetricsResponse
import io.meshcloud.dockerosb.metrics.samplingcounter.CloudStorageNetworkTrafficMetricsProvider
import org.junit.Assert
import org.junit.Test
import java.time.Instant
import java.time.temporal.ChronoUnit

class SamplingCounterMetricsServiceTest : BaseMetricsServiceTest() {
  @Test
  fun testCloudStorageServiceMetricsEndpoint() {
    val instanceId = "k90133dd-8373-4c25-8014-fde98f38a728"
    provisionServiceInstance(
        instanceId = instanceId,
        serviceId = CloudStorageNetworkTrafficMetricsProvider.serviceId,
        planId = CloudStorageNetworkTrafficMetricsProvider.planId
    )

    val response = restTemplateWithBasicAuth()
        .getForEntity(
            """${baseUrl()}metrics/samplingCounters/${CloudStorageNetworkTrafficMetricsProvider.serviceId}?from=${Instant.now().minus(1, ChronoUnit.DAYS)}&to=${Instant.now()}""",
            MetricsResponse::class.java
        )
    Assert.assertEquals(1, response.body?.dataPoints?.size)
    Assert.assertEquals(instanceId, response.body?.dataPoints?.get(0)?.serviceInstanceId)
    Assert.assertEquals(25, response.body?.dataPoints?.get(0)?.values?.size)
  }
}
