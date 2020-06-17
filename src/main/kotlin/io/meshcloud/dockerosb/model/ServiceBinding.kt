package io.meshcloud.dockerosb.model

import org.springframework.cloud.servicebroker.model.Context
import org.springframework.cloud.servicebroker.model.binding.CreateServiceInstanceBindingRequest
import org.springframework.cloud.servicebroker.model.instance.CreateServiceInstanceRequest
import javax.validation.constraints.NotEmpty

data class ServiceBinding(

    @NotEmpty
    val bindingId: String,

    @NotEmpty
    val serviceInstanceId: String,

    val serviceDefinitionId: String?,
    var planId: String?,

    val originatingIdentity: Context,
    val parameters: MutableMap<String, Any>,
    val bindResource: MutableMap<String, Any>,
    val context: Context?,
    var deleted: Boolean = false
) {
  constructor(request: CreateServiceInstanceBindingRequest) : this(
      bindingId = request.bindingId,
      serviceInstanceId = request.serviceInstanceId,
      context = request.context,
      originatingIdentity = request.originatingIdentity,
      parameters = request.parameters ?: mutableMapOf(),
      bindResource = request.bindResource?.properties ?: mutableMapOf(),
      planId = request.planId,
      serviceDefinitionId = request.serviceDefinitionId
  )
}
