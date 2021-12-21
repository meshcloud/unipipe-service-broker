package io.meshcloud.dockerosb.integrationtests

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.KotlinModule
import io.meshcloud.dockerosb.metrics.MetricsResponse
import io.meshcloud.dockerosb.metrics.gauge.RunningVmsMetricProvider
import org.junit.Assert
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.Instant
import java.time.temporal.ChronoUnit

class GaugeMetricsServiceTest : BaseMetricsServiceTest() {

  val objectMapper = ObjectMapper(YAMLFactory()).apply {
    registerModules(KotlinModule())
    registerModule(JavaTimeModule())
    configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
  }

  @Test
  fun testPaginatedGaugeMetricsServiceEndpoint() {
    val firstInstanceId = "j90133dd-8373-4c25-8014-fde98f38a728"
    val secondInstanceId = "j91133dd-8373-4c25-8014-fde98f38a728"
    provisionServiceInstance(
        instanceId = firstInstanceId,
        serviceId = RunningVmsMetricProvider.serviceId,
        planId = RunningVmsMetricProvider.planId
    )
    provisionServiceInstance(
        instanceId = secondInstanceId,
        serviceId = RunningVmsMetricProvider.serviceId,
        planId = RunningVmsMetricProvider.planId
    )

    val firstResponse = restTemplateWithBasicAuth()
        .getForEntity(
            """${baseUrl()}metrics/gauges/${RunningVmsMetricProvider.serviceId}?from=${Instant.now().minus(5, ChronoUnit.HOURS)}&to=${Instant.now()}""",
            String::class.java
        )
    val firstResponseObject = objectMapper.readValue(firstResponse.body, MetricsResponse::class.java)
    Assert.assertEquals(1, firstResponseObject?.dataPoints?.size)
    Assert.assertEquals(firstInstanceId, firstResponseObject?.dataPoints?.get(0)?.serviceInstanceId)
    Assert.assertEquals(6, firstResponseObject?.dataPoints?.get(0)?.values?.size)

    val firstResponseMap = objectMapper.readValue(firstResponse.body, Map::class.java)
    @Suppress("UNCHECKED_CAST")
    val nextLink = ((firstResponseMap["_links"] as Map<String, *>)["next"] as Map<String, *>)["href"] as String

    val secondResponse = restTemplateWithBasicAuth().getForEntity(nextLink, String::class.java)
    val secondResponseObject = objectMapper.readValue(secondResponse.body, MetricsResponse::class.java)
    Assert.assertEquals(1, secondResponseObject?.dataPoints?.size)
    Assert.assertEquals(secondInstanceId, secondResponseObject?.dataPoints?.get(0)?.serviceInstanceId)
    Assert.assertEquals(6, secondResponseObject?.dataPoints?.get(0)?.values?.size)

    val secondResponseMap = objectMapper.readValue(secondResponse.body, Map::class.java)
    assertNull(secondResponseMap["_links"])
  }
}
