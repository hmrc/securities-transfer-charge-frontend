/*
 * Copyright 2026 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package uk.gov.hmrc.securitiestransferchargefrontend.forms.stf.shared

import play.api.data.Form
import play.api.data.Forms.*
import uk.gov.hmrc.securitiestransferchargefrontend.forms.mappings.Mappings
import uk.gov.hmrc.securitiestransferchargefrontend.models.stf.SecuritiesTarget

import javax.inject.Inject

class SecuritiesTargetFormProvider @Inject() extends Mappings {

   def apply(affinityKey:String): Form[SecuritiesTarget] = Form(
     mapping(
      "businessName" -> text(s"${affinityKey}.securitiesTarget.error.businessName.required")
        .verifying(maxLength(160, s"${affinityKey}.securitiesTarget.error.businessName.length")),
       "crn" -> validatedOptionalText(s"${affinityKey}.securitiesTarget.error.crn.length", 8)
         .verifying(s"${affinityKey}.securitiesTarget.error.crn.invalid", crn => crn.forall(s => s.matches("^[a-zA-Z0-9]{8}$")))
     )(SecuritiesTarget.apply)(x => Some((x.businessName, x.crn)))
   )
 }
