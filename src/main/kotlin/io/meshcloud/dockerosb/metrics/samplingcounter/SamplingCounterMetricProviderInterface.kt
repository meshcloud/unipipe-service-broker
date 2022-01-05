package io.meshcloud.dockerosb.metrics.samplingcounter

import io.meshcloud.dockerosb.metrics.MetricsProvider
import io.meshcloud.dockerosb.metrics.ServiceInstanceDatapoints
import java.time.Instant

interface SamplingCounterMetricProviderInterface : MetricsProvider<SamplingCounterMetricModel> {
  /**
   * serviceDefinitionId: defines which serviceDefinition metrics you want to get
   * from: the time filter start date for observedAt parameter
   * end: the time filter end date for observedAt parameter
   * index: selects the service instance for the serviceDefinitionId and ordering them with observedAt parameter
   */
  fun getMetrics(serviceDefinitionId: String, from: Instant, to: Instant, index: Int): List<ServiceInstanceDatapoints<SamplingCounterMetricModel>>

  /**
   * Total count of instances for which this provider provides metrics
   */
  fun totalInstanceCount(serviceDefinitionId: String): Int
}
