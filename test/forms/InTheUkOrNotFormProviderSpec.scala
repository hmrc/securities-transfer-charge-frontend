package forms

import uk.gov.hmrc.securitiestransferchargefrontend.forms.behaviours.BooleanFieldBehaviours
import play.api.data.FormError

class InTheUkOrNotFormProviderSpec extends BooleanFieldBehaviours {

  val requiredKey = "inTheUkOrNot.error.required"
  val invalidKey = "error.boolean"

  val form = new InTheUkOrNotFormProvider()()

  ".value" - {

    val fieldName = "value"

    behave like booleanField(
      form,
      fieldName,
      invalidError = FormError(fieldName, invalidKey)
    )

    behave like mandatoryField(
      form,
      fieldName,
      requiredError = FormError(fieldName, requiredKey)
    )
  }
}
