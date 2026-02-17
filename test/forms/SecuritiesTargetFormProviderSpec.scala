package forms

import uk.gov.hmrc.securitiestransferchargefrontend.forms.behaviours.StringFieldBehaviours
import play.api.data.FormError

class SecuritiesTargetFormProviderSpec extends StringFieldBehaviours {

  val form = new SecuritiesTargetFormProvider()()

  ".BusinessName" - {

    val fieldName = "BusinessName"
    val requiredKey = "securitiesTarget.error.BusinessName.required"
    val lengthKey = "securitiesTarget.error.BusinessName.length"
    val maxLength = 1000

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

  ".CRN" - {

    val fieldName = "CRN"
    val requiredKey = "securitiesTarget.error.CRN.required"
    val lengthKey = "securitiesTarget.error.CRN.length"
    val maxLength = 8

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
