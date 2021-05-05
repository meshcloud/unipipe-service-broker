package io.meshcloud.dockerosb.persistence

import com.fasterxml.jackson.core.type.TypeReference
import io.meshcloud.dockerosb.model.ServiceBinding
import io.meshcloud.dockerosb.model.Status
import org.springframework.cloud.servicebroker.model.instance.OperationState
import java.io.File
import java.util.HashMap

class ServiceInstanceBindingRepository(private val yamlHandler: YamlHandler, private val gitHandler: GitHandler) {
  fun createBinding(serviceBinding: ServiceBinding) {
    val bindingYml = bindingYmlFile(serviceBinding.serviceInstanceId, serviceBinding.bindingId)

    yamlHandler.writeObject(
        objectToWrite = serviceBinding,
        file = bindingYml
    )

    gitHandler.commitAllChanges(
        commitMessage = "Created Service binding ${serviceBinding.bindingId}"
    )
  }

  fun getServiceBindingStatus(serviceInstanceId: String, bindingId: String): Status {
    val statusYml = statusYmlFile(serviceInstanceId, bindingId)

    return when (statusYml.exists()) {
      true -> yamlHandler.readObject(statusYml, Status::class.java)
      else -> Status(
          status = OperationState.IN_PROGRESS.value,
          description = "preparing binding"
      )
    }
  }

  fun getServiceBindingCredentials(serviceInstanceId: String, bindingId: String): Map<String, Any> {
    val credentialsYml = credentialsYmlFile(serviceInstanceId, bindingId)

    return when (credentialsYml.exists()) {
      true -> {
        val typeRef = object : TypeReference<HashMap<String, Any>>() {}
        yamlHandler.readObject(credentialsYml, typeRef)
      }
      else -> emptyMap()
    }
  }

  fun tryGetServiceBinding(serviceInstanceId: String, bindingId: String): ServiceBinding? {
    val bindingYml = bindingYmlFile(serviceInstanceId, bindingId)

    if (!bindingYml.exists()) {
      return null
    }

    return yamlHandler.readObject(bindingYml, ServiceBinding::class.java)
  }

  fun deleteServiceInstanceBinding(binding: ServiceBinding) {
    binding.deleted = true
    val bindingYml = bindingYmlFile(binding.serviceInstanceId, binding.bindingId)
    yamlHandler.writeObject(
        objectToWrite = binding,
        file = bindingYml
    )

    val statusYml = statusYmlFile(binding.serviceInstanceId, binding.bindingId)
    val status = Status("in progress", "preparing binding deletion")
    yamlHandler.writeObject(
        objectToWrite = status,
        file = statusYml
    )

    gitHandler.commitAllChanges(
        commitMessage = "Marked Service binding ${binding.bindingId} as deleted."
    )
  }

  private fun bindingYmlFile(serviceInstanceId: String, bindingId: String): File {
    val bindingYmlPath = bindingFolderPath(serviceInstanceId, bindingId) + "/binding.yml"

    return gitHandler.fileInRepo(bindingYmlPath)
  }

  private fun statusYmlFile(serviceInstanceId: String, bindingId: String): File {
    val statusYmlPath = bindingFolderPath(serviceInstanceId, bindingId) + "/status.yml"

    return gitHandler.fileInRepo(statusYmlPath)
  }

  private fun credentialsYmlFile(serviceInstanceId: String, bindingId: String): File {
    val credentialsYmlPath = bindingFolderPath(serviceInstanceId, bindingId) + "/credentials.yml"

    return gitHandler.fileInRepo(credentialsYmlPath)
  }

  private fun bindingFolderPath(serviceInstanceId: String, bindingId: String): String {
    return "instances/${serviceInstanceId}/bindings/${bindingId}"
  }
}