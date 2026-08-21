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

package uk.gov.hmrc.securitiestransferchargefrontend.navigation.sh03.agents

import play.api.mvc.Call
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.securitiestransferchargefrontend.config.FrontendAppConfig
import uk.gov.hmrc.securitiestransferchargefrontend.controllers.sh03.agents.bulk.routes as sh03AgentBulkRoutes
import uk.gov.hmrc.securitiestransferchargefrontend.controllers.sh03.agents.single.routes as sh03AgentSingleRoutes
import uk.gov.hmrc.securitiestransferchargefrontend.controllers.sh03.shared.bulk.routes as sh03BulkCyaRoutes
import uk.gov.hmrc.securitiestransferchargefrontend.models.sh03.HowToNotifyAboutShareBuyback.{MoreThanOneAtATime, OneAtATime}
import uk.gov.hmrc.securitiestransferchargefrontend.models.sh03.shared.{ReasonForPurchase, RoleAtPurchasingCompany}
import uk.gov.hmrc.securitiestransferchargefrontend.models.{CheckMode, Mode, NormalMode, UserAnswers}
import uk.gov.hmrc.securitiestransferchargefrontend.navigation.PersistentNavigationHelper
import uk.gov.hmrc.securitiestransferchargefrontend.pages.Page
import uk.gov.hmrc.securitiestransferchargefrontend.pages.sh03.*
import uk.gov.hmrc.securitiestransferchargefrontend.pages.sh03.bulk.*
import uk.gov.hmrc.securitiestransferchargefrontend.services.AnswerPersistenceService

import scala.concurrent.{ExecutionContext, Future}

class ForwardRoutes(answerPersistenceService: AnswerPersistenceService,
                    defaultPage: Call,
                    errorPages: Seq[Call],
                    appConfig: FrontendAppConfig)
                   (implicit ec: ExecutionContext):

  val helper = new PersistentNavigationHelper(answerPersistenceService, defaultPage, errorPages)

  import helper.*

  private val firstDate = appConfig.firstChargingPoint
  private lazy val cyaPage = sh03AgentSingleRoutes.CheckYourAnswersController.onPageLoad()
  
  def forwardRoutes(page: Page, mode: Mode)(implicit hc: HeaderCarrier): UserAnswers => Future[Call] = mode match {
    case NormalMode => normalRoutes(page)
    case CheckMode => checkRoutes(page)
  }
  
  private def normalRoutes(page: Page)(implicit hc: HeaderCarrier): UserAnswers => Future[Call] = page match {

    case HowToNotifyAboutShareBuybackPage => userAnswers => {
      dataDependent(HowToNotifyAboutShareBuybackPage, userAnswers) {
        case OneAtATime => sh03AgentSingleRoutes.AgentReferenceController.onPageLoad(NormalMode)
        case MoreThanOneAtATime => sh03AgentBulkRoutes.AgentReferenceController.onPageLoad(NormalMode)
      }
    }
    case AgentReferencePage => userAnswers =>
      dataRequired(AgentReferencePage, userAnswers, sh03AgentSingleRoutes.CompanyDetailsController.onPageLoad(NormalMode))

    case CompanyDetailsPage => userAnswers =>
      dataRequired(CompanyDetailsPage, userAnswers, sh03AgentSingleRoutes.ReasonForPurchaseController.onPageLoad(NormalMode))

    case ReasonForPurchasePage => userAnswers =>
      dataDependent(ReasonForPurchasePage, userAnswers) {
        case ReasonForPurchase.ForCancellation => sh03AgentSingleRoutes.TreasurySharesController.onPageLoad(NormalMode)
        case ReasonForPurchase.ToPlaceIntoTreasury => sh03AgentSingleRoutes.ConnectedPersonsController.onPageLoad(NormalMode)
      }
    case TreasurySharesPage => userAnswers =>
      dataRequired(TreasurySharesPage, userAnswers, sh03AgentSingleRoutes.ConnectedPersonsController.onPageLoad(NormalMode))

    case ConnectedPersonsPage => userAnswers =>
      dataRequired(ConnectedPersonsPage, userAnswers, sh03AgentSingleRoutes.ApplyingForReliefController.onPageLoad(NormalMode))

    case ApplyingForReliefPage => userAnswers =>
      dataDependent(ApplyingForReliefPage, userAnswers) {
        case false => sh03AgentSingleRoutes.DetailsOfThisSharePurchaseController.onPageLoad(NormalMode)
        case true => sh03AgentSingleRoutes.WhatReliefAreYouApplyingForController.onPageLoad(NormalMode)
      }
    case WhatReliefAreYouApplyingForPage => userAnswers =>
      dataRequired(WhatReliefAreYouApplyingForPage, userAnswers, sh03AgentSingleRoutes.DetailsOfThisSharePurchaseController.onPageLoad(NormalMode))

    case DetailsOfThisSharePurchasePage => userAnswers =>
      dataDependent(CompanyDetailsPage, userAnswers) {companyDetails =>
        if (companyDetails.isPlc)
          sh03AgentSingleRoutes.MaximumAmountPaidController.onPageLoad(NormalMode)
        else
          sh03AgentSingleRoutes.ChargingPointController.onPageLoad(NormalMode)
      }

    case MaximumAmountPaidPage => userAnswers =>
      dataRequired(MaximumAmountPaidPage, userAnswers, sh03AgentSingleRoutes.MinimumAmountPaidController.onPageLoad(NormalMode))

    case MinimumAmountPaidPage => userAnswers =>
      dataRequired(MinimumAmountPaidPage, userAnswers, sh03AgentSingleRoutes.ChargingPointController.onPageLoad(NormalMode))

    case ChargingPointPage => userAnswers =>
      dataDependent(ChargingPointPage, userAnswers) { enterDate =>
        if (enterDate.isBefore(firstDate)) defaultPage
        else sh03AgentSingleRoutes.RoleAtPurchasingCompanyController.onPageLoad(NormalMode)
      }

    case RoleAtPurchasingCompanyPage => userAnswers =>
      dataDependent(RoleAtPurchasingCompanyPage, userAnswers) {
        roleAtPurchasingCompany =>
          if (roleAtPurchasingCompany.role == RoleAtPurchasingCompany.unsupportedRole)
            sh03AgentSingleRoutes.CannotSubmitFormErrorController.onPageLoad()
          else
            cyaPage
      }

    case BulkAgentReferencePage => userAnswers =>
      dataRequired(BulkAgentReferencePage, userAnswers, sh03AgentBulkRoutes.CompanyDetailsController.onPageLoad(NormalMode))

    case BulkCompanyDetailsPage => userAnswers =>
      dataRequired(BulkCompanyDetailsPage, userAnswers, sh03AgentBulkRoutes.TemplateInstructionsController.onPageLoad())

    case BulkRoleAtPurchasingCompanyPage => userAnswers =>
      dataDependent(BulkRoleAtPurchasingCompanyPage, userAnswers) {
        roleAtPurchasingCompany =>
          if (roleAtPurchasingCompany.role == RoleAtPurchasingCompany.unsupportedRole)
            sh03AgentBulkRoutes.CannotSubmitFormErrorController.onPageLoad()
          else sh03BulkCyaRoutes.CheckYourAnswersController.onPageLoad()
      }

    case _ => _ => Future.successful(defaultPage)
  }

  def checkRoutes(page: Page)(implicit hc: HeaderCarrier): UserAnswers => Future[Call] = page match {

    case ReasonForPurchasePage => userAnswers =>
      if (userAnswers.get(ReasonForPurchasePage).contains(ReasonForPurchase.ForCancellation) && userAnswers.get(TreasurySharesPage).isEmpty) {
        goTo(sh03AgentSingleRoutes.TreasurySharesController.onPageLoad(CheckMode), Some(userAnswers))
      } else {
        goTo(cyaPage, Some(userAnswers))
      }

    case ApplyingForReliefPage => userAnswers =>
      if (userAnswers.get(ApplyingForReliefPage).contains(true) && userAnswers.get(WhatReliefAreYouApplyingForPage).isEmpty) {
        goTo(sh03AgentSingleRoutes.WhatReliefAreYouApplyingForController.onPageLoad(CheckMode), Some(userAnswers))
      } else {
        goTo(cyaPage, Some(userAnswers))
      }

    case CompanyDetailsPage => userAnswers =>
      userAnswers.get(CompanyDetailsPage) match {
        case Some(details) if details.isPlc && userAnswers.get(MaximumAmountPaidPage).isEmpty =>
          goTo(sh03AgentSingleRoutes.MaximumAmountPaidController.onPageLoad(CheckMode), Some(userAnswers))
        case _ => goTo(cyaPage, Some(userAnswers))
      }

    case MaximumAmountPaidPage => userAnswers =>
      if (userAnswers.get(MinimumAmountPaidPage).isEmpty) {
        goTo(sh03AgentSingleRoutes.MinimumAmountPaidController.onPageLoad(CheckMode), Some(userAnswers))
      } else {
        goTo(cyaPage, Some(userAnswers))
      }

    case ConnectedPersonsPage => userAnswers =>
      if (userAnswers.get(ConnectedPersonsPage).contains(true)) {
        goTo(sh03AgentSingleRoutes.DetailsOfThisSharePurchaseController.onPageLoad(CheckMode), Some(userAnswers))
      } else {
        goTo(cyaPage, Some(userAnswers))
      }

    case _ => userAnswers => goTo(cyaPage, Some(userAnswers))
  }
