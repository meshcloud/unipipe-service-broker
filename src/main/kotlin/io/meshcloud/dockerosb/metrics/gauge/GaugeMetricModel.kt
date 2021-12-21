package io.meshcloud.dockerosb.metrics.gauge

import java.math.BigDecimal
import java.time.Instant

class GaugeMetricModel(
    val writtenAt: Instant,
    val observedAt: Instant,
    val value: BigDecimal
)