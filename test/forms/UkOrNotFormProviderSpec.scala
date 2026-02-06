package forms

import uk.gov.hmrc.securitiestransferchargefrontend.forms.behaviours.OptionFieldBehaviours
import uk.gov.hmrc.securitiestransferchargefrontend.models.UkOrNot
import play.api.data.FormError

class UkOrNotFormProviderSpec extends OptionFieldBehaviours {

  val form = new UkOrNotFormProvider()()

  ".value" - {

    val fieldName = "value"
    val requiredKey = "ukOrNot.error.required"

    behave like optionsField[UkOrNot](
      form,
      fieldName,
      validValues  = UkOrNot.values,
      invalidError = FormError(fieldName, "error.invalid")
    )

    behave like mandatoryField(
      form,
      fieldName,
      requiredError = FormError(fieldName, requiredKey)
    )
  }
}
