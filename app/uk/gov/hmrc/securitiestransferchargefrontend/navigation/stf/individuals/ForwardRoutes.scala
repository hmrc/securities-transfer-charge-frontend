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

package uk.gov.hmrc.securitiestransferchargefrontend.navigation.stf.individuals

import play.api.mvc.Call
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.securitiestransferchargefrontend.config.FrontendAppConfig
import uk.gov.hmrc.securitiestransferchargefrontend.controllers.routes
import uk.gov.hmrc.securitiestransferchargefrontend.controllers.stf.individuals.bulk.routes as individualBulkRoutes
import uk.gov.hmrc.securitiestransferchargefrontend.controllers.stf.individuals.routes as individualRoutes
import uk.gov.hmrc.securitiestransferchargefrontend.controllers.stf.individuals.single.routes as individualSingleRoutes
import uk.gov.hmrc.securitiestransferchargefrontend.controllers.stf.shared.single.routes as stfSingleCyaRoutes
import uk.gov.hmrc.securitiestransferchargefrontend.models.stf.HowToNotifyAboutSecuritiesTransfer.{MoreThanOneAtATime, OneAtATime}
import uk.gov.hmrc.securitiestransferchargefrontend.models.stf.WhatTypeOfSecurities
import uk.gov.hmrc.securitiestransferchargefrontend.models.{NormalMode, UserAnswers}
import uk.gov.hmrc.securitiestransferchargefrontend.navigation.*
import uk.gov.hmrc.securitiestransferchargefrontend.pages.*
import uk.gov.hmrc.securitiestransferchargefrontend.pages.stf.shared.{HowToNotifyAboutSecuritiesTransferPage, SubmissionsDashboardPage}
import uk.gov.hmrc.securitiestransferchargefrontend.pages.stf.single.{AmountPaidForSecuritiesPage, ApplyingForReliefPage, ChargingPointPage, ConfirmAddressPage, ConnectedPersonsPage, DetailsOfThisTransferPage, NameOfSellerPage, OtherSecuritiesTypePage, SecuritiesTargetPage, StfBuyersAddressPage, StfSellerAddressPage, TaxRatePage, TotalMarketValuePage, WhatReliefAreYouApplyingForPage, WhatTypeOfSecuritiesPage}
import uk.gov.hmrc.securitiestransferchargefrontend.pages.JourneyRecoveryPage
import uk.gov.hmrc.securitiestransferchargefrontend.pages.stf.bulk.TemplateInstructionsPage
import uk.gov.hmrc.securitiestransferchargefrontend.services.AnswerPersistenceService

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
        case Some(OneAtATime) => ConfirmAddressPage
        case Some(MoreThanOneAtATime) => TemplateInstructionsPage
        case _ => JourneyRecoveryPage
      }
    case ConfirmAddressPage => NameOfSellerPage
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
    case TaxRatePage => WhatTypeOfSecuritiesPage
    case OtherSecuritiesTypePage => AmountPaidForSecuritiesPage
    case WhatTypeOfSecuritiesPage =>
      userAnswers.get(WhatTypeOfSecuritiesPage) match {
        case Some(WhatTypeOfSecurities.Shares) => DetailsOfThisTransferPage
        case Some(WhatTypeOfSecurities.Other) => OtherSecuritiesTypePage
        case _ => JourneyRecoveryPage
      }
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

    case SubmissionsDashboardPage => userAnswers => goTo(individualRoutes.HowToNotifyAboutSecuritiesTransferController.onPageLoad(), Some(userAnswers))
    case HowToNotifyAboutSecuritiesTransferPage => userAnswers => {
      dataDependent(HowToNotifyAboutSecuritiesTransferPage, userAnswers) {
        case OneAtATime => individualSingleRoutes.ConfirmAddressController.onPageLoad()
        case MoreThanOneAtATime => individualBulkRoutes.TemplateInstructionsController.onPageLoad()
      }
    }
    case ConfirmAddressPage => userAnswers => dataRequired(ConfirmAddressPage, userAnswers, individualSingleRoutes.NameOfSellerController.onPageLoad(NormalMode))
    case StfBuyersAddressPage => userAnswers => dataRequired(StfBuyersAddressPage, userAnswers, individualSingleRoutes.NameOfSellerController.onPageLoad(NormalMode))
    case NameOfSellerPage => userAnswers => dataRequired(NameOfSellerPage, userAnswers, individualSingleRoutes.StfSellerAddressController.onPageLoad())
    case StfSellerAddressPage => userAnswers => dataRequired(StfSellerAddressPage, userAnswers, individualSingleRoutes.ConnectedPersonsController.onPageLoad(NormalMode))
    case ConnectedPersonsPage => userAnswers => dataRequired(ConnectedPersonsPage, userAnswers, individualSingleRoutes.ApplyingForReliefController.onPageLoad(NormalMode))
    case ApplyingForReliefPage => userAnswers =>
      dataDependent(ApplyingForReliefPage, userAnswers) {
        case true => individualSingleRoutes.WhatReliefAreYouApplyingForController.onPageLoad(NormalMode)
        case false => individualSingleRoutes.SecuritiesTargetController.onPageLoad(NormalMode)
      }
    case WhatReliefAreYouApplyingForPage => userAnswers => dataRequired(WhatReliefAreYouApplyingForPage, userAnswers, individualSingleRoutes.SecuritiesTargetController.onPageLoad(NormalMode))
    case SecuritiesTargetPage => userAnswers => dataRequired(SecuritiesTargetPage, userAnswers, individualSingleRoutes.ChargingPointController.onPageLoad(NormalMode))
    case ChargingPointPage => userAnswers => dataDependent(ChargingPointPage, userAnswers) {enterDate =>
      if (enterDate.isBefore(firstDate)) routes.JourneyRecoveryController.onPageLoad()
      else individualSingleRoutes.TaxRateController.onPageLoad(NormalMode)
    }
    case TaxRatePage => userAnswers => dataRequired(TaxRatePage, userAnswers, individualSingleRoutes.WhatTypeOfSecuritiesController.onPageLoad(NormalMode))
    case OtherSecuritiesTypePage => userAnswers => dataRequired(OtherSecuritiesTypePage, userAnswers, individualSingleRoutes.AmountPaidForSecuritiesController.onPageLoad(NormalMode))
    case WhatTypeOfSecuritiesPage => userAnswers =>
      dataDependent(WhatTypeOfSecuritiesPage, userAnswers) {
        case WhatTypeOfSecurities.Shares => individualSingleRoutes.DetailsOfThisTransferController.onPageLoad(NormalMode)
        case WhatTypeOfSecurities.Other => individualSingleRoutes.OtherSecuritiesTypeController.onPageLoad(NormalMode)
      }
    case DetailsOfThisTransferPage => userAnswers => dataRequired(DetailsOfThisTransferPage, userAnswers, stfSingleCyaRoutes.CheckYourAnswersController.onPageLoad())
    case AmountPaidForSecuritiesPage => userAnswers =>
      userAnswersDependent(userAnswers) {
        userAnswers =>
          userAnswers.get(ConnectedPersonsPage).fold(defaultPage) {
            isConnected =>
              if (isConnected) individualSingleRoutes.TotalMarketValueController.onPageLoad(NormalMode)
              else stfSingleCyaRoutes.CheckYourAnswersController.onPageLoad()
          }
      }
    case TotalMarketValuePage => userAnswers => dataRequired(TotalMarketValuePage, userAnswers, stfSingleCyaRoutes.CheckYourAnswersController.onPageLoad())
    case _ => _ => Future.successful(defaultPage)
  }
