package io.meshcloud.dockerosb.persistence

import io.meshcloud.dockerosb.model.ServiceInstance
import io.meshcloud.dockerosb.model.Status
import org.springframework.cloud.servicebroker.model.instance.OperationState
import java.io.File

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

  fun getServiceInstance(serviceInstanceId: String): ServiceInstance {
    val instanceYml = serviceInstanceYmlFile(serviceInstanceId)

    return yamlHandler.readObject(instanceYml, ServiceInstance::class.java)
  }

  fun tryGetServiceInstance(serviceInstanceId: String): ServiceInstance? {
    val instanceYml = serviceInstanceYmlFile(serviceInstanceId)

    if (!instanceYml.exists()) {
      return null
    }

    return yamlHandler.readObject(instanceYml, ServiceInstance::class.java)
  }

  private fun serviceInstanceYmlFile(serviceInstanceId: String): File {
    val instanceYmlPath = "instances/$serviceInstanceId/instance.yml"

    return gitHandler.fileInRepo(instanceYmlPath)
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

  private fun serviceInstanceStatusYmlFile(serviceInstanceId: String): File {
    val instanceYmlPath = "instances/$serviceInstanceId/status.yml"

    return gitHandler.fileInRepo(instanceYmlPath)
  }
}