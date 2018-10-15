package io.meshcloud.dockerosb

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class DockerOsbApplication

fun main(args: Array<String>) {
    runApplication<DockerOsbApplication>(*args)
}
