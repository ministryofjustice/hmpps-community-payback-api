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
        val controller = Layer("controller", "..communitypaybackapi..controller..")
        val dto = Layer("dto", "..communitypaybackapi..dto..")
        val service = Layer("service", "..communitypaybackapi..service..")
        val client = Layer("client", "..communitypaybackapi....client..")

        dto.dependsOnNothing()
        controller.doesNotDependOn(client)
        service.doesNotDependOn(controller)
        client.dependsOnNothing()
      }
  }
}
