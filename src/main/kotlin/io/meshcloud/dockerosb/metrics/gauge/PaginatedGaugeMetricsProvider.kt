package io.meshcloud.dockerosb.metrics.gauge

import io.meshcloud.dockerosb.metrics.MetricsProvider
import io.meshcloud.dockerosb.metrics.ServiceInstanceDatapoints
import java.time.Instant

/**
 * Provides gauge metrics with one page containing the metrics data for a single instance
 */
interface PaginatedGaugeMetricsProvider : MetricsProvider<PaginatedGaugeMetricsProvider> {
  /**
   * index: the index of the service instance that the metrics is request is for, after sorting by last modified time
   */
  fun getMetrics(serviceInstanceId: String, from: Instant, to: Instant, index: Int): List<ServiceInstanceDatapoints<GaugeMetricModel>>

  /**
   * Total count of instances for which this provider provides metrics
   */
  fun totalInstanceCount(): Int
}
