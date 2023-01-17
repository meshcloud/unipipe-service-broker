package io.meshcloud.dockerosb

import io.meshcloud.dockerosb.persistence.CatalogRepository
import io.meshcloud.dockerosb.persistence.YamlHandler
import org.springframework.cloud.servicebroker.model.PlatformContext
import org.springframework.cloud.servicebroker.model.binding.CreateServiceInstanceBindingRequest
import org.springframework.cloud.servicebroker.model.binding.DeleteServiceInstanceBindingRequest
import org.springframework.cloud.servicebroker.model.binding.GetServiceInstanceBindingRequest
import org.springframework.cloud.servicebroker.model.binding.GetServiceInstanceBindingRequest.GetServiceInstanceBindingRequestBuilder
import org.springframework.cloud.servicebroker.model.instance.CreateServiceInstanceRequest
import org.springframework.cloud.servicebroker.model.instance.UpdateServiceInstanceRequest
import java.io.File

class TestDataBuilder(catalogPath: String, yamlHandler: YamlHandler) {

  private val catalogFile = File(catalogPath)
  private val catalog = yamlHandler.readObject(catalogFile, CatalogRepository.YamlCatalog::class.java)


  private val originatingIdentity = PlatformContext.builder()
      .property("user", "unittester")
      .build()

  fun createServiceInstanceRequest(
      instanceId: String,
      customize: (CreateServiceInstanceRequest.CreateServiceInstanceRequestBuilder.() -> Unit)? = null
  ): CreateServiceInstanceRequest {
    val serviceDefinition = catalog.services.first()

    return CreateServiceInstanceRequest
        .builder()
        .serviceDefinition(serviceDefinition)
        .serviceDefinitionId(serviceDefinition.id)
        .planId(serviceDefinition.plans.first().id)
        .serviceInstanceId(instanceId)
        .originatingIdentity(originatingIdentity)
        .asyncAccepted(true)
        .also {
          customize?.invoke(it)
        }
        .build()
  }

    fun updateServiceInstanceRequest(
        instanceId: String,
        customize: (UpdateServiceInstanceRequest.UpdateServiceInstanceRequestBuilder.() -> Unit)? = null
    ): UpdateServiceInstanceRequest {
        val serviceDefinition = catalog.services.first()

        return UpdateServiceInstanceRequest
            .builder()
            .serviceDefinition(serviceDefinition)
            .serviceDefinitionId(serviceDefinition.id)
            .planId(serviceDefinition.plans[1].id)
            .serviceInstanceId(instanceId)
            .originatingIdentity(originatingIdentity)
            .asyncAccepted(true)
            .also {
                customize?.invoke(it)
            }
            .build()
    }


    fun createServiceInstanceBindingRequest(
      instanceId: String,
      bindingId: String,
      customize: (CreateServiceInstanceBindingRequest.CreateServiceInstanceBindingRequestBuilder.() -> Unit)? = null
  ): CreateServiceInstanceBindingRequest {
    val serviceDefinition = catalog.services.first()

    return CreateServiceInstanceBindingRequest.builder()
        .serviceDefinition(serviceDefinition)
        .serviceDefinitionId(serviceDefinition.id)
        .planId(serviceDefinition.plans.first().id)
        .serviceInstanceId(instanceId)
        .originatingIdentity(originatingIdentity)
        .asyncAccepted(true)
        .bindingId(bindingId)
        .also {
          customize?.invoke(it)
        }
        .build()
  }

  fun deleteServiceInstanceBindingRequest(
      instanceId: String,
      bindingId: String,
      customize: (DeleteServiceInstanceBindingRequest.DeleteServiceInstanceBindingRequestBuilder.() -> Unit)? = null
  ): DeleteServiceInstanceBindingRequest {
    val serviceDefinition = catalog.services.first()

    return DeleteServiceInstanceBindingRequest.builder()
        .serviceDefinition(serviceDefinition)
        .serviceDefinitionId(serviceDefinition.id)
        .planId(serviceDefinition.plans.first().id)
        .serviceInstanceId(instanceId)
        .originatingIdentity(originatingIdentity)
        .asyncAccepted(true)
        .bindingId(bindingId)
        .also {
          customize?.invoke(it)
        }
        .build()
  }

  fun getServiceInstanceBindingRequest(
      instanceId: String,
      bindingId: String,
      customize: (GetServiceInstanceBindingRequestBuilder.() -> Unit)? = null
  ): GetServiceInstanceBindingRequest {
    return GetServiceInstanceBindingRequest.builder()
        .serviceInstanceId(instanceId)
        .bindingId(bindingId)
        .originatingIdentity(originatingIdentity)
        .also {
          customize?.invoke(it)
        }
        .build()
  }
}

