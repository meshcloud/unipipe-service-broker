package io.meshcloud.dockerosb.service

import io.meshcloud.dockerosb.model.ServiceBinding
import io.meshcloud.dockerosb.persistence.GitOperationContextFactory
import org.springframework.cloud.servicebroker.exception.ServiceBrokerAsyncRequiredException
import org.springframework.cloud.servicebroker.model.binding.*
import org.springframework.cloud.servicebroker.service.ServiceInstanceBindingService
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono

@Service
class GenericServiceInstanceBindingService(
    private val gitContextFactory: GitOperationContextFactory
) : ServiceInstanceBindingService {


  override fun createServiceInstanceBinding(request: CreateServiceInstanceBindingRequest): Mono<CreateServiceInstanceBindingResponse> {
    if (!request.isAsyncAccepted){
      throw ServiceBrokerAsyncRequiredException("UniPipe service broker invokes async CI/CD pipelines")
    }

    gitContextFactory.acquireContext().use { context ->
      val repository = context.buildServiceInstanceBindingRepository()

      val serviceBinding = ServiceBinding(request)
      repository.createBinding(serviceBinding)

      return Mono.just(
          CreateServiceInstanceAppBindingResponse.builder()
              .async(true)
              .operation("creating binding")
              .bindingExisted(false)
              .build()
      )
    }
  }

  override fun deleteServiceInstanceBinding(request: DeleteServiceInstanceBindingRequest): Mono<DeleteServiceInstanceBindingResponse> {
    gitContextFactory.acquireContext().use { context ->
      val repository = context.buildServiceInstanceBindingRepository()
      val binding = repository.tryGetServiceBinding(request.serviceInstanceId, request.bindingId)

      if (binding == null || binding.deleted)
        return Mono.just(
            DeleteServiceInstanceBindingResponse.builder()
                .async(false)
                .build()
        )

      if (!request.isAsyncAccepted){
        throw ServiceBrokerAsyncRequiredException("UniPipe service broker invokes async CI/CD pipelines")
      }

      repository.deleteServiceInstanceBinding(binding)

      return Mono.just(
          DeleteServiceInstanceBindingResponse.builder()
              .async(true)
              .operation("deleting")
              .build()
      )
    }
  }

  override fun getLastOperation(request: GetLastServiceBindingOperationRequest): Mono<GetLastServiceBindingOperationResponse> {
    gitContextFactory.acquireContext().use { context ->
      context.attemptToRefreshRemoteChanges()

      val repository = context.buildServiceInstanceBindingRepository()

      val status = repository.getServiceBindingStatus(request.serviceInstanceId, request.bindingId)

      return Mono.just(
          GetLastServiceBindingOperationResponse.builder()
              .operationState(status.toOperationState())
              .description(status.description)
              .build()
      )
    }
  }

  override fun getServiceInstanceBinding(request: GetServiceInstanceBindingRequest): Mono<GetServiceInstanceBindingResponse> {
    gitContextFactory.acquireContext().use { context ->
      context.attemptToRefreshRemoteChanges()

      val repository = context.buildServiceInstanceBindingRepository()

      val credentials = repository.getServiceBindingCredentials(request.serviceInstanceId, request.bindingId)

      return Mono.just(
          GetServiceInstanceAppBindingResponse.builder()
              .credentials(credentials)
              .build()
      )
    }
  }
}