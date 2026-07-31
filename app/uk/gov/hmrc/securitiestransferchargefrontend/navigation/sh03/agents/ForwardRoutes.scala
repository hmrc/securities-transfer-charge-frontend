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
import uk.gov.hmrc.securitiestransferchargefrontend.controllers.sh03.shared.single.routes as sh03SingleCyaRoutes
import uk.gov.hmrc.securitiestransferchargefrontend.controllers.sh03.shared.bulk.routes as sh03BulkCyaRoutes
import uk.gov.hmrc.securitiestransferchargefrontend.models.sh03.HowToNotifyAboutShareBuyback.{MoreThanOneAtATime, OneAtATime}
import uk.gov.hmrc.securitiestransferchargefrontend.models.sh03.shared.{ReasonForPurchase, RoleAtPurchasingCompany}
import uk.gov.hmrc.securitiestransferchargefrontend.models.{NormalMode, UserAnswers}
import uk.gov.hmrc.securitiestransferchargefrontend.navigation.PersistentNavigationHelper
import uk.gov.hmrc.securitiestransferchargefrontend.pages.{CheckYourAnswersPage, JourneyRecoveryPage, Page}
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

  def forwardRoutesPage(page: Page, userAnswers: UserAnswers): Page = page match {
    case BeforeYouStartPage => HowToNotifyAboutShareBuybackPage
    case HowToNotifyAboutShareBuybackPage =>
      userAnswers.get(HowToNotifyAboutShareBuybackPage) match {
        case Some(OneAtATime) => AgentReferencePage
        case Some(MoreThanOneAtATime) => BulkAgentReferencePage
        case _ => JourneyRecoveryPage
      }
    case AgentReferencePage =>
      if (userAnswers.get(AgentReferencePage).isDefined) CompanyDetailsPage else JourneyRecoveryPage
    case CompanyDetailsPage =>
      if (userAnswers.get(CompanyDetailsPage).isDefined) ReasonForPurchasePage else JourneyRecoveryPage
    case ReasonForPurchasePage =>
      userAnswers.get(ReasonForPurchasePage) match {
        case Some(ReasonForPurchase.ForCancellation) => TreasurySharesPage
        case Some(ReasonForPurchase.ToPlaceIntoTreasury) => ConnectedPersonsPage
        case _ => JourneyRecoveryPage
      }
    case TreasurySharesPage =>
      if (userAnswers.get(TreasurySharesPage).isDefined) ConnectedPersonsPage else JourneyRecoveryPage
    case ConnectedPersonsPage =>
      if (userAnswers.get(ConnectedPersonsPage).isDefined) ApplyingForReliefPage else JourneyRecoveryPage
    case ApplyingForReliefPage =>
      userAnswers.get(ApplyingForReliefPage) match {
        case Some(false) => DetailsOfThisSharePurchasePage
        case Some(true) => WhatReliefAreYouApplyingForPage
        case _ => JourneyRecoveryPage
      }
    case WhatReliefAreYouApplyingForPage =>
      if (userAnswers.get(WhatReliefAreYouApplyingForPage).isDefined) DetailsOfThisSharePurchasePage else JourneyRecoveryPage
    case DetailsOfThisSharePurchasePage =>
      userAnswers.get(CompanyDetailsPage) match {
        case Some(companyDetails) if companyDetails.isPlc => MaximumAmountPaidPage
        case Some(_) => ChargingPointPage
        case _ => JourneyRecoveryPage
      }
    case MaximumAmountPaidPage =>
      if (userAnswers.get(MaximumAmountPaidPage).isDefined) MinimumAmountPaidPage else JourneyRecoveryPage
    case MinimumAmountPaidPage =>
      if (userAnswers.get(MinimumAmountPaidPage).isDefined) ChargingPointPage else JourneyRecoveryPage
    case ChargingPointPage =>
      userAnswers.get(ChargingPointPage) match {
        case Some(enterDate) if enterDate.isBefore(firstDate) => JourneyRecoveryPage
        case Some(_) => RoleAtPurchasingCompanyPage
        case _ => JourneyRecoveryPage
      }
    case RoleAtPurchasingCompanyPage =>
      userAnswers.get(RoleAtPurchasingCompanyPage) match {
        case Some(roleAtPurchasingCompany) if roleAtPurchasingCompany.role == RoleAtPurchasingCompany.unsupportedRole =>
          CannotSubmitFormErrorPage
        case Some(_) => CheckYourAnswersPage
        case _ => JourneyRecoveryPage
      }
    case BulkAgentReferencePage =>
      if (userAnswers.get(BulkAgentReferencePage).isDefined) BulkCompanyDetailsPage else JourneyRecoveryPage
    case BulkCompanyDetailsPage =>
      if (userAnswers.get(BulkCompanyDetailsPage).isDefined) JourneyRecoveryPage else JourneyRecoveryPage
    case BulkRoleAtPurchasingCompanyPage =>
      userAnswers.get(BulkRoleAtPurchasingCompanyPage) match {
        case Some(roleAtPurchasingCompany) if roleAtPurchasingCompany.role == RoleAtPurchasingCompany.unsupportedRole =>
          CannotSubmitFormErrorPage
        case Some(_) => JourneyRecoveryPage
        case _ => JourneyRecoveryPage
      }
    case _ => JourneyRecoveryPage
  }

  def forwardRoutes(page: Page)(implicit hc: HeaderCarrier): UserAnswers => Future[Call] = page match {

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
            sh03SingleCyaRoutes.CheckYourAnswersController.onPageLoad()
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
