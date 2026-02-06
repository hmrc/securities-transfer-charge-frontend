package uk.gov.hmrc.securitiestransferchargefrontend.forms

import javax.inject.Inject

import uk.gov.hmrc.securitiestransferchargefrontend.mappings.Mappings
import play.api.data.Form

class NameOfSellerFormProvider @Inject() extends Mappings {

  def apply(): Form[String] =
    Form(
      "value" -> text("nameOfSeller.error.required")
        .verifying(maxLength(35, "nameOfSeller.error.length"))
    )
}
