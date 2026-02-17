package uk.gov.hmrc.securitiestransferchargefrontend.forms

import javax.inject.Inject

import uk.gov.hmrc.securitiestransferchargefrontend.forms.mappings.Mappings
import play.api.data.Form
import play.api.data.Forms._
import uk.gov.hmrc.securitiestransferchargefrontend.models.SecuritiesTarget

class SecuritiesTargetFormProvider @Inject() extends Mappings {

   def apply(): Form[SecuritiesTarget] = Form(
     mapping(
      "BusinessName" -> text("securitiesTarget.error.BusinessName.required")
        .verifying(maxLength(1000, "securitiesTarget.error.BusinessName.length")),
      "CRN" -> text("securitiesTarget.error.CRN.required")
        .verifying(maxLength(8, "securitiesTarget.error.CRN.length"))
    )(SecuritiesTarget.apply)(x => Some((x.BusinessName, x.CRN)))
   )
 }
