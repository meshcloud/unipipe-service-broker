package io.meshcloud.dockerosb.persistence

import io.meshcloud.dockerosb.model.ServiceInstance
import io.meshcloud.dockerosb.model.Status
import org.springframework.cloud.servicebroker.model.instance.OperationState
import org.springframework.jmx.support.MetricType
import org.springframework.stereotype.Component
import java.io.File
import java.time.Instant

@Component
class ServiceInstanceRepository(private val yamlHandler: YamlHandler, private val gitHandler: GitHandler) {

  fun createServiceInstance(serviceInstance: ServiceInstance) {
    val serviceInstanceId = serviceInstance.serviceInstanceId

    val instanceYml = serviceInstanceYmlFile(serviceInstanceId)

    yamlHandler.writeObject(
        objectToWrite = serviceInstance,
        file = instanceYml
    )
    gitHandler.commitAllChanges(
        commitMessage = "Created Service instance $serviceInstanceId"
    )
  }

  // TODO Check if an update request is allowed. See https://github.com/meshcloud/unipipe-service-broker/pull/35/files#r651527916
  // Right now we don't apply any validation and trust the marketplace to know what it's doing.
  //
  //  There are several actions that can be [triggered via an update request](https://github.com/openservicebrokerapi/servicebroker/blob/master/spec.md#updating-a-service-instance):
  //- updating the plan a service instance is using, if `plan_updateable` is true
  //- updating the context object of a service instance, if `allow_context_updates` is true
  //- applying a maintenance update, if the service broker previously provided `maintenance_info` to the platform.
  fun updateServiceInstance(serviceInstance: ServiceInstance): Status {
    val serviceInstanceId = serviceInstance.serviceInstanceId

    val instanceYml = serviceInstanceYmlFile(serviceInstanceId)
    yamlHandler.writeObject(
      objectToWrite = serviceInstance,
      file = instanceYml
    )

    val statusYml = serviceInstanceStatusYmlFile(serviceInstanceId)
    val status = Status("in progress", "updating service")
    yamlHandler.writeObject(
        objectToWrite = status,
        file = statusYml
    )

    gitHandler.commitAllChanges(
      commitMessage = "Updated Service instance $serviceInstanceId"
    )

    return status
  }

  fun deleteServiceInstance(serviceInstance: ServiceInstance) {
    val serviceInstanceId = serviceInstance.serviceInstanceId

    val instanceYml = serviceInstanceYmlFile(serviceInstanceId)
    serviceInstance.deleted = true
    yamlHandler.writeObject(
        objectToWrite = serviceInstance,
        file = instanceYml
    )

    val statusYml = serviceInstanceStatusYmlFile(serviceInstanceId)
    val status = Status("in progress", "preparing service deletion")
    yamlHandler.writeObject(
        objectToWrite = status,
        file = statusYml
    )

    gitHandler.commitAllChanges(commitMessage = "Marked Service instance $serviceInstanceId as deleted.")
  }

  fun tryGetServiceInstance(serviceInstanceId: String): ServiceInstance? {
    val instanceYml = serviceInstanceYmlFile(serviceInstanceId)

    if (!instanceYml.exists()) {
      return null
    }

    return yamlHandler.readObject(instanceYml, ServiceInstance::class.java)
  }

  fun getServiceInstanceStatus(serviceInstanceId: String): Status {
    val statusYml = serviceInstanceStatusYmlFile(serviceInstanceId)

    return when (statusYml.exists()) {
      true -> yamlHandler.readObject(statusYml, Status::class.java)
      else -> Status(
          status = OperationState.IN_PROGRESS.value,
          description = "preparing deployment"
      )
    }
  }

  private fun serviceInstanceYmlFile(serviceInstanceId: String): File {
    val instanceYmlPath = instanceFolderPath(serviceInstanceId) + "/instance.yml"

    return gitHandler.fileInRepo(instanceYmlPath)
  }

  private fun serviceInstanceStatusYmlFile(serviceInstanceId: String): File {
    val instanceYmlPath = instanceFolderPath(serviceInstanceId) + "/status.yml"

    return gitHandler.fileInRepo(instanceYmlPath)
  }

  private fun instanceFolderPath(serviceInstanceId: String): String {
    return "instances/$serviceInstanceId"
  }
}

