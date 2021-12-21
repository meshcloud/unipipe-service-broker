package io.meshcloud.dockerosb.metrics.samplingcounter

import io.meshcloud.dockerosb.metrics.MetricsProvider
import io.meshcloud.dockerosb.metrics.ServiceInstanceDatapoints
import java.time.Instant

interface SamplingCounterMetricsProvider : MetricsProvider<SamplingCounterMetricModel> {
  fun getMetrics(from: Instant, to: Instant): List<ServiceInstanceDatapoints<SamplingCounterMetricModel>>
}
