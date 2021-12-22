package io.meshcloud.dockerosb.persistence

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import org.springframework.stereotype.Service
import java.io.File
import com.fasterxml.jackson.module.kotlin.*

@Service
class GenericYamlHandler {

  val metricYamlMapper = ObjectMapper(YAMLFactory())
    .registerKotlinModule()
    .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
    .registerModule(JavaTimeModule())

  final inline fun <reified T>readGeneric(file: File): T {
    return metricYamlMapper.readValue(file)
  }
}