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

package navigation.sh03

import base.SpecBase
import uk.gov.hmrc.securitiestransferchargefrontend.controllers.sh03.agents.bulk.routes as sh03AgentBulkRoutes
import uk.gov.hmrc.securitiestransferchargefrontend.controllers.sh03.organisations.bulk.routes as sh03OrgBulkRoutes
import uk.gov.hmrc.securitiestransferchargefrontend.models.sh03.bulk.CompanyDetails
import uk.gov.hmrc.securitiestransferchargefrontend.models.sh03.shared.RoleAtPurchasingCompany
import uk.gov.hmrc.securitiestransferchargefrontend.models.{CheckMode, UserAnswers}
import uk.gov.hmrc.securitiestransferchargefrontend.navigation.sh03.Sh03BulkAnswersValidator
import uk.gov.hmrc.securitiestransferchargefrontend.pages.sh03.bulk.{BulkCompanyDetailsPage, BulkRoleAtPurchasingCompanyPage}

class Sh03BulkAnswersValidatorSpec extends SpecBase {

  private val completeAnswers: UserAnswers =
    emptyUserAnswers
      .set(BulkCompanyDetailsPage, CompanyDetails("Test Ltd", "12345678")).success.value
      .set(BulkRoleAtPurchasingCompanyPage, RoleAtPurchasingCompany("director", None)).success.value
      .setFileUploadReference("file-ref")

  Seq(
    "organisations" -> (
      Sh03BulkAnswersValidator.organisations,
      sh03OrgBulkRoutes.CompanyDetailsController.onPageLoad(CheckMode),
      sh03OrgBulkRoutes.TemplateInstructionsController.onPageLoad(),
      sh03OrgBulkRoutes.RoleAtPurchasingCompanyController.onPageLoad(CheckMode)
    ),
    "agents" -> (
      Sh03BulkAnswersValidator.agents,
      sh03AgentBulkRoutes.CompanyDetailsController.onPageLoad(CheckMode),
      sh03AgentBulkRoutes.TemplateInstructionsController.onPageLoad(),
      sh03AgentBulkRoutes.RoleAtPurchasingCompanyController.onPageLoad(CheckMode)
    )
  ).foreach { case (userType, (validator, companyDetailsPage, fileUploadPage, rolePage)) =>

    s"Sh03BulkAnswersValidator for $userType" - {

      "must return None when everything has been answered" in {
        validator.missingAnswer(completeAnswers) mustBe None
      }

      "must return the company details page when company details are missing" in {
        val answers = completeAnswers.remove(BulkCompanyDetailsPage).success.value
        validator.missingAnswer(answers).value mustBe companyDetailsPage
      }

      "must return the template instructions page when the file reference is missing" in {
        val answers = completeAnswers.copy(fileUploadReference = None)
        validator.missingAnswer(answers).value mustBe fileUploadPage
      }

      "must return the role at purchasing company page when the role is missing" in {
        val answers = completeAnswers.remove(BulkRoleAtPurchasingCompanyPage).success.value
        validator.missingAnswer(answers).value mustBe rolePage
      }

      "must return the first missing page in journey order when several are missing" in {
        validator.missingAnswer(emptyUserAnswers).value mustBe companyDetailsPage

        val noFileOrRole = completeAnswers.copy(fileUploadReference = None).remove(BulkRoleAtPurchasingCompanyPage).success.value
        validator.missingAnswer(noFileOrRole).value mustBe fileUploadPage
      }
    }
  }
}
