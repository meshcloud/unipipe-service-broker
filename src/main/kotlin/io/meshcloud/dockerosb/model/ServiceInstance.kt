package io.meshcloud.dockerosb.model

import org.springframework.cloud.servicebroker.model.Context
import org.springframework.cloud.servicebroker.model.instance.CreateServiceInstanceRequest
import org.springframework.cloud.servicebroker.model.instance.UpdateServiceInstanceRequest
import javax.validation.constraints.NotEmpty

data class ServiceInstance(

    @NotEmpty
    val serviceInstanceId: String,

    @NotEmpty
    val serviceDefinitionId: String,

    @NotEmpty
    var planId: String,

    val serviceDefinition: ServiceDefinition,
    val originatingIdentity: Context,
    val asyncAccepted: Boolean,
    val parameters: MutableMap<String, Any>,
    val context: Context?,
    var deleted: Boolean = false
) {
  constructor(request: CreateServiceInstanceRequest) : this(
      serviceInstanceId = request.serviceInstanceId,
      asyncAccepted = request.isAsyncAccepted,
      context = request.context,
      originatingIdentity = request.originatingIdentity,
      parameters = request.parameters ?: mutableMapOf(),
      planId = request.planId,
      serviceDefinition = ServiceDefinition(request.serviceDefinition),
      serviceDefinitionId = request.serviceDefinitionId
  )

  fun update(request: UpdateServiceInstanceRequest): ServiceInstance {
    require(serviceInstanceId == request.serviceInstanceId)

    return copy(
        asyncAccepted = request.isAsyncAccepted,
        context = request.context ?: context,
        originatingIdentity = request.originatingIdentity ?: originatingIdentity,
        parameters = request.parameters ?: request.parameters,
        planId = request.planId ?: planId,
        serviceDefinition = request.serviceDefinition?.let { ServiceDefinition(it) } ?: serviceDefinition,
        serviceDefinitionId = request.serviceDefinitionId ?: serviceDefinitionId
    )
  }
}
