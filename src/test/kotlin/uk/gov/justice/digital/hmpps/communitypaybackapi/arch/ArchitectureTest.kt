package uk.gov.justice.digital.hmpps.communitypaybackapi.arch

import com.lemonappdev.konsist.api.Konsist
import com.lemonappdev.konsist.api.architecture.KoArchitectureCreator.assertArchitecture
import com.lemonappdev.konsist.api.architecture.Layer
import org.junit.jupiter.api.Test

class ArchitectureTest {
  @Test
  fun `Ensure packages are correctly isolated`() {
    Konsist.scopeFromProduction()
      .assertArchitecture {
        val client = Layer("client", "..communitypaybackapi....client..")
        val service = Layer("service", "..communitypaybackapi..service..")
        val controller = Layer("controller", "..communitypaybackapi..controller..")

        client.doesNotDependOn(controller, service)
        service.doesNotDependOn(controller)
        controller.doesNotDependOn(client)
      }
  }
}
