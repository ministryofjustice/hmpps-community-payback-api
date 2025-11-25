package uk.gov.justice.digital.hmpps.communitypaybackapi.integration.container

object TestContainersUtil {

  /**
   * The latest version of Docker Desktop (Mac) requires clients to use the docker API
   * with at least version 1.44, which causes issues in test containers v1.x.
   *
   * Upgrading to the latest test containers 2.x should resolve this, but in the meantime
   * we can use this workaround to force the API version
   *
   * For more information see https://github.com/testcontainers/testcontainers-java/issues/11212
   */
  fun setDockerApiVersion() {
    System.setProperty("api.version", "1.44")
  }
}
