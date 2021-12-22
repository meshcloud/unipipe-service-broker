package io.meshcloud.dockerosb.persistence

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import io.meshcloud.dockerosb.metrics.ServiceInstanceDatapoints
import io.meshcloud.dockerosb.metrics.inplace.InplaceMetricModel
import org.springframework.stereotype.Service
import java.io.File
import com.fasterxml.jackson.module.kotlin.*
import io.meshcloud.dockerosb.metrics.gauge.GaugeMetricModel


@Service
class MetricYamlHandler {

  fun readInplaceServiceInstanceDatapoints(file: File): ServiceInstanceDatapoints<InplaceMetricModel> {
    return yamlMapper.readValue<ServiceInstanceDatapoints<InplaceMetricModel>>(file)
  }

  fun readGaugeServiceInstanceDatapoints(file: File): ServiceInstanceDatapoints<GaugeMetricModel> {
    return yamlMapper.readValue<ServiceInstanceDatapoints<GaugeMetricModel>>(file)
  }

  companion object {
    private val yamlMapper = ObjectMapper(YAMLFactory())
        .registerKotlinModule()
        .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
        .registerModule(JavaTimeModule())
  }
}