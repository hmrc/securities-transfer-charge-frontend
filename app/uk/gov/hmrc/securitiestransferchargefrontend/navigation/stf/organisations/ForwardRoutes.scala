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
import uk.gov.hmrc.securitiestransferchargefrontend.controllers.stf.organisations.bulk.routes as orgBulkRoutes
import uk.gov.hmrc.securitiestransferchargefrontend.controllers.stf.organisations.routes as orgRoutes
import uk.gov.hmrc.securitiestransferchargefrontend.controllers.stf.organisations.single.routes as orgSingleRoutes
import uk.gov.hmrc.securitiestransferchargefrontend.controllers.stf.shared.single.routes as stfSingleCyaRoutes
import uk.gov.hmrc.securitiestransferchargefrontend.models.stf.HowToNotifyAboutSecuritiesTransfer.{MoreThanOneAtATime, OneAtATime}
import uk.gov.hmrc.securitiestransferchargefrontend.models.{CheckMode, Mode, NormalMode, UserAnswers}
import uk.gov.hmrc.securitiestransferchargefrontend.navigation.PersistentNavigationHelper
import uk.gov.hmrc.securitiestransferchargefrontend.pages.*
import uk.gov.hmrc.securitiestransferchargefrontend.pages.stf.shared.{HowToNotifyAboutSecuritiesTransferPage, SubmissionsDashboardPage}
import uk.gov.hmrc.securitiestransferchargefrontend.pages.stf.single.*
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
  private lazy val cyaPage = stfSingleCyaRoutes.CheckYourAnswersController.onPageLoad()
  
  def forwardRoutes(page: Page, mode: Mode)(implicit hc: HeaderCarrier): UserAnswers => Future[Call] = mode match {
    case NormalMode => normalRoutes(page)
    case CheckMode => checkRoutes(page)
  }
  
  private def normalRoutes(page: Page)(implicit hc: HeaderCarrier): UserAnswers => Future[Call] = page match {

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
    case TaxRatePage => userAnswers => dataRequired(TaxRatePage, userAnswers, orgSingleRoutes.PurchasingSharesController.onPageLoad(NormalMode))
    case OtherSecuritiesTypePage => userAnswers => dataRequired(OtherSecuritiesTypePage, userAnswers, orgSingleRoutes.AmountPaidForSecuritiesController.onPageLoad(NormalMode))
    case PurchasingSharesPage => userAnswers =>
      dataDependent(PurchasingSharesPage,userAnswers) { isPurchasingShares =>
        if(isPurchasingShares)
          orgSingleRoutes.DetailsOfThisTransferController.onPageLoad(NormalMode)
        else
          orgSingleRoutes.OtherSecuritiesTypeController.onPageLoad(NormalMode)
      }
    case DetailsOfThisTransferPage => userAnswers => dataRequired(DetailsOfThisTransferPage, userAnswers, cyaPage)
    case AmountPaidForSecuritiesPage => userAnswers =>
      dataDependent(ConnectedPersonsPage, userAnswers) { isConnected =>
        if (isConnected) orgSingleRoutes.TotalMarketValueController.onPageLoad(NormalMode)
        else cyaPage
      }
    case TotalMarketValuePage => userAnswers => dataRequired(TotalMarketValuePage, userAnswers, cyaPage)
    case _ => _ => Future.successful(defaultPage)
  }
  
  def checkRoutes(page: Page)(implicit hc: HeaderCarrier): UserAnswers => Future[Call] = page match {
    case _ => userAnswers => goTo(cyaPage, Some(userAnswers))
  }
