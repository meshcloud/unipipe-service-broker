package io.meshcloud.dockerosb.service

import io.meshcloud.dockerosb.model.ServiceInstance
import io.meshcloud.dockerosb.model.Status
import io.meshcloud.dockerosb.persistence.GitHandler
import io.meshcloud.dockerosb.persistence.YamlHandler
import org.springframework.cloud.servicebroker.model.instance.*
import org.springframework.cloud.servicebroker.service.ServiceInstanceService
import org.springframework.stereotype.Service

@Service
class GenericServiceInstanceService(
    private val yamlHandler: YamlHandler,
    private val gitHandler: GitHandler
) : ServiceInstanceService {

  override fun createServiceInstance(request: CreateServiceInstanceRequest): CreateServiceInstanceResponse {
    gitHandler.pull()
    val instanceYmlPath = "instances/${request.serviceInstanceId}/instance.yml"
    val instanceYml = gitHandler.fileInRepo(instanceYmlPath)
    yamlHandler.writeObject(
        objectToWrite = ServiceInstance(request),
        file = instanceYml
    )
    gitHandler.commitAndPushChanges(
        filePaths = listOf(instanceYmlPath),
        commitMessage = "Created Service instance ${request.serviceInstanceId}"
    )

    return CreateServiceInstanceResponse.builder()
        .async(true)
        .operation("creating service")
        .build()
  }

  override fun getLastOperation(request: GetLastServiceOperationRequest): GetLastServiceOperationResponse {
    gitHandler.pull()
    val statusPath = "instances/${request.serviceInstanceId}/status.yml"
    val statusYml = gitHandler.fileInRepo(statusPath)
    var status = OperationState.IN_PROGRESS
    var description = "preparing deployment"
    if (statusYml.exists()) {
      val retrievedStatus = yamlHandler.readObject(statusYml, Status::class.java)
      status = when (retrievedStatus.status) {
        "succeeded" -> OperationState.SUCCEEDED
        "failed" -> OperationState.FAILED
        else -> OperationState.IN_PROGRESS
      }
      description = retrievedStatus.description
    }
    return GetLastServiceOperationResponse.builder()
        .operationState(status)
        .description(description)
        .build()
  }

  override fun deleteServiceInstance(request: DeleteServiceInstanceRequest): DeleteServiceInstanceResponse {
    gitHandler.pull()
    val instanceYmlPath = "instances/${request.serviceInstanceId}/instance.yml"
    val instanceYml = gitHandler.fileInRepo(instanceYmlPath)
    val instance = yamlHandler.readObject(instanceYml, ServiceInstance::class.java)
    instance.deleted = true
    yamlHandler.writeObject(
        objectToWrite = instance,
        file = instanceYml
    )

    val statusPath = "instances/${request.serviceInstanceId}/status.yml"
    val statusYml = gitHandler.fileInRepo(statusPath)
    val status = Status("in progress", "preparing service deletion")
    yamlHandler.writeObject(
        objectToWrite = status,
        file = statusYml
    )

    gitHandler.commitAndPushChanges(
        filePaths = listOf(instanceYmlPath, statusPath),
        commitMessage = "Marked Service instance ${request.serviceInstanceId} as deleted."
    )

    return DeleteServiceInstanceResponse.builder()
        .async(true)
        .operation("deleting service")
        .build()
  }

}
