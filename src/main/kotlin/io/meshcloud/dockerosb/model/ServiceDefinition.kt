package io.meshcloud.dockerosb.model

import org.springframework.cloud.servicebroker.model.catalog.ServiceDefinition
import javax.validation.constraints.NotEmpty

data class ServiceDefinition(
    @NotEmpty
    val id: String,

    @NotEmpty
    val name: String,

    @NotEmpty
    val description: String,

    val plans: List<Plan>,
    val bindable: Boolean,
    val planUpdateable: Boolean,
    val tags: List<String>,
    val metadata: Map<String, Any>,
    val requires: List<String>,
    val dashboardClient: DashboardClient?
) {
  constructor(def: ServiceDefinition) : this(
      id = def.id,
      bindable = def.isBindable,
      dashboardClient = def.dashboardClient?.let { DashboardClient(def.dashboardClient) },
      description = def.description,
      metadata = def.metadata ?: emptyMap(),
      name = def.name,
      plans = def.plans.map { Plan(it) },
      planUpdateable = def.isPlanUpdateable ?: false,
      requires = def.requires ?: emptyList(),
      tags = def.tags ?: emptyList()
  )
}