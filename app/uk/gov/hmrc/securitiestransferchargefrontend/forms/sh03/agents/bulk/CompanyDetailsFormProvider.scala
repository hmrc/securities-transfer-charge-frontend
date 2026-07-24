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

package uk.gov.hmrc.securitiestransferchargefrontend.forms.sh03.agents.bulk

import play.api.data.Form
import play.api.data.Forms.*
import uk.gov.hmrc.securitiestransferchargefrontend.forms.mappings.Mappings
import uk.gov.hmrc.securitiestransferchargefrontend.models.sh03.bulk.CompanyDetails

import javax.inject.Inject

class CompanyDetailsFormProvider @Inject() extends Mappings {

  def apply(): Form[CompanyDetails] = Form(
    mapping(
      "companyName" -> text("agent.sh03.bulk.companyDetails.companyName.error.required")
        .verifying(maxLength(160, "agent.sh03.bulk.companyDetails.companyName.error.length")),
      
      "companyRegistrationNumber" -> text("agent.sh03.bulk.companyDetails.crn.error.required")
        .verifying(regexp("""^[a-zA-Z0-9]+$""", "agent.sh03.bulk.companyDetails.crn.error.invalid"))
        .verifying(regexp("""^.{8}$""", "agent.sh03.bulk.companyDetails.crn.error.length"))
      
    )(CompanyDetails.apply)(x => Some((x.companyName, x.companyRegistrationNumber))))
}
