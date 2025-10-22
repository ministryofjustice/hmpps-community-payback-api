package uk.gov.justice.digital.hmpps.communitypaybackapi.config

/**
 * Baseline configuration is provided by the hmpps kotlin library, see https://github.com/ministryofjustice/hmpps-kotlin-lib/blob/main/readme-contents/SpringResourceServer.md
 */
object SecurityConfiguration {
  const val ROLE_ADMIN_UI = "ROLE_COMMUNITY_PAYBACK__COMMUNITY_PAYBACK_UI"
  const val ROLE_DOMAIN_EVENT_DETAILS = "ROLE_COMMUNITY_PAYBACK__DOMAIN_EVENT_DETAILS__ALL__RO"
  const val ROLE_SUPERVISOR_UI = "ROLE_COMMUNITY_PAYBACK__SUPERVISOR"
}
