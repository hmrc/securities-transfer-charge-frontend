package uk.gov.hmrc.securitiestransferchargefrontend.forms

import javax.inject.Inject

import uk.gov.hmrc.securitiestransferchargefrontend.forms.mappings.Mappings
import play.api.data.Form

class InTheUkOrNtFormProvider @Inject() extends Mappings {

  def apply(): Form[Boolean] =
    Form(
      "value" -> boolean("inTheUkOrNt.error.required")
    )
}
