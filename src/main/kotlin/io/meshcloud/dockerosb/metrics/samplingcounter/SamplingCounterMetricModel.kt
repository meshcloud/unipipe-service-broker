package io.meshcloud.dockerosb.metrics.samplingcounter

import java.math.BigDecimal
import java.time.Instant

class SamplingCounterMetricModel(
    val writtenAt: Instant,
    val observedAt: Instant,
    val value: BigDecimal
)
