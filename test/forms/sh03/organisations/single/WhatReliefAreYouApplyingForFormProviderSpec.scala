package forms.sh03.organisations.single

import play.api.data.FormError
import uk.gov.hmrc.securitiestransferchargefrontend.forms.behaviours.StringFieldBehaviours

class WhatReliefAreYouApplyingForFormProviderSpec extends StringFieldBehaviours {

  val requiredKey = "whatReliefAreYouApplyingFor.error.required"
  val lengthKey = "whatReliefAreYouApplyingFor.error.length"
  val maxLength = 100

  val form = new WhatReliefAreYouApplyingForFormProvider()()

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
