package io.meshcloud.dockerosb.metrics

import io.meshcloud.dockerosb.metrics.gauge.GaugeMetricModel
import io.meshcloud.dockerosb.persistence.YamlHandler
import org.assertj.core.api.Assertions.assertThat
import org.junit.Assert
import org.junit.Test
import java.io.File
import java.math.BigDecimal
import java.time.Instant

class GetServiceInstanceMetricsTest {
  private val uut = YamlHandler()

  @Test
  fun canReadGenericMetrics() {

    val gaugeMetrics = uut.readGeneric<ServiceInstanceDatapoints<GaugeMetricModel>>(
        File("src/test/resources/metrics/gauge-test.yml")
    )

    Assert.assertEquals(gaugeMetrics.serviceInstanceId, "test")
    Assert.assertEquals(gaugeMetrics.resource, "test-resource")
    assertThat(gaugeMetrics.values).containsExactlyInAnyOrderElementsOf(listOf(
        GaugeMetricModel(
            writtenAt = Instant.parse("2021-12-01T16:37:23Z"),
            observedAt = Instant.parse("2021-12-01T16:37:23Z"),
            value = BigDecimal.valueOf(1.0)
        ),
        GaugeMetricModel(
            writtenAt = Instant.parse("2021-12-02T16:37:23Z"),
            observedAt = Instant.parse("2021-12-02T16:37:23Z"),
            value = BigDecimal.valueOf(2.0)
        ),
    ))
  }
}

