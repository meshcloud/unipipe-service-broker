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

      val serviceInstance = ServiceInstance(request)

      val repository = context.buildServiceInstanceRepository()
      repository.createServiceInstance(serviceInstance)

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

      val repository = context.buildServiceInstanceRepository()
      val instance = repository.getServiceInstance(request.serviceInstanceId)

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

      val repository = context.buildServiceInstanceRepository()
      val instanceStatus = repository.getServiceInstanceStatus(request.serviceInstanceId)

      return Mono.just(
          GetLastServiceOperationResponse.builder()
              .operationState(instanceStatus.toOperationState())
              .description(instanceStatus.description)
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

      val repository = context.buildServiceInstanceRepository()
      val instance = repository.tryGetServiceInstance(request.serviceInstanceId)

      if (instance == null || instance.deleted)
        return Mono.just(
            DeleteServiceInstanceResponse.builder()
                .async(false)
                .build()
        )

      repository.deleteServiceInstance(instance)


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