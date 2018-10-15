package io.meshcloud.dockerosb.service

import com.fasterxml.jackson.core.type.TypeReference
import io.meshcloud.dockerosb.model.ServiceBinding
import io.meshcloud.dockerosb.model.Status
import io.meshcloud.dockerosb.persistence.GitHandler
import io.meshcloud.dockerosb.persistence.YamlHandler
import org.springframework.cloud.servicebroker.model.binding.*
import org.springframework.cloud.servicebroker.model.instance.OperationState
import org.springframework.cloud.servicebroker.service.ServiceInstanceBindingService
import org.springframework.stereotype.Service
import java.util.HashMap

@Service
class GenericServiceInstanceBindingService(
    private val yamlHandler: YamlHandler,
    private val gitHandler: GitHandler
) : ServiceInstanceBindingService {

  override fun createServiceInstanceBinding(request: CreateServiceInstanceBindingRequest): CreateServiceInstanceBindingResponse {
    gitHandler.pull()
    val bindingYmlPath = "instances/${request.serviceInstanceId}/bindings/${request.bindingId}/binding.yml"
    val bindingYml = gitHandler.fileInRepo(bindingYmlPath)
    yamlHandler.writeObject(
        objectToWrite = ServiceBinding(request),
        file = bindingYml
    )
    gitHandler.commitAndPushChanges(
        filePaths = listOf(bindingYmlPath),
        commitMessage = "Created Service binding ${request.bindingId}"
    )

    return CreateServiceInstanceAsyncBindingResponse.builder()
        .async(true)
        .operation("creating binding")
        .bindingExisted(false)
        .build()
  }

  override fun deleteServiceInstanceBinding(request: DeleteServiceInstanceBindingRequest): DeleteServiceInstanceBindingResponse {
    gitHandler.pull()
    val bindingYmlPath = "instances/${request.serviceInstanceId}/bindings/${request.bindingId}/binding.yml"
    val bindingYml = gitHandler.fileInRepo(bindingYmlPath)
    val binding = yamlHandler.readObject(bindingYml, ServiceBinding::class.java)
    binding.deleted = true
    yamlHandler.writeObject(
        objectToWrite = binding,
        file = bindingYml
    )

    val statusPath = "instances/${request.serviceInstanceId}/bindings/${request.bindingId}/status.yml"
    val statusYml = gitHandler.fileInRepo(statusPath)
    val status = Status("in progress", "preparing binding deletion")
    yamlHandler.writeObject(
        objectToWrite = status,
        file = statusYml
    )

    gitHandler.commitAndPushChanges(
        filePaths = listOf(bindingYmlPath, statusPath),
        commitMessage = "Marked Service binding ${request.serviceInstanceId} as deleted."
    )
    return DeleteServiceInstanceBindingResponse.builder()
        .async(true)
        .operation("deleting")
        .build()
  }

  override fun getLastOperation(request: GetLastBindingOperationRequest): GetLastBindingOperationResponse {
    gitHandler.pull()
    val statusPath = "instances/${request.serviceInstanceId}/bindings/${request.serviceBindingId}/status.yml"
    val statusYml = gitHandler.fileInRepo(statusPath)
    var status = OperationState.IN_PROGRESS
    var description = "preparing binding"
    if (statusYml.exists()) {
      val retrievedStatus = yamlHandler.readObject(statusYml, Status::class.java)
      status = when (retrievedStatus.status) {
        "succeeded" -> OperationState.SUCCEEDED
        "failed" -> OperationState.FAILED
        else -> OperationState.IN_PROGRESS
      }
      description = retrievedStatus.description
    }

    return GetLastBindingOperationResponse.builder()
        .operationState(status)
        .description(description)
        .build()
  }

  override fun getServiceInstanceBinding(request: GetServiceInstanceBindingRequest): GetServiceInstanceBindingResponse {
    gitHandler.pull()
    val credentialsPath = "instances/${request.serviceInstanceId}/bindings/${request.bindingId}/credentials.yml"
    val credentialsYml = gitHandler.fileInRepo(credentialsPath)
    var credentials = emptyMap<String, Any?>()
    if (credentialsYml.exists()) {
      val typeRef = object : TypeReference<HashMap<String, Any>>() {}
      credentials = yamlHandler.readObject(credentialsYml, typeRef)
    }

    return GetServiceInstanceAppBindingResponse.builder()
        .credentials(credentials)
        .build()
  }
}