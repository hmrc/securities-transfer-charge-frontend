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

package uk.gov.hmrc.securitiestransferchargefrontend.forms

import javax.inject.Inject

import uk.gov.hmrc.securitiestransferchargefrontend.forms.mappings.Mappings
import play.api.data.Form
import play.api.data.Forms._
import uk.gov.hmrc.securitiestransferchargefrontend.models.SecuritiesTarget

class SecuritiesTargetFormProvider @Inject() extends Mappings {

   def apply(): Form[SecuritiesTarget] = Form(
     mapping(
      "BusinessName" -> text("securitiesTarget.error.BusinessName.required"),
       "CRN" -> validatedOptionalText("securitiesTarget.error.CRN.length", 8)
     )(SecuritiesTarget.apply)(x => Some((x.BusinessName, x.CRN)))
   )
 }
