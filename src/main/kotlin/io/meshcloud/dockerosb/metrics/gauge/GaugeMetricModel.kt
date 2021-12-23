package io.meshcloud.dockerosb.metrics.gauge

import java.math.BigDecimal
import java.time.Instant

data class GaugeMetricModel(
    val writtenAt: Instant,
    val observedAt: Instant,
    val value: BigDecimal
)