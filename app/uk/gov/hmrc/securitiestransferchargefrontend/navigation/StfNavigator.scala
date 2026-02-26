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

package uk.gov.hmrc.securitiestransferchargefrontend.navigation

import play.api.mvc.{Call, Request}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.http.HeaderCarrierConverter
import uk.gov.hmrc.securitiestransferchargefrontend.controllers.routes
import uk.gov.hmrc.securitiestransferchargefrontend.controllers.individuals.routes as individualRoutes
import uk.gov.hmrc.securitiestransferchargefrontend.models.HowToNotifyAboutSecuritiesTransfer.{MoreThanOneAtATime, OneAtATime}
import uk.gov.hmrc.securitiestransferchargefrontend.models.{CheckMode, Mode, NormalMode, UserAnswers, WhatTypeOfSecurities}
import uk.gov.hmrc.securitiestransferchargefrontend.pages.*
import uk.gov.hmrc.securitiestransferchargefrontend.pages.individuals.{AmountPaidForSecuritiesPage, ApplyingForReliefPage, ChargingPointPage, ConfirmAddressPage, ConnectedPersonsPage, DetailsOfThisTransferPage, HowToNotifyAboutSecuritiesTransferPage, NameOfSellerPage, OtherSecuritiesTypePage, SecuritiesTargetPage, SellerAddressPage, StfBuyersAddressPage, TaxRatePage, TotalMarketValuePage, WhatReliefAreYouApplyingForPage, WhatTypeOfSecuritiesPage}
import uk.gov.hmrc.securitiestransferchargefrontend.queries.Gettable
import uk.gov.hmrc.securitiestransferchargefrontend.services.AnswerPersistenceService

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class StfNavigator @Inject()(answerPersistenceService: AnswerPersistenceService)
                            (implicit ec: ExecutionContext) extends AbstractNavigator(answerPersistenceService) {

  private def normalRoutes(page: Page)(implicit hc: HeaderCarrier): UserAnswers => Future[Call] = page match {

    case SubmissionsDashboardPage => userAnswers => goTo(individualRoutes.HowToNotifyAboutSecuritiesTransferController.onPageLoad(NormalMode), Some(userAnswers))
    case HowToNotifyAboutSecuritiesTransferPage => userAnswers => {
      dataDependent(HowToNotifyAboutSecuritiesTransferPage, userAnswers) {
        case OneAtATime => individualRoutes.ConfirmAddressController.onPageLoad()
        case MoreThanOneAtATime => Navigator.defaultPage
      }
    }
    case ConfirmAddressPage => userAnswers => dataRequired(ConfirmAddressPage, userAnswers, individualRoutes.NameOfSellerController.onPageLoad(NormalMode))
    case StfBuyersAddressPage => userAnswers => dataRequired(StfBuyersAddressPage, userAnswers, individualRoutes.NameOfSellerController.onPageLoad(NormalMode))
    case NameOfSellerPage => userAnswers => dataRequired(NameOfSellerPage, userAnswers, individualRoutes.StfSellerAddressController.onPageLoad())
    case SellerAddressPage => userAnswers => dataRequired(SellerAddressPage, userAnswers, individualRoutes.ConnectedPersonsController.onPageLoad(NormalMode))
    case ConnectedPersonsPage => userAnswers => dataRequired(ConnectedPersonsPage, userAnswers, individualRoutes.ApplyingForReliefController.onPageLoad(NormalMode))
    case ApplyingForReliefPage => userAnswers =>
      dataDependent(ApplyingForReliefPage, userAnswers) {
        case true => individualRoutes.WhatReliefAreYouApplyingForController.onPageLoad(NormalMode)
        case false => individualRoutes.SecuritiesTargetController.onPageLoad(NormalMode)
      }
    case WhatReliefAreYouApplyingForPage => userAnswers => dataRequired(WhatReliefAreYouApplyingForPage, userAnswers, individualRoutes.SecuritiesTargetController.onPageLoad(NormalMode))
    case SecuritiesTargetPage => userAnswers => dataRequired(SecuritiesTargetPage, userAnswers, individualRoutes.ChargingPointController.onPageLoad(NormalMode))
    case ChargingPointPage => userAnswers => dataRequired(ChargingPointPage, userAnswers, individualRoutes.TaxRateController.onPageLoad(NormalMode))
    case TaxRatePage => userAnswers => dataRequired(TaxRatePage, userAnswers, individualRoutes.WhatTypeOfSecuritiesController.onPageLoad(NormalMode))
    case OtherSecuritiesTypePage => userAnswers => dataRequired(OtherSecuritiesTypePage, userAnswers, individualRoutes.AmountPaidForSecuritiesController.onPageLoad(NormalMode))

    case WhatTypeOfSecuritiesPage => userAnswers =>
      dataDependent(WhatTypeOfSecuritiesPage, userAnswers) {
        case WhatTypeOfSecurities.Shares => individualRoutes.DetailsOfThisTransferController.onPageLoad(NormalMode)
        case WhatTypeOfSecurities.Other => individualRoutes.OtherSecuritiesTypeController.onPageLoad(NormalMode)
      }
    case DetailsOfThisTransferPage => userAnswers => dataRequired(DetailsOfThisTransferPage, userAnswers, routes.CheckYourAnswersController.onPageLoad())
    case OtherSecuritiesTypePage => userAnswers => dataRequired(OtherSecuritiesTypePage, userAnswers, routes.AmountPaidForSecuritiesController.onPageLoad(NormalMode))
    case AmountPaidForSecuritiesPage => userAnswers =>
      userAnswersDependent(userAnswers) {
        userAnswers =>
          userAnswers.get(ConnectedPersonsPage).fold(Navigator.defaultPage) {
            isConnected =>
              if (isConnected) individualRoutes.TotalMarketValueController.onPageLoad(NormalMode)
              else routes.CheckYourAnswersController.onPageLoad()
          }
      }
    case TotalMarketValuePage => userAnswers => dataRequired(TotalMarketValuePage, userAnswers, routes.CheckYourAnswersController.onPageLoad())
    case _ => _ => Navigator.defaultPageF

  }

  val checkRouteMap: Page => UserAnswers => Call = (_ => _ => routes.CheckYourAnswersController.onPageLoad())

  def nextPage(page: Page, mode: Mode, userAnswers: UserAnswers)(implicit request: Request[?]): Future[Call] = {
    mode match {
      case NormalMode =>
        implicit val hc: HeaderCarrier = HeaderCarrierConverter.fromRequestAndSession(request, request.session)
        normalRoutes(page)(hc)(userAnswers)
      case CheckMode => Future.successful(checkRouteMap(page)(userAnswers))
    }
  }

  val errorPage: Page => Call = {
    case _: Gettable[?] => ???
    case _ => routes.JourneyRecoveryController.onPageLoad()
  }
}
