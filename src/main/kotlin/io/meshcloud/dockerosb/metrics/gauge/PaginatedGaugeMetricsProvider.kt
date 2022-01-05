package io.meshcloud.dockerosb.metrics.gauge

import io.meshcloud.dockerosb.metrics.MetricsProvider
import io.meshcloud.dockerosb.metrics.ServiceInstanceDatapoints
import java.time.Instant

/**
 * Provides gauge metrics with one page containing the metrics data for a single instance
 */
interface PaginatedGaugeMetricsProvider : MetricsProvider<PaginatedGaugeMetricsProvider> {
  /**
   * serviceDefinitionId: defines which serviceDefinition metrics you want to get
   * from: the time filter start date for observedAt parameter
   * end: the time filter end date for observedAt parameter
   * index: selects the service instance for the serviceDefinitionId and ordering them with observedAt parameter
   */
  fun getMetrics(serviceDefinitionId: String, from: Instant, to: Instant, index: Int): List<ServiceInstanceDatapoints<GaugeMetricModel>>

  /**
   * Total count of instances for which this provider provides metrics
   */
  fun totalInstanceCount(serviceDefinitionId: String): Int
}
