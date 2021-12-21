package io.meshcloud.dockerosb.metrics

import java.time.ZoneId

interface MetricsProvider<T> {

  fun canHandle(serviceDefinitionId: String): Boolean

  companion object {
    val utcZoneId: ZoneId = ZoneId.of("UTC")
  }
}