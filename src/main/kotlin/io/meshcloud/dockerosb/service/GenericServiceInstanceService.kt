package io.meshcloud.dockerosb.service

import io.meshcloud.dockerosb.model.ServiceInstance
import io.meshcloud.dockerosb.persistence.GitOperationContextFactory
import org.springframework.cloud.servicebroker.exception.ServiceBrokerAsyncRequiredException
import org.springframework.cloud.servicebroker.exception.ServiceBrokerDeleteOperationInProgressException
import org.springframework.cloud.servicebroker.exception.ServiceInstanceDoesNotExistException
import org.springframework.cloud.servicebroker.model.instance.*
import org.springframework.cloud.servicebroker.service.ServiceInstanceService
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono


@Service
class GenericServiceInstanceService(
    private val gitContextFactory: GitOperationContextFactory
) : ServiceInstanceService {

  override fun createServiceInstance(request: CreateServiceInstanceRequest): Mono<CreateServiceInstanceResponse> {
    if (!request.isAsyncAccepted) {
      throw ServiceBrokerAsyncRequiredException("UniPipe service broker invokes async CI/CD pipelines")
    }

    gitContextFactory.acquireContext().use { context ->
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

  override fun updateServiceInstance(request: UpdateServiceInstanceRequest): Mono<UpdateServiceInstanceResponse> {
    if (!request.isAsyncAccepted) {
        throw ServiceBrokerAsyncRequiredException("UniPipe service broker invokes async CI/CD pipelines")
    }

    gitContextFactory.acquireContext().use { context ->
      val repository = context.buildServiceInstanceRepository()
      val existingInstance = repository.tryGetServiceInstance(request.serviceInstanceId)
          ?: throw ServiceInstanceDoesNotExistException(request.serviceInstanceId)

      val updatedInstance = existingInstance.update(request)
      val status = repository.updateServiceInstance(updatedInstance)

      return Mono.just(
          UpdateServiceInstanceResponse.builder()
              .async(true)
              .operation(status.description)
              .build()
      )
    }
  }

  override fun getServiceInstance(request: GetServiceInstanceRequest): Mono<GetServiceInstanceResponse> {
    gitContextFactory.acquireContext().use { context ->

      val repository = context.buildServiceInstanceRepository()
      val instance = repository.tryGetServiceInstance(request.serviceInstanceId)
          ?: throw ServiceInstanceDoesNotExistException(request.serviceInstanceId)

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
      context.attemptToRefreshRemoteChanges()

      val repository = context.buildServiceInstanceRepository()

      val instance = repository.tryGetServiceInstance(request.serviceInstanceId)
          ?: throw ServiceInstanceDoesNotExistException(request.serviceInstanceId)

      val instanceStatus = repository.getServiceInstanceStatus(serviceInstanceId = request.serviceInstanceId)

      return Mono.just(
          GetLastServiceOperationResponse.builder()
              .operationState(instanceStatus.toOperationState())
              .description(instanceStatus.description)
              .deleteOperation(instance.deleted)
              .build()
      )
    }
  }

  override fun deleteServiceInstance(request: DeleteServiceInstanceRequest): Mono<DeleteServiceInstanceResponse> {

    gitContextFactory.acquireContext().use { context ->
      val repository = context.buildServiceInstanceRepository()
      val instance = repository.tryGetServiceInstance(request.serviceInstanceId)
          ?: throw ServiceInstanceDoesNotExistException(request.serviceInstanceId)

      if (!request.isAsyncAccepted) {
        throw ServiceBrokerAsyncRequiredException("UniPipe service broker invokes async CI/CD pipelines")
      }

      val deletingOperation = "deleting service"
      val status = repository.getServiceInstanceStatus(request.serviceInstanceId).toOperationState()

      if (instance.deleted) {
        when (status) {
          // From the spec: Note that a re-sent DELETE request MUST return a 202 Accepted, not a 200 OK, if the delete request has not completed yet.
          OperationState.FAILED,
          OperationState.IN_PROGRESS -> throw ServiceBrokerDeleteOperationInProgressException(deletingOperation)
          // indicate the instance does not exist anymore
          OperationState.SUCCEEDED -> throw ServiceInstanceDoesNotExistException(request.serviceInstanceId)
        }
      }

      repository.deleteServiceInstance(instance)

      return Mono.just(
          DeleteServiceInstanceResponse.builder()
              .async(true)
              .operation(deletingOperation)
              .build()
      )
    }
  }

}