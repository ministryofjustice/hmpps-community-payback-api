package uk.gov.justice.digital.hmpps.communitypaybackapi.architecture

import com.lemonappdev.konsist.api.Konsist
import com.lemonappdev.konsist.api.architecture.KoArchitectureCreator.assertArchitecture
import com.lemonappdev.konsist.api.architecture.Layer
import org.junit.jupiter.api.Test

class ArchitectureTest {
  @Test
  fun `Ensure packages are correctly isolated`() {
    Konsist.scopeFromProduction()
      .assertArchitecture {
        val controller = Layer("controller", "..communitypaybackapi.controller..")
        val dto = Layer("dto", "..communitypaybackapi.dto..")
        val service = Layer("service", "..communitypaybackapi.service..")
        val serviceInternal = Layer("service.internal", "..communitypaybackapi.service.internal..")
        val serviceMappers = Layer("service.mappers", "..communitypaybackapi.service.mappers..")
        val listener = Layer("listener", "..communitypaybackapi.listener..")
        val client = Layer("client", "..communitypaybackapi.client..")

        dto.dependsOnNothing()
        controller.doesNotDependOn(client, serviceInternal, serviceMappers)
        service.doesNotDependOn(controller)
        listener.doesNotDependOn(controller)
        client.dependsOnNothing()
      }
  }
}
