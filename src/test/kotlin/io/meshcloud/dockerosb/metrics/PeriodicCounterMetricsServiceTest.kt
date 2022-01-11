package io.meshcloud.dockerosb.metrics

import io.meshcloud.dockerosb.ServiceBrokerFixture
import io.meshcloud.dockerosb.metrics.periodiccounter.PeriodicCounterController
import io.meshcloud.dockerosb.metrics.periodiccounter.PeriodicCounterMetricProvider
import io.meshcloud.dockerosb.persistence.ServiceInstanceRepository
import io.meshcloud.dockerosb.service.GenericCatalogService
import org.apache.commons.io.FileUtils
import org.junit.*
import org.springframework.cloud.servicebroker.model.catalog.Catalog
import java.io.File
import java.time.Instant

class PeriodicCounterMetricsServiceTest {
  private val serviceDefinitionId = "d40133dd-8373-4c25-8014-fde98f38a728"
  private val serviceInstanceId = "testInstanceID"
  private val serviceInstanceId2 = "testInstanceID2"
  private val resource1 = "test"
  private val resource2 = "testSecond"
  private val firstStartDate = Instant.parse("2018-01-01T12:00:00Z")
  private val secondStartDate = Instant.parse("2020-01-01T12:00:00Z")
  private val endDate = Instant.parse("2022-12-01T12:00:00Z")

  companion object {
    private lateinit var periodicCounterController: PeriodicCounterController
    private var fixture: ServiceBrokerFixture = ServiceBrokerFixture("src/test/resources/catalog.yml")
    var serviceInstanceRepository: ServiceInstanceRepository = ServiceInstanceRepository(fixture.yamlHandler, fixture.gitHandler)
    var catalogService: GenericCatalogService = GenericCatalogService(fixture.contextFactory)

    @BeforeClass
    @JvmStatic
    fun beforeClass() {
      FileUtils.copyDirectory(File("src/test/resources/instances"), File("${fixture.localGitPath}/instances"))
      periodicCounterController = PeriodicCounterController(listOf(PeriodicCounterMetricProvider(catalogService.catalog.block() as Catalog, serviceInstanceRepository)))
    }

    @AfterClass
    @JvmStatic
    fun afterClass() {
      fixture.close()
      FileUtils.deleteDirectory(File(fixture.localGitPath))
    }
  }

  @Test
  fun canReadMultipleFiles(){
    // can read multiple files for the same serviceInstanceId
    val test1 = periodicCounterController.getPeriodicCounterMetricValues(serviceDefinitionId, firstStartDate, endDate).body?.dataPoints?.find { serviceInstanceDatapoints -> (serviceInstanceDatapoints.serviceInstanceId == serviceInstanceId && serviceInstanceDatapoints.resource==resource1 )}
    Assert.assertEquals(serviceInstanceId,test1?.serviceInstanceId)
    Assert.assertEquals(resource1, test1?.resource)
    Assert.assertEquals(6, test1?.values?.count())
  }

  @Test
  fun canReadMultipleFilesWithTimeFilter(){
    val test2 = periodicCounterController.getPeriodicCounterMetricValues(serviceDefinitionId, secondStartDate, endDate).body?.dataPoints?.find { serviceInstanceDatapoints -> (serviceInstanceDatapoints.serviceInstanceId == serviceInstanceId && serviceInstanceDatapoints.resource==resource1 ) }
    Assert.assertEquals(serviceInstanceId,test2?.serviceInstanceId)
    Assert.assertEquals(resource1, test2?.resource)
    Assert.assertEquals(2, test2?.values?.count())
  }

  @Test
  fun canSeparateOtherServiceInstanceIds(){
    // can separate other serviceInstanceIds
    val test3 = periodicCounterController.getPeriodicCounterMetricValues(serviceDefinitionId, firstStartDate, endDate).body?.dataPoints?.find { serviceInstanceDatapoints -> (serviceInstanceDatapoints.serviceInstanceId == serviceInstanceId2 && serviceInstanceDatapoints.resource==resource1 ) }
    Assert.assertEquals(serviceInstanceId2,test3?.serviceInstanceId)
    Assert.assertEquals(resource1, test3?.resource)
    Assert.assertEquals(3, test3?.values?.count())
  }

  @Test
  fun canSeparateOtherServiceInstanceIdsWithTimeFilter(){
    // can separate other serviceInstanceIds and time filter works
    val test4 = periodicCounterController.getPeriodicCounterMetricValues(serviceDefinitionId, secondStartDate, endDate).body?.dataPoints?.find { serviceInstanceDatapoints -> (serviceInstanceDatapoints.serviceInstanceId == serviceInstanceId2 && serviceInstanceDatapoints.resource==resource1 ) }
    Assert.assertEquals(serviceInstanceId2,test4?.serviceInstanceId)
    Assert.assertEquals(resource1, test4?.resource)
    Assert.assertEquals(1, test4?.values?.count())
  }

  @Test
  fun canSeparateDifferentResources(){
    // can separate different resources for the serviceInstanceId
    val test5 = periodicCounterController.getPeriodicCounterMetricValues(serviceDefinitionId, firstStartDate, endDate).body?.dataPoints?.find { serviceInstanceDatapoints -> (serviceInstanceDatapoints.serviceInstanceId == serviceInstanceId && serviceInstanceDatapoints.resource==resource2 ) }
    Assert.assertEquals(serviceInstanceId,test5?.serviceInstanceId)
    Assert.assertEquals(resource2, test5?.resource)
    Assert.assertEquals(4, test5?.values?.count())
  }

  @Test
  fun canSeparateDifferentResourcesWithTimeFilter(){
    // can separate different resources for the serviceInstanceId and time filter works
    val test6 = periodicCounterController.getPeriodicCounterMetricValues(serviceDefinitionId, secondStartDate, endDate).body?.dataPoints?.find { serviceInstanceDatapoints -> (serviceInstanceDatapoints.serviceInstanceId == serviceInstanceId && serviceInstanceDatapoints.resource==resource2 ) }
    Assert.assertEquals(serviceInstanceId,test6?.serviceInstanceId)
    Assert.assertEquals(resource2, test6?.resource)
    Assert.assertEquals(2, test6?.values?.count())
  }
}
