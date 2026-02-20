package forms

import forms.behaviours.OptionFieldBehaviours
import uk.gov.hmrc.securitiestransferchargefrontend.models.TaxRate
import play.api.data.FormError
import uk.gov.hmrc.securitiestransferchargefrontend.forms.TaxRateFormProvider

class TaxRateFormProviderSpec extends OptionFieldBehaviours {

  val form = new TaxRateFormProvider()()

  ".value" - {

    val fieldName = "value"
    val requiredKey = "taxRate.error.required"

    behave like optionsField[TaxRate](
      form,
      fieldName,
      validValues  = TaxRate.values,
      invalidError = FormError(fieldName, "error.invalid")
    )

    behave like mandatoryField(
      form,
      fieldName,
      requiredError = FormError(fieldName, requiredKey)
    )
  }
}
