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

package uk.gov.hmrc.securitiestransferchargefrontend.navigation.sh03

import play.api.mvc.Call
import uk.gov.hmrc.securitiestransferchargefrontend.controllers.sh03.agents.bulk.routes as sh03AgentBulkRoutes
import uk.gov.hmrc.securitiestransferchargefrontend.controllers.sh03.organisations.bulk.routes as sh03OrgBulkRoutes
import uk.gov.hmrc.securitiestransferchargefrontend.models.{CheckMode, UserAnswers}
import uk.gov.hmrc.securitiestransferchargefrontend.pages.sh03.bulk.{BulkCompanyDetailsPage, BulkRoleAtPurchasingCompanyPage}


final class Sh03BulkAnswersValidator(companyDetailsPage: => Call, templateInstructionPage: => Call, roleAtPurchasingCompanyPage: => Call) {

  def missingAnswer(userAnswers: UserAnswers): Option[Call] =
    if (userAnswers.get(BulkCompanyDetailsPage).isEmpty) Some(companyDetailsPage)
    else if (userAnswers.fileUploadReference.isEmpty) Some(templateInstructionPage)
    else if (userAnswers.get(BulkRoleAtPurchasingCompanyPage).isEmpty) Some(roleAtPurchasingCompanyPage)
    else None
}

object Sh03BulkAnswersValidator {

  val organisations: Sh03BulkAnswersValidator = new Sh03BulkAnswersValidator(
    companyDetailsPage          = sh03OrgBulkRoutes.CompanyDetailsController.onPageLoad(CheckMode),
    templateInstructionPage              = sh03OrgBulkRoutes.TemplateInstructionsController.onPageLoad(),
    roleAtPurchasingCompanyPage = sh03OrgBulkRoutes.RoleAtPurchasingCompanyController.onPageLoad(CheckMode)
  )

  val agents: Sh03BulkAnswersValidator = new Sh03BulkAnswersValidator(
    companyDetailsPage          = sh03AgentBulkRoutes.CompanyDetailsController.onPageLoad(CheckMode),
    templateInstructionPage              = sh03AgentBulkRoutes.TemplateInstructionsController.onPageLoad(),
    roleAtPurchasingCompanyPage = sh03AgentBulkRoutes.RoleAtPurchasingCompanyController.onPageLoad(CheckMode)
  )
}
