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

package uk.gov.hmrc.securitiestransferchargefrontend.navigation.stf.organisations

import play.api.mvc.Call
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.securitiestransferchargefrontend.config.FrontendAppConfig
import uk.gov.hmrc.securitiestransferchargefrontend.controllers.routes
import uk.gov.hmrc.securitiestransferchargefrontend.controllers.stf.organisations.single.routes as orgSingleRoutes
import uk.gov.hmrc.securitiestransferchargefrontend.controllers.stf.organisations.bulk.routes as orgBulkRoutes
import uk.gov.hmrc.securitiestransferchargefrontend.controllers.stf.organisations.routes as orgRoutes
import uk.gov.hmrc.securitiestransferchargefrontend.controllers.stf.shared.single.routes as stfSingleCyaRoutes
import uk.gov.hmrc.securitiestransferchargefrontend.models.stf.HowToNotifyAboutSecuritiesTransfer.{MoreThanOneAtATime, OneAtATime}
import uk.gov.hmrc.securitiestransferchargefrontend.models.stf.WhatTypeOfSecurities
import uk.gov.hmrc.securitiestransferchargefrontend.models.{NormalMode, UserAnswers}
import uk.gov.hmrc.securitiestransferchargefrontend.navigation.PersistentNavigationHelper
import uk.gov.hmrc.securitiestransferchargefrontend.pages.*
import uk.gov.hmrc.securitiestransferchargefrontend.pages.stf.bulk.TemplateInstructionsPage
import uk.gov.hmrc.securitiestransferchargefrontend.pages.stf.shared.{HowToNotifyAboutSecuritiesTransferPage, SubmissionsDashboardPage}
import uk.gov.hmrc.securitiestransferchargefrontend.pages.stf.single.{AmountPaidForSecuritiesPage, ApplyingForReliefPage, ChargingPointPage, ConfirmAddressPage, ConnectedPersonsPage, DetailsOfThisTransferPage, NameOfSellerPage, OtherSecuritiesTypePage, SecuritiesTargetPage, StfBuyersAddressPage, StfSellerAddressPage, TaxRatePage, TotalMarketValuePage, WhatReliefAreYouApplyingForPage, WhatTypeOfSecuritiesPage}
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

    case SubmissionsDashboardPage => userAnswers => goTo(orgRoutes.HowToNotifyAboutSecuritiesTransferController.onPageLoad(), Some(userAnswers))
    case HowToNotifyAboutSecuritiesTransferPage => userAnswers => {
      dataDependent(HowToNotifyAboutSecuritiesTransferPage, userAnswers) {
        case OneAtATime => orgSingleRoutes.ConfirmAddressController.onPageLoad()
        case MoreThanOneAtATime => orgBulkRoutes.TemplateInstructionsController.onPageLoad()
      }
    }
    case ConfirmAddressPage => userAnswers => dataRequired(ConfirmAddressPage, userAnswers, orgSingleRoutes.NameOfSellerController.onPageLoad(NormalMode))
    case StfBuyersAddressPage => userAnswers => dataRequired(StfBuyersAddressPage, userAnswers, orgSingleRoutes.NameOfSellerController.onPageLoad(NormalMode))
    case NameOfSellerPage => userAnswers => dataRequired(NameOfSellerPage, userAnswers, orgSingleRoutes.StfSellerAddressController.onPageLoad())
    case StfSellerAddressPage => userAnswers => dataRequired(StfSellerAddressPage, userAnswers, orgSingleRoutes.ConnectedPersonsController.onPageLoad(NormalMode))
    case ConnectedPersonsPage => userAnswers => dataRequired(ConnectedPersonsPage, userAnswers, orgSingleRoutes.ApplyingForReliefController.onPageLoad(NormalMode))
    case ApplyingForReliefPage => userAnswers =>
      dataDependent(ApplyingForReliefPage, userAnswers) {
        case true => orgSingleRoutes.WhatReliefAreYouApplyingForController.onPageLoad(NormalMode)
        case false => orgSingleRoutes.SecuritiesTargetController.onPageLoad(NormalMode)
      }
    case WhatReliefAreYouApplyingForPage => userAnswers => dataRequired(WhatReliefAreYouApplyingForPage, userAnswers, orgSingleRoutes.SecuritiesTargetController.onPageLoad(NormalMode))
    case SecuritiesTargetPage => userAnswers => dataRequired(SecuritiesTargetPage, userAnswers, orgSingleRoutes.ChargingPointController.onPageLoad(NormalMode))
    case ChargingPointPage => userAnswers => dataDependent(ChargingPointPage, userAnswers) {enterDate =>
      if (enterDate.isBefore(firstDate)) routes.JourneyRecoveryController.onPageLoad()
      else orgSingleRoutes.TaxRateController.onPageLoad(NormalMode)
    }
    case TaxRatePage => userAnswers => dataRequired(TaxRatePage, userAnswers, orgSingleRoutes.WhatTypeOfSecuritiesController.onPageLoad(NormalMode))
    case OtherSecuritiesTypePage => userAnswers => dataRequired(OtherSecuritiesTypePage, userAnswers, orgSingleRoutes.AmountPaidForSecuritiesController.onPageLoad(NormalMode))
    case WhatTypeOfSecuritiesPage => userAnswers =>
      dataDependent(WhatTypeOfSecuritiesPage, userAnswers) {
        case WhatTypeOfSecurities.Shares => orgSingleRoutes.DetailsOfThisTransferController.onPageLoad(NormalMode)
        case WhatTypeOfSecurities.Other => orgSingleRoutes.OtherSecuritiesTypeController.onPageLoad(NormalMode)
      }
    case DetailsOfThisTransferPage => userAnswers => dataRequired(DetailsOfThisTransferPage, userAnswers, stfSingleCyaRoutes.CheckYourAnswersController.onPageLoad())
    case AmountPaidForSecuritiesPage => userAnswers =>
      dataDependent(ConnectedPersonsPage, userAnswers) { isConnected =>
        if (isConnected) orgSingleRoutes.TotalMarketValueController.onPageLoad(NormalMode)
        else stfSingleCyaRoutes.CheckYourAnswersController.onPageLoad()
      }
    case TotalMarketValuePage => userAnswers => dataRequired(TotalMarketValuePage, userAnswers, stfSingleCyaRoutes.CheckYourAnswersController.onPageLoad())
    case _ => _ => Future.successful(defaultPage)
  }
