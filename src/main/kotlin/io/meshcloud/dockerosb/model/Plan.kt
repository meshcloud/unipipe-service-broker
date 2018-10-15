package io.meshcloud.dockerosb.model

import org.springframework.cloud.servicebroker.model.catalog.Plan
import org.springframework.cloud.servicebroker.model.catalog.Schemas
import javax.validation.constraints.NotEmpty

data class Plan(
    @NotEmpty
    val id: String,

    @NotEmpty
    val name: String,

    @NotEmpty
    val description: String,

    val metadata: Map<String, Any>,
    // TODO: Kotlin classes may have to be created for the Schemas class too, otherwise Jackson might not be able to
    // create the object. Somehow the kotlin jackson plugin does not interact that well with the JSON annotations
    // in the Java classes or so. That’s why i added the kotlin implementations of the other classes.
    // As we don’t use these schema definitions for now we don’t have to copy these classes, because it are quite a few.
    val schemas: Schemas?,
    val free: Boolean
) {
  constructor(plan: Plan) : this(
      id = plan.id,
      name = plan.name,
      description = plan.description,
      metadata = plan.metadata ?: emptyMap(),
      schemas = plan.schemas,
      free = plan.isFree
  )
}