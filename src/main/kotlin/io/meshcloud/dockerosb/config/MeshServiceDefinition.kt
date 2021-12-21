package io.meshcloud.dockerosb.config


import org.springframework.cloud.servicebroker.model.catalog.ServiceDefinition

class MeshServiceDefinition() : ServiceDefinition() {
  val metrics: MetricsCatalogExtension? = null
}

class MetricsCatalogExtension(
    val gauges: String?,
    val samplingCounters: String?,
    val periodicCounters: String?
)
