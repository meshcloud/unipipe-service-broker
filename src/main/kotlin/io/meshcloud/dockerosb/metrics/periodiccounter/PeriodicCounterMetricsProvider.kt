package io.meshcloud.dockerosb.metrics.periodiccounter

import io.meshcloud.dockerosb.metrics.MetricsProvider
import io.meshcloud.dockerosb.metrics.ServiceInstanceDatapoints
import java.time.Instant

interface PeriodicCounterMetricsProvider : MetricsProvider<PeriodicCounterMetricModel> {
  fun getMetrics(from: Instant, to: Instant): List<ServiceInstanceDatapoints<PeriodicCounterMetricModel>>
}
