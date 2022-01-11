package io.meshcloud.dockerosb.metrics

import org.springframework.hateoas.RepresentationModel


data class MetricsResponse<T>(
    val dataPoints: List<ServiceInstanceDatapoints<T>>
) : RepresentationModel<MetricsResponse<T>>()
