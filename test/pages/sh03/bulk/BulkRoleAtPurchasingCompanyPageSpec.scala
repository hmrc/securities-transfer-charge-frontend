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

package pages.sh03.bulk

import base.SpecBase
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import uk.gov.hmrc.securitiestransferchargefrontend.models.sh03.shared.RoleAtPurchasingCompany
import uk.gov.hmrc.securitiestransferchargefrontend.pages.sh03.bulk.BulkRoleAtPurchasingCompanyPage

class BulkRoleAtPurchasingCompanyPageSpec extends AnyFreeSpec with Matchers with SpecBase {

  "BulkRoleAtPurchasingCompanyPage" - {

    "save its answer under its own key" in {
      BulkRoleAtPurchasingCompanyPage.toString mustBe "bulkRoleAtPurchasingCompany"
    }

    "remove the UKS organ when the role is changed from UK Societas" in {
      val answers = emptyUserAnswers
        .set(BulkRoleAtPurchasingCompanyPage, RoleAtPurchasingCompany("ukSocietas", Some("Board"))).success.value
        .set(BulkRoleAtPurchasingCompanyPage, RoleAtPurchasingCompany("director", Some("Board"))).success.value

      answers.get(BulkRoleAtPurchasingCompanyPage).value mustBe RoleAtPurchasingCompany("director", None)
    }

    "keep the UKS organ when the role is UK Societas" in {
      val answers = emptyUserAnswers
        .set(BulkRoleAtPurchasingCompanyPage, RoleAtPurchasingCompany("ukSocietas", Some("Board"))).success.value

      answers.get(BulkRoleAtPurchasingCompanyPage).value mustBe RoleAtPurchasingCompany("ukSocietas", Some("Board"))
    }
  }
}
