package io.meshcloud.dockerosb.metrics.periodiccounter

import java.math.BigDecimal
import java.time.Instant

data class PeriodicCounterMetricModel(
    val writtenAt: Instant,
    val periodStart: Instant,
    val periodEnd: Instant,
    val countedValue: BigDecimal
)
