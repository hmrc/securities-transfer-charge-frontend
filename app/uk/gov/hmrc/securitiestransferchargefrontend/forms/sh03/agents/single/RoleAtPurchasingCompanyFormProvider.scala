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

package uk.gov.hmrc.securitiestransferchargefrontend.forms.sh03.agents.single

import javax.inject.Inject
import play.api.data.Form
import play.api.data.Forms.*
import uk.gov.hmrc.securitiestransferchargefrontend.models.sh03.agents.RoleAtPurchasingCompany

class RoleAtPurchasingCompanyFormProvider @Inject() {

  def apply(): Form[RoleAtPurchasingCompany] = Form(
    mapping(
      "role" -> default(text, "").verifying("agent.sh03.roleAtPurchasingCompany.error.required", _.trim.nonEmpty),
      "uksOrgan" -> optional(text)
    )(RoleAtPurchasingCompany.apply)(o => Some((o.role, o.uksOrgan)))
      .verifying("agent.sh03.roleAtPurchasingCompany.uksOrgan.error.required", data =>
        if (data.role == "ukSocietas") data.uksOrgan.exists(_.trim.nonEmpty) else true
      )
      .verifying("agent.sh03.roleAtPurchasingCompany.uksOrgan.error.length", data =>
        if (data.role == "ukSocietas") data.uksOrgan.forall(_.length <= 100) else true
      )
  )
}