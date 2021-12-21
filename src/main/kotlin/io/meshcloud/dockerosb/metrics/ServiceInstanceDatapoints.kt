package io.meshcloud.dockerosb.metrics

import io.meshcloud.dockerosb.metrics.inplace.InplaceMetricModel

data class ServiceInstanceDatapoints<T>(
    val serviceInstanceId: String,
    val resource: String,
    val values: List<T>
)