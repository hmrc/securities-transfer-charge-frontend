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

package uk.gov.hmrc.securitiestransferchargefrontend.navigation.stf.agents

import play.api.mvc.Call
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.securitiestransferchargefrontend.config.FrontendAppConfig
import uk.gov.hmrc.securitiestransferchargefrontend.controllers.stf.agents.routes as agentRoutes
import uk.gov.hmrc.securitiestransferchargefrontend.controllers.stf.agents.single.routes as agentSingleRoutes
import uk.gov.hmrc.securitiestransferchargefrontend.controllers.stf.agents.bulk.routes as agentBulkRoutes
import uk.gov.hmrc.securitiestransferchargefrontend.controllers.stf.shared.single.routes as stfSingleCyaRoutes
import uk.gov.hmrc.securitiestransferchargefrontend.models.stf.HowToNotifyAboutSecuritiesTransfer.{MoreThanOneAtATime, OneAtATime}
import uk.gov.hmrc.securitiestransferchargefrontend.models.{NormalMode, UserAnswers}
import uk.gov.hmrc.securitiestransferchargefrontend.navigation.PersistentNavigationHelper
import uk.gov.hmrc.securitiestransferchargefrontend.pages.{CheckYourAnswersPage, JourneyRecoveryPage, Page}
import uk.gov.hmrc.securitiestransferchargefrontend.pages.stf.shared.{AgentReferencePage, HowToNotifyAboutSecuritiesTransferPage, SubmissionsDashboardPage}
import uk.gov.hmrc.securitiestransferchargefrontend.pages.stf.single.*
import uk.gov.hmrc.securitiestransferchargefrontend.services.AnswerPersistenceService
import uk.gov.hmrc.securitiestransferchargefrontend.controllers.routes
import uk.gov.hmrc.securitiestransferchargefrontend.pages.stf.bulk.TemplateInstructionsPage

import scala.concurrent.{ExecutionContext, Future}

class ForwardRoutes(answerPersistenceService: AnswerPersistenceService,
                    appConfig: FrontendAppConfig,
                    defaultPage: Call,
                    errorPages: Seq[Call])
                   (implicit ec: ExecutionContext):

  val helper = new PersistentNavigationHelper(answerPersistenceService, defaultPage, errorPages)

  import helper.*

  private val firstDate = appConfig.firstChargingPoint

  def forwardRoutesPage(page: Page, userAnswers: UserAnswers): Page = page match {
    case SubmissionsDashboardPage => HowToNotifyAboutSecuritiesTransferPage
    case HowToNotifyAboutSecuritiesTransferPage =>
      userAnswers.get(HowToNotifyAboutSecuritiesTransferPage) match {
        case Some(OneAtATime) => AgentReferencePage
        case Some(MoreThanOneAtATime) => TemplateInstructionsPage
        case _ => JourneyRecoveryPage
      }
    case AgentReferencePage =>
      userAnswers.get(HowToNotifyAboutSecuritiesTransferPage) match {
        case Some(OneAtATime) => NameOfBuyerPage
        case Some(MoreThanOneAtATime) => CheckYourAnswersPage
        case _ => JourneyRecoveryPage
      }
    case NameOfBuyerPage => StfBuyersAddressPage
    case StfBuyersAddressPage => NameOfSellerPage
    case NameOfSellerPage => StfSellerAddressPage
    case StfSellerAddressPage => ConnectedPersonsPage
    case ConnectedPersonsPage => ApplyingForReliefPage
    case ApplyingForReliefPage =>
      userAnswers.get(ApplyingForReliefPage) match {
        case Some(true) => WhatReliefAreYouApplyingForPage
        case Some(false) => SecuritiesTargetPage
        case _ => JourneyRecoveryPage
      }
    case WhatReliefAreYouApplyingForPage => SecuritiesTargetPage
    case SecuritiesTargetPage => ChargingPointPage
    case ChargingPointPage =>
      userAnswers.get(ChargingPointPage) match {
        case Some(date) if date.isBefore(firstDate) => JourneyRecoveryPage
        case Some(_) => TaxRatePage
        case _ => JourneyRecoveryPage
      }
    case TaxRatePage => PurchasingSharesPage
    case PurchasingSharesPage =>
      userAnswers.get(PurchasingSharesPage) match {
        case Some(true) => DetailsOfThisTransferPage
        case Some(false) => OtherSecuritiesTypePage
        case _ => JourneyRecoveryPage
      }
    case OtherSecuritiesTypePage => AmountPaidForSecuritiesPage
    case DetailsOfThisTransferPage => CheckYourAnswersPage
    case AmountPaidForSecuritiesPage =>
      userAnswers.get(ConnectedPersonsPage) match {
        case Some(true) => TotalMarketValuePage
        case Some(false) => CheckYourAnswersPage
        case _ => JourneyRecoveryPage
      }
    case TotalMarketValuePage => CheckYourAnswersPage
    case _ => JourneyRecoveryPage
  }

  def forwardRoutes(page: Page)(implicit hc: HeaderCarrier): UserAnswers => Future[Call] = page match {

    case SubmissionsDashboardPage => userAnswers => goTo(agentRoutes.HowToNotifyAboutSecuritiesTransferController.onPageLoad(), Some(userAnswers))
    case HowToNotifyAboutSecuritiesTransferPage => userAnswers => {
      dataDependent(HowToNotifyAboutSecuritiesTransferPage, userAnswers) {
        case OneAtATime => agentSingleRoutes.AgentReferenceController.onPageLoad(NormalMode)
        case MoreThanOneAtATime => agentBulkRoutes.TemplateInstructionsController.onPageLoad()
      }
    }
    case AgentReferencePage => userAnswers => {
      dataDependent(HowToNotifyAboutSecuritiesTransferPage, userAnswers) {
        case OneAtATime => agentSingleRoutes.NameOfBuyerController.onPageLoad(NormalMode)
        case MoreThanOneAtATime => routes.CheckYourAnswersController.onPageLoad()
      }
    }
    case NameOfBuyerPage => userAnswers => dataRequired(NameOfBuyerPage, userAnswers, agentSingleRoutes.AddressController.onPageLoad())
    case StfBuyersAddressPage => userAnswers => dataRequired(StfBuyersAddressPage, userAnswers, agentSingleRoutes.NameOfSellerController.onPageLoad(NormalMode))
    case NameOfSellerPage => userAnswers => dataRequired(NameOfSellerPage, userAnswers, agentSingleRoutes.StfSellerAddressController.onPageLoad())
    case StfSellerAddressPage => userAnswers => dataRequired(StfSellerAddressPage, userAnswers, agentSingleRoutes.ConnectedPersonsController.onPageLoad(NormalMode))
    case ConnectedPersonsPage => userAnswers => dataRequired(ConnectedPersonsPage, userAnswers, agentSingleRoutes.ApplyingForReliefController.onPageLoad(NormalMode))
    case ApplyingForReliefPage => userAnswers => dataDependent(ApplyingForReliefPage, userAnswers) {
      case true => agentSingleRoutes.WhatReliefAreYouApplyingForController.onPageLoad(NormalMode)
      case false => agentSingleRoutes.SecuritiesTargetController.onPageLoad(NormalMode)
    }
    case WhatReliefAreYouApplyingForPage => userAnswers => dataRequired(WhatReliefAreYouApplyingForPage, userAnswers, agentSingleRoutes.SecuritiesTargetController.onPageLoad(NormalMode))
    case SecuritiesTargetPage => userAnswers => dataRequired(SecuritiesTargetPage, userAnswers, agentSingleRoutes.ChargingPointController.onPageLoad(NormalMode))
    case ChargingPointPage => userAnswers => dataDependent(ChargingPointPage, userAnswers) {enterDate =>
      if (enterDate.isBefore(firstDate)) defaultPage
      else agentSingleRoutes.TaxRateController.onPageLoad(NormalMode)
    }
    case TaxRatePage => userAnswers => dataRequired(TaxRatePage, userAnswers, agentSingleRoutes.PurchasingSharesController.onPageLoad(NormalMode))
    case OtherSecuritiesTypePage => userAnswers => dataRequired(OtherSecuritiesTypePage, userAnswers, agentSingleRoutes.AmountPaidForSecuritiesController.onPageLoad(NormalMode))
    case AmountPaidForSecuritiesPage => userAnswers =>
      dataDependent(ConnectedPersonsPage, userAnswers) { isConnected =>
        if (isConnected)
          agentSingleRoutes.TotalMarketValueController.onPageLoad(NormalMode)
        else
          stfSingleCyaRoutes.CheckYourAnswersController.onPageLoad()
      }
    case TotalMarketValuePage => userAnswers => dataRequired(TotalMarketValuePage, userAnswers, stfSingleCyaRoutes.CheckYourAnswersController.onPageLoad())
    case PurchasingSharesPage => userAnswers =>
      dataDependent(PurchasingSharesPage,userAnswers) { isPurchasingShares =>
        if(isPurchasingShares)
          agentSingleRoutes.DetailsOfThisTransferController.onPageLoad(NormalMode)
        else
          agentSingleRoutes.OtherSecuritiesTypeController.onPageLoad(NormalMode)
      }
    case DetailsOfThisTransferPage => userAnswers => dataRequired(DetailsOfThisTransferPage, userAnswers, stfSingleCyaRoutes.CheckYourAnswersController.onPageLoad())  

    case _ => _ => Future.successful(defaultPage)
  }
