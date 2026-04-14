package uk.gov.justice.digital.hmpps.communitypaybackapi.common

import org.owasp.html.PolicyFactory
import org.owasp.html.Sanitizers
import tools.jackson.core.JsonParser
import tools.jackson.databind.DeserializationContext
import tools.jackson.databind.deser.std.StdScalarDeserializer

/**
 * Use the OWASP HTML sanitizer to strip out potentially harmful HTML
 */
class SanitizingStringDeserializer : StdScalarDeserializer<String>(String::class.java) {
  companion object {
    val POLICY: PolicyFactory = Sanitizers.FORMATTING.and(Sanitizers.LINKS)
  }

  override fun deserialize(
    p: JsonParser,
    ctxt: DeserializationContext,
  ): String? = POLICY.sanitize(p.valueAsString)
}
