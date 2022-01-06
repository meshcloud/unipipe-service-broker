package io.meshcloud.dockerosb

import org.springframework.cloud.servicebroker.model.catalog.Catalog
import org.springframework.cloud.servicebroker.model.catalog.ServiceDefinition

fun Catalog.findServiceByDefinitionId(definitionId: String): ServiceDefinition? {
  return serviceDefinitions.singleOrNull { it.id == definitionId }
}

