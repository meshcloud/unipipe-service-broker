package io.meshcloud.dockerosb

import org.springframework.boot.autoconfigure.EnableAutoConfiguration
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.runApplication
import org.springframework.cloud.servicebroker.autoconfigure.web.Catalog

@SpringBootApplication
class DockerOsbApplication

fun main(args: Array<String>) {
  runApplication<DockerOsbApplication>(*args)
}
