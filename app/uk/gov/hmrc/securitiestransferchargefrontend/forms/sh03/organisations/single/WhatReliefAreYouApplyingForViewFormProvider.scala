package uk.gov.hmrc.securitiestransferchargefrontend.forms.sh03.organisations.single

import play.api.data.Form
import uk.gov.hmrc.securitiestransferchargefrontend.forms.mappings.Mappings

import javax.inject.Inject

class WhatReliefAreYouApplyingForViewFormProvider @Inject() extends Mappings {

  def apply(): Form[String] =
    Form(
      "value" -> text("whatReliefAreYouApplyingForView.error.required")
        .verifying(maxLength(100, "whatReliefAreYouApplyingForView.error.length"))
    )
}
