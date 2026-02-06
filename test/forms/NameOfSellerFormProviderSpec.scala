package forms

import uk.gov.hmrc.securitiestransferchargefrontend.forms.behaviours.StringFieldBehaviours
import play.api.data.FormError

class NameOfSellerFormProviderSpec extends StringFieldBehaviours {

  val requiredKey = "nameOfSeller.error.required"
  val lengthKey = "nameOfSeller.error.length"
  val maxLength = 35

  val form = new NameOfSellerFormProvider()()

  ".value" - {

    val fieldName = "value"

    behave like fieldThatBindsValidData(
      form,
      fieldName,
      stringsWithMaxLength(maxLength)
    )

    behave like fieldWithMaxLength(
      form,
      fieldName,
      maxLength = maxLength,
      lengthError = FormError(fieldName, lengthKey, Seq(maxLength))
    )

    behave like mandatoryField(
      form,
      fieldName,
      requiredError = FormError(fieldName, requiredKey)
    )
  }
}
