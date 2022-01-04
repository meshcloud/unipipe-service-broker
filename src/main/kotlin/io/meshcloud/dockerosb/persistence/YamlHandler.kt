package io.meshcloud.dockerosb.persistence

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.readValue
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import org.apache.commons.io.FileUtils
import org.springframework.stereotype.Service
import java.io.File
import java.io.FileWriter


/**
 * Note: consumers should use this only via a [GitOperationContext]
 */
@Service
class YamlHandler {

  val yamlMapper = ObjectMapper(YAMLFactory())
    .registerKotlinModule()
    .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
    .registerModule(JavaTimeModule())

  fun writeObject(objectToWrite: Any, file: File) {
    FileUtils.forceMkdir(file.parentFile)

    FileWriter(file).use { writer ->
      yamlMapper
          .writerWithDefaultPrettyPrinter()
          .writeValue(writer, objectToWrite)

    }
  }

  fun <T> readObject(file: File, targetClass: Class<T>): T {
    return yamlMapper.readValue(file, targetClass)
  }

  fun <T> readObject(file: File, typeRef: TypeReference<T>): T {
    return yamlMapper.readValue(file, typeRef)
  }

  final inline fun <reified T>readGeneric(file: File): T {
    return yamlMapper.readValue(file)
  }

  final inline fun <reified T>readGeneric(string: String): T {
    return yamlMapper.readValue(string)
  }
}