package io.meshcloud.dockerosb

import org.springframework.cloud.servicebroker.model.catalog.Catalog
import org.springframework.cloud.servicebroker.model.catalog.ServiceDefinition

fun Catalog.findService(id: String): ServiceDefinition? {
  return serviceDefinitions.singleOrNull { it.id == id }
}

fun Catalog.isSynchronousService(serviceId: String): Boolean {
  return findService(serviceId)?.isSynchronous() ?: false
}

fun ServiceDefinition.isSynchronous(): Boolean {
  return tags.contains("synchronous")
}