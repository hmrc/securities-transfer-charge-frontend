package uk.gov.hmrc.securitiestransferchargefrontend.forms

import javax.inject.Inject

import uk.gov.hmrc.securitiestransferchargefrontend.forms.mappings.Mappings
import play.api.data.Form

class $className$FormProvider @Inject() extends Mappings {

  def apply(): Form[Boolean] =
    Form(
      "value" -> boolean("$className;format="decap"$.error.required")
    )
}
