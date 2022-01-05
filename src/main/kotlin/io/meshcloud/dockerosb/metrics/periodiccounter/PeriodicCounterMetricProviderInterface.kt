package io.meshcloud.dockerosb.metrics.periodiccounter

import io.meshcloud.dockerosb.metrics.MetricsProvider
import io.meshcloud.dockerosb.metrics.ServiceInstanceDatapoints
import java.time.Instant

interface PeriodicCounterMetricProviderInterface : MetricsProvider<PeriodicCounterMetricModel> {
  /**
   * serviceDefinitionId: defines which serviceDefinition metrics you want to get
   * from: the time filter start date for periodStart parameter
   * end: the time filter end date for periodEnd parameter
   * index: selects the service instance for the serviceDefinitionId and ordering them with periodStart parameter
   */
  fun getMetrics(serviceDefinitionId: String, from: Instant, to: Instant, index: Int): List<ServiceInstanceDatapoints<PeriodicCounterMetricModel>>

  /**
   * Total count of instances for which this provider provides metrics
   */
  fun totalInstanceCount(serviceDefinitionId: String): Int
}
