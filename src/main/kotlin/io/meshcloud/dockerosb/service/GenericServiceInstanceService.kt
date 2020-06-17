package io.meshcloud.dockerosb.service

import io.meshcloud.dockerosb.isSynchronousService
import io.meshcloud.dockerosb.model.ServiceInstance
import io.meshcloud.dockerosb.model.Status
import io.meshcloud.dockerosb.persistence.GitHandler
import io.meshcloud.dockerosb.persistence.YamlHandler
import org.springframework.cloud.servicebroker.model.catalog.Catalog
import org.springframework.cloud.servicebroker.model.instance.*
import org.springframework.cloud.servicebroker.service.ServiceInstanceService
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono

@Service
class GenericServiceInstanceService(
    private val yamlHandler: YamlHandler,
    private val gitHandler: GitHandler,
    private val catalog: Catalog
) : ServiceInstanceService {

  override fun createServiceInstance(request: CreateServiceInstanceRequest): Mono<CreateServiceInstanceResponse> {

    if (catalog.isSynchronousService(request.serviceDefinitionId)) {
      return Mono.just(
          CreateServiceInstanceResponse.builder()
              .async(false)
              .build()
      )
    }

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

    return Mono.just(
        CreateServiceInstanceResponse.builder()
            .async(true)
            .operation("creating service")
            .build()
    )
  }

  override fun getServiceInstance(request: GetServiceInstanceRequest): Mono<GetServiceInstanceResponse> {
    return Mono.just(
        GetServiceInstanceResponse.builder()
            .serviceDefinitionId("<serviceDefinitionId>")
            .planId("<planId>")
            .dashboardUrl("http://localhost:8080")
            .parameters(
                mapOf(
                    "textarea" to "Any text",
                    "checkbox" to true,
                    "conditionaltextarea" to "Any conditional text",
                    "dropdown" to "option2",
                    "array" to arrayOf(
                      mapOf(
                        "mapper" to "groups",
                        "mapper_access_token" to false,
                        "mapper_id_token" to true,
                        "mapper_userinfo" to false
                      ),
                      mapOf(
                        "mapper" to "duns",
                        "mapper_access_token" to true,
                        "mapper_id_token" to true,
                        "mapper_userinfo" to true
                      )
                    )
                )
            )
            .build()
    )
  }

  override fun getLastOperation(request: GetLastServiceOperationRequest): Mono<GetLastServiceOperationResponse> {
    if (catalog.isSynchronousService(request.serviceDefinitionId)) {
      return Mono.just(
          GetLastServiceOperationResponse.builder()
              .operationState(OperationState.SUCCEEDED)
              .build()
      )
    }

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

    return Mono.just(
        GetLastServiceOperationResponse.builder()
            .operationState(status)
            .description(description)
            .build()
    )
  }

  override fun deleteServiceInstance(request: DeleteServiceInstanceRequest): Mono<DeleteServiceInstanceResponse> {

    if (catalog.isSynchronousService(request.serviceDefinitionId)) {
      return Mono.just(
          DeleteServiceInstanceResponse.builder()
              .async(false)
              .build()
      )
    }

    gitHandler.pull()
    val instanceYmlPath = "instances/${request.serviceInstanceId}/instance.yml"
    val instanceYml = gitHandler.fileInRepo(instanceYmlPath)
    if (!instanceYml.exists()) {
      return Mono.just(
          DeleteServiceInstanceResponse.builder()
              .async(false)
              .build()
      )
    }

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

    return Mono.just(
        DeleteServiceInstanceResponse.builder()
            .async(true)
            .operation("deleting service")
            .build()
    )
  }

  override fun updateServiceInstance(request: UpdateServiceInstanceRequest): Mono<UpdateServiceInstanceResponse> {
    return Mono.just(
        UpdateServiceInstanceResponse.builder()
            .async(false)
            .build()
    )
  }
}
