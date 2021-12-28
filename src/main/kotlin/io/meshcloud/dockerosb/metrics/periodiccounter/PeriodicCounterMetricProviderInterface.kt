package io.meshcloud.dockerosb.metrics.periodiccounter

import io.meshcloud.dockerosb.metrics.MetricsProvider
import io.meshcloud.dockerosb.metrics.ServiceInstanceDatapoints
import java.time.Instant

interface PeriodicCounterMetricProviderInterface : MetricsProvider<PeriodicCounterMetricModel> {
  /**
   * index: the index of the service instance that the metrics is request is for, after sorting by last modified time
   */
  fun getMetrics(serviceDefinitionId: String, from: Instant, to: Instant, index: Int): List<ServiceInstanceDatapoints<PeriodicCounterMetricModel>>

  /**
   * Total count of instances for which this provider provides metrics
   */
  fun totalInstanceCount(serviceDefinitionId: String): Int
}
