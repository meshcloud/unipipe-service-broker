package io.meshcloud.dockerosb.metrics

data class ServiceInstanceDatapoints<T>(
    val serviceInstanceId: String,
    val resource: String,
    var values: List<T>
)