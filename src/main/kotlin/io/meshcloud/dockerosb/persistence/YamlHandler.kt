package io.meshcloud.dockerosb.persistence

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
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
    file.parentFile.mkdirs() // mkdir -p

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
}