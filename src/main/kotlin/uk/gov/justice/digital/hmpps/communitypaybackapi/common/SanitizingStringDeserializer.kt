package uk.gov.justice.digital.hmpps.communitypaybackapi.common

import org.owasp.html.Encoding
import org.owasp.html.HtmlChangeListener
import org.owasp.html.HtmlPolicyBuilder
import org.owasp.html.PolicyFactory
import tools.jackson.core.JsonParser
import tools.jackson.databind.DatabindException
import tools.jackson.databind.DeserializationContext
import tools.jackson.databind.deser.std.StdScalarDeserializer

/**
 * Use the OWASP HTML sanitizer to strip out potentially harmful HTML
 */
class SanitizingStringDeserializer : StdScalarDeserializer<String>(String::class.java) {
  companion object {
    // Disallows everything
    val POLICY: PolicyFactory = HtmlPolicyBuilder().toFactory()
  }

  override fun deserialize(
    p: JsonParser,
    ctxt: DeserializationContext,
  ): String? = Encoding.decodeHtml(
    POLICY.sanitize(p.valueAsString, ThrowingHtmlChangeListener(), p),
    false,
  )
}

class ThrowingHtmlChangeListener : HtmlChangeListener<JsonParser> {
  override fun discardedTag(ctx: JsonParser?, elementName: String): Unit = throw DatabindException.from(ctx, "Tag '<$elementName>' is not allowed")

  override fun discardedAttributes(ctx: JsonParser?, elementName: String, vararg attributeNames: String?): Unit = throw DatabindException.from(
    ctx,
    "Attributes [${attributeNames.joinToString(", ") { "'$it'" }}] are not allowed on tag '<$elementName>'",
  )
}
