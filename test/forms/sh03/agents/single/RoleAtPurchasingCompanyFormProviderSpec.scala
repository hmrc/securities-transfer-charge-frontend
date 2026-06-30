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

package forms.sh03.agents.single

import base.SpecBase
import play.api.data.{Form, FormError}
import uk.gov.hmrc.securitiestransferchargefrontend.forms.sh03.agents.single.RoleAtPurchasingCompanyFormProvider
import uk.gov.hmrc.securitiestransferchargefrontend.models.sh03.agents.RoleAtPurchasingCompany

class RoleAtPurchasingCompanyFormProviderSpec extends SpecBase {

  val formProvider = new RoleAtPurchasingCompanyFormProvider()
  val form: Form[RoleAtPurchasingCompany] = formProvider()

  "RoleAtPurchasingCompanyFormProvider" - {

    "must bind valid data for a standard role" in {
      val data = Map("role" -> "director")
      val result = form.bind(data)
      result.value.value mustBe RoleAtPurchasingCompany("director", None)
    }

    "must bind valid data for UK Societas with an organ name" in {
      val data = Map("role" -> "ukSocietas", "uksOrgan" -> "Management Board")
      val result = form.bind(data)
      result.value.value mustBe RoleAtPurchasingCompany("ukSocietas", Some("Management Board"))
    }

    "must fail to bind when no role is selected" in {
      val data = Map.empty[String, String]
      val result = form.bind(data)
      result.errors must contain(FormError("role", "agent.sh03.roleAtPurchasingCompany.error.required"))
    }

    "must fail to bind when UK Societas is selected but the organ name is left blank" in {
      val data = Map("role" -> "ukSocietas", "uksOrgan" -> "")
      val result = form.bind(data)
      
      result.errors must contain(FormError("uksOrgan", "agent.sh03.roleAtPurchasingCompany.uksOrgan.error.required"))
    }

    "must fail to bind when UK Societas organ name exceeds 100 characters" in {
      val data = Map("role" -> "ukSocietas", "uksOrgan" -> ("A" * 101))
      val result = form.bind(data)
      
      result.errors must contain(FormError("uksOrgan", "agent.sh03.roleAtPurchasingCompany.uksOrgan.error.length"))
    }
  }
}