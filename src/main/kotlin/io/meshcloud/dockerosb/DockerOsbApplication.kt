package io.meshcloud.dockerosb

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.scheduling.annotation.EnableScheduling

@SpringBootApplication
@EnableScheduling
class DockerOsbApplication

fun main(args: Array<String>) {
  runApplication<DockerOsbApplication>(*args)
}
