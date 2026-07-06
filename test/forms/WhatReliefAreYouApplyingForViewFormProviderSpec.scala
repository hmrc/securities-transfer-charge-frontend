package forms

import uk.gov.hmrc.securitiestransferchargefrontend.forms.behaviours.StringFieldBehaviours
import play.api.data.FormError

class WhatReliefAreYouApplyingForViewFormProviderSpec extends StringFieldBehaviours {

  val requiredKey = "whatReliefAreYouApplyingForView.error.required"
  val lengthKey = "whatReliefAreYouApplyingForView.error.length"
  val maxLength = 100

  val form = new WhatReliefAreYouApplyingForViewFormProvider()()

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
