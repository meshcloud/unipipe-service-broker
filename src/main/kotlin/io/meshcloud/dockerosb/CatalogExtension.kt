package io.meshcloud.dockerosb

import org.springframework.cloud.servicebroker.model.catalog.Catalog
import org.springframework.cloud.servicebroker.model.catalog.Plan
import org.springframework.cloud.servicebroker.model.catalog.ServiceDefinition

fun Catalog.tryFindServicePlan(definitionId: String, planId: String): Plan? {
  return serviceDefinitions.singleOrNull { it.id == definitionId }?.plans?.singleOrNull { it.id == planId }
}

fun Catalog.findServiceByName(serviceName: String): ServiceDefinition {
  return serviceDefinitions.single { it.name == serviceName }
}

fun Catalog.findServiceByDefinitionId(definitionId: String): ServiceDefinition? {
  return serviceDefinitions.singleOrNull { it.id == definitionId }
}

