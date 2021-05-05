package io.meshcloud.dockerosb.model

import org.springframework.cloud.servicebroker.model.instance.OperationState

data class Status(
    var status: String = "",
    var description: String = ""
) {
  fun toOperationState(): OperationState {
    return when (status) {
      "succeeded" -> OperationState.SUCCEEDED
      "failed" -> OperationState.FAILED
      else -> OperationState.IN_PROGRESS
    }
  }
}