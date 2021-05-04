package io.meshcloud.dockerosb.service

import io.meshcloud.dockerosb.isSynchronousService
import io.meshcloud.dockerosb.model.ServiceInstance
import io.meshcloud.dockerosb.model.Status
import io.meshcloud.dockerosb.persistence.GitOperationContextFactory
import org.springframework.cloud.servicebroker.model.instance.*
import org.springframework.cloud.servicebroker.service.ServiceInstanceService
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono

@Service
class GenericServiceInstanceService(
    private val gitContextFactory: GitOperationContextFactory,
    private val catalogService: GenericCatalogService
) : ServiceInstanceService {

  override fun createServiceInstance(request: CreateServiceInstanceRequest): Mono<CreateServiceInstanceResponse> {
    gitContextFactory.acquireContext().use { context ->

      val catalog = catalogService.getCatalogInternal()
      if (catalog.isSynchronousService(request.serviceDefinitionId)) {
        return Mono.just(
            CreateServiceInstanceResponse.builder()
                .async(false)
                .build()
        )
      }

      val instanceYmlPath = "instances/${request.serviceInstanceId}/instance.yml"
      val instanceYml = context.gitHandler.fileInRepo(instanceYmlPath)
      context.yamlHandler.writeObject(
          objectToWrite = ServiceInstance(request),
          file = instanceYml
      )
      context.gitHandler.commit(
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
  }

  override fun getServiceInstance(request: GetServiceInstanceRequest): Mono<GetServiceInstanceResponse> {
    gitContextFactory.acquireContext().use { context ->
      val instanceYmlPath = "instances/${request.serviceInstanceId}/instance.yml"
      val instanceYml = context.gitHandler.fileInRepo(instanceYmlPath)

      val instance = context.yamlHandler.readObject(instanceYml, ServiceInstance::class.java)

      return Mono.just(
          GetServiceInstanceResponse.builder()
              .parameters(instance.parameters)
              .planId(instance.planId)
              .serviceDefinitionId(instance.serviceDefinitionId)
              .dashboardUrl(instance.serviceDefinition.dashboardClient?.redirectUri)
              .build()
      )
    }
  }

  override fun getLastOperation(request: GetLastServiceOperationRequest): Mono<GetLastServiceOperationResponse> {

    gitContextFactory.acquireContext().use { context ->
      val catalog = catalogService.getCatalogInternal()
      if (catalog.isSynchronousService(request.serviceDefinitionId)) {
        return Mono.just(
            GetLastServiceOperationResponse.builder()
                .operationState(OperationState.SUCCEEDED)
                .build()
        )
      }

      val statusPath = "instances/${request.serviceInstanceId}/status.yml"
      val statusYml = context.gitHandler.fileInRepo(statusPath)
      var status = OperationState.IN_PROGRESS
      var description = "preparing deployment"
      if (statusYml.exists()) {
        val retrievedStatus = context.yamlHandler.readObject(statusYml, Status::class.java)
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
  }

  override fun deleteServiceInstance(request: DeleteServiceInstanceRequest): Mono<DeleteServiceInstanceResponse> {
    gitContextFactory.acquireContext().use { context ->
      val catalog = catalogService.getCatalogInternal()
      if (catalog.isSynchronousService(request.serviceDefinitionId)) {
        return Mono.just(
            DeleteServiceInstanceResponse.builder()
                .async(false)
                .build()
        )
      }

      val instanceYmlPath = "instances/${request.serviceInstanceId}/instance.yml"
      val instanceYml = context.gitHandler.fileInRepo(instanceYmlPath)
      if (!instanceYml.exists()) {
        return Mono.just(
            DeleteServiceInstanceResponse.builder()
                .async(false)
                .build()
        )
      }

      val instance = context.yamlHandler.readObject(instanceYml, ServiceInstance::class.java)
      instance.deleted = true
      context.yamlHandler.writeObject(
          objectToWrite = instance,
          file = instanceYml
      )

      val statusPath = "instances/${request.serviceInstanceId}/status.yml"
      val statusYml = context.gitHandler.fileInRepo(statusPath)
      val status = Status("in progress", "preparing service deletion")
      context.yamlHandler.writeObject(
          objectToWrite = status,
          file = statusYml
      )

      context.gitHandler.commit(
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
  }

  override fun updateServiceInstance(request: UpdateServiceInstanceRequest): Mono<UpdateServiceInstanceResponse> {
    // TODO: not supported

    return Mono.just(
        UpdateServiceInstanceResponse.builder()
            .async(false)
            .build()
    )
  }
}