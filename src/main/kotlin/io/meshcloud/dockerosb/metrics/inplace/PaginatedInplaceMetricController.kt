package io.meshcloud.dockerosb.metrics.inplace

import io.meshcloud.dockerosb.metrics.MetricsProvider
import io.meshcloud.dockerosb.metrics.ServiceInstanceDatapoints
import io.meshcloud.dockerosb.metrics.inplace.InplaceMetricModel
import java.time.Instant

/**
 * Provides gauge metrics with one page containing the metrics data for a single instance
 */
interface PaginatedInplaceMetricsProvider : MetricsProvider<PaginatedInplaceMetricsProvider> {
  /**
   * index: the index of the service instance that the metrics is request is for, after sorting by last modified time
   */
  fun getMetrics(serviceDefinitionId: String, from: Instant, to: Instant, index: Int): List<ServiceInstanceDatapoints<InplaceMetricModel>>

  /**
   * Total count of instances for which this provider provides metrics
   */
  fun totalInstanceCount(): Int
}
