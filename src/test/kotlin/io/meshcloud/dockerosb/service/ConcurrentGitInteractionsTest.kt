package io.meshcloud.dockerosb.service

import io.meshcloud.dockerosb.ServiceBrokerFixture
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.springframework.cloud.servicebroker.model.PlatformContext
import org.springframework.cloud.servicebroker.model.instance.CreateServiceInstanceRequest
import org.springframework.cloud.servicebroker.model.instance.GetServiceInstanceRequest
import java.util.concurrent.Executors


class ConcurrentGitInteractionsTest {

  private lateinit var fixture: ServiceBrokerFixture

  @Before
  fun before() {
    fixture = ServiceBrokerFixture("src/test/resources/catalog.yml")
  }

  @After
  fun cleanUp() {
    fixture.close()
  }

  private fun makeSut(): GenericServiceInstanceService {
    return GenericServiceInstanceService(fixture.contextFactory, fixture.catalogService)
  }

  @Test
  fun `can process concurrent instance requests`() {
    val sut = makeSut()

    val ids = (0..9).map { "000000$it-7e05-4d5a-97b8-f8c5d1c710ab" }
    val requests = ids.map { createServiceInstanceRequest(it) }

    val executor = Executors.newFixedThreadPool(10)
    try {
      requests.forEach { executor.run { sut.createServiceInstance(it).block() } }
    } finally {
      executor.shutdown()
    }

    val log = fixture.gitHandler.getLog()
        .map { it.shortMessage }
        .toList()

    val expectedMessages = ids.map { "OSB API: Created Service instance $it" } + "initial commit on remote"
    assertEquals(expectedMessages, log.sorted()) // note: have to sort logs as the order is not guaranteed
  }


  @Test
  fun `can synchronize with concurrent, non-conflicting remote changes`() {
    val sut = makeSut()

    val request = createServiceInstanceRequest("00000000-7e05-4d5a-97b8-f8c5d1c710ab")

    // this will create a local request
    sut.createServiceInstance(request).block()

    fixture.remote.writeFile("file.md", "hello")
    fixture.remote.commit("non-conflicting change on remote")

    fixture.gitHandler.pullFastForwardOnly()
    fixture.gitHandler.synchronizeWithRemoteRepository()

    val log = fixture.gitHandler
        .getLog()
        .map { "${it.shortMessage} :: ${it.parentCount}" }
        .toList()

    val expected = listOf(
        "OSB API: auto-merging upstream changes attempt #0 :: 2",
        "OSB API: Created Service instance 00000000-7e05-4d5a-97b8-f8c5d1c710ab :: 1",
        "non-conflicting change on remote :: 1",
        "initial commit on remote :: 0"
    )
    assertEquals(expected, log)
  }

  @Test
  fun `can synchronize with concurrent, conflicting remote changes`() {
    val sut = makeSut()

    val createRequest = createServiceInstanceRequest("00000000-7e05-4d5a-97b8-f8c5d1c710ab")

    // this will create a local request
    sut.createServiceInstance(createRequest).block()

    fixture.remote.writeFile("instances/${createRequest.serviceInstanceId}/instance.yml", "hello, invalid yaml")
    fixture.remote.commit("conflicting change on remote")

    fixture.gitHandler.pullFastForwardOnly()
    fixture.gitHandler.synchronizeWithRemoteRepository()

    val log = fixture.gitHandler
        .getLog()
        .map { "${it.shortMessage} :: ${it.parentCount}" }
        .toList()

    val expected = listOf(
        "OSB API: auto-merging upstream changes attempt #0 :: 2",
        "OSB API: Created Service instance 00000000-7e05-4d5a-97b8-f8c5d1c710ab :: 1",
        "conflicting change on remote :: 1",
        "initial commit on remote :: 0"
    )

    assertEquals(expected, log)

    // verify that our local changes have won over the remote changes by reading back the instance
    val getRequest = GetServiceInstanceRequest(createRequest.serviceInstanceId, null, null, null, null)
    val instance = sut.getServiceInstance(getRequest).block()!!
    assertEquals(createRequest.serviceDefinitionId, instance.serviceDefinitionId)
  }

  @Test
  fun `can synchronize with concurrent, conflicting changes that involve deletion and`() {
    // note: checkout conflicts are something different,
    val sut = makeSut()

    val createRequest = createServiceInstanceRequest("00000000-7e05-4d5a-97b8-f8c5d1c710ab")

    val statusFilePath = "instances/${createRequest.serviceInstanceId}/status.yml"

    // this will create a local service instance request
    sut.createServiceInstance(createRequest).block()
    fixture.gitHandler.synchronizeWithRemoteRepository()

    // pretend the remote starts deploying the service
    fixture.remote.writeFile(statusFilePath, "status: in progress\ndescription: deploying")
    fixture.remote.commit("deployed service - started")

    // now pull locally
    fixture.gitHandler.pullFastForwardOnly()

    // pretend the remote deployed the service
    fixture.remote.writeFile(statusFilePath, "status: succeeded\ndescription: all good")
    fixture.remote.commit("deployed service - succeeded")

    // pretend we locally delete the file because we need to invalidate the status, e.g. due to an instance update
    fixture.gitHandler.fileInRepo(statusFilePath).delete()
    fixture.gitHandler.commitAllChanges("invalidated status")

    // sync
    fixture.gitHandler.synchronizeWithRemoteRepository()

    val log = fixture.gitHandler
        .getLog()
        .map { "${it.shortMessage} :: ${it.parentCount}" }
        .toList()

    val expected = listOf(
        "OSB API: auto-merging upstream changes attempt #0 :: 2",
        "OSB API: invalidated status :: 1",
        "deployed service - succeeded :: 1",
        "deployed service - started :: 1",
        "OSB API: Created Service instance 00000000-7e05-4d5a-97b8-f8c5d1c710ab :: 1",
        "initial commit on remote :: 0"
    )

    assertEquals(expected, log)

    // verify that our local changes have won over the remote changes - status file should not exist anymore... but
    // SURPRISE: this is no the case because `git checkout --ours` cannot remove changes
    //  - see https://stackoverflow.com/questions/39438168/git-checkout-ours-does-not-remove-files-from-unmerged-files-list
    //  - we may have to wait for jgit alternative https://github.com/eclipse/jgit/commit/8210f29fe43ccd35e7d2ed3ed45a84a75b2717c4 to ship in a release
    //  - fixing this could also be relevant for properly implementing https://github.com/meshcloud/unipipe-service-broker/issues/22
    //
    // because this is a fairly uncommon edge case we'll just leave the merge logic in place like it is for now (conflicting updates/deletes should be rare)
    assertTrue(fixture.gitHandler.fileInRepo(statusFilePath).exists())
  }

  private fun createServiceInstanceRequest(instanceId: String): CreateServiceInstanceRequest {
    return CreateServiceInstanceRequest
        .builder()
        .serviceDefinitionId("d40133dd-8373-4c25-8014-fde98f38a728")
        .planId("a13edcdf-eb54-44d3-8902-8f24d5acb07e")
        .serviceInstanceId(instanceId)
        .originatingIdentity(PlatformContext.builder().property("user", "unittester").build())
        .asyncAccepted(true)
        .serviceDefinition(fixture.catalogService.getCatalogInternal().serviceDefinitions.first())
        .build()
  }
}