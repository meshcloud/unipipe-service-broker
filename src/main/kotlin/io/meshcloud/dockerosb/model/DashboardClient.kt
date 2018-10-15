package io.meshcloud.dockerosb.model

import org.springframework.cloud.servicebroker.model.catalog.DashboardClient

data class DashboardClient(
    val id: String,
    val secret: String,
    val redirectUri: String
) {
  constructor(client: DashboardClient) : this(
      id = client.id,
      secret = client.secret,
      redirectUri = client.redirectUri
  )
}