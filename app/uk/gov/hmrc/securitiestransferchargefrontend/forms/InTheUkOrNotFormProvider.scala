package uk.gov.hmrc.securitiestransferchargefrontend.forms

import javax.inject.Inject

import uk.gov.hmrc.securitiestransferchargefrontend.mappings.Mappings
import play.api.data.Form

class InTheUkOrNotFormProvider @Inject() extends Mappings {

  def apply(): Form[Boolean] =
    Form(
      "value" -> boolean("inTheUkOrNot.error.required")
    )
}
