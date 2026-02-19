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
import uk.gov.hmrc.securitiestransferchargefrontend.clients.SaveAndReturnClient
import uk.gov.hmrc.securitiestransferchargefrontend.controllers.routes
import uk.gov.hmrc.securitiestransferchargefrontend.controllers.seller.routes as sellerRoutes

import uk.gov.hmrc.securitiestransferchargefrontend.models.HowToNotifyAboutSecuritiesTransfer.{MoreThanOneAtATime, OneAtATime}
import uk.gov.hmrc.securitiestransferchargefrontend.models.{CheckMode, Mode, NormalMode, UserAnswers}
import uk.gov.hmrc.securitiestransferchargefrontend.pages.*
import uk.gov.hmrc.securitiestransferchargefrontend.queries.Gettable
import uk.gov.hmrc.securitiestransferchargefrontend.repositories.SessionRepository

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class StfNavigator @Inject()(sessionRepository: SessionRepository,
                             saveAndReturnClient: SaveAndReturnClient)
                            (implicit ec: ExecutionContext) extends AbstractNavigator(sessionRepository, saveAndReturnClient) {

  private def normalRoutes(page: Page)(implicit hc: HeaderCarrier): UserAnswers => Future[Call] = page match {

    case SubmissionsDashboardPage => userAnswers => goTo(routes.HowToNotifyAboutSecuritiesTransferController.onPageLoad(NormalMode), Some(userAnswers))
    case HowToNotifyAboutSecuritiesTransferPage => userAnswers => {
      dataDependent(HowToNotifyAboutSecuritiesTransferPage, userAnswers) {
        case OneAtATime => routes.ConfirmAddressController.onPageLoad()
        case MoreThanOneAtATime => ???
      }
    }
    case StfBuyersAddressPage => userAnswers => dataRequired(StfBuyersAddressPage, userAnswers, routes.NameOfSellerController.onPageLoad(NormalMode))
    case NameOfSellerPage => userAnswers => dataRequired(NameOfSellerPage, userAnswers,sellerRoutes.StfSellerAddressController.onPageLoad())
    case ConfirmAddressPage => userAnswers => dataRequired(ConfirmAddressPage, userAnswers, routes.NameOfSellerController.onPageLoad(NormalMode))
    case ConnectedPersonsPage => userAnswers => dataRequired(ConnectedPersonsPage, userAnswers, routes.ApplyingForReliefController.onPageLoad(NormalMode))
    case SellerAddressPage => userAnswers => dataRequired(SellerAddressPage, userAnswers, routes.ConnectedPersonsController.onPageLoad(NormalMode))
    case ApplyingForReliefPage => userAnswers => dataDependent(ApplyingForReliefPage,userAnswers){
      case true => routes.WhatReliefAreYouApplyingForController.onPageLoad(NormalMode)
      case false => routes.SecuritiesTargetController.onPageLoad(NormalMode)
    }
    case WhatReliefAreYouApplyingForPage => userAnswers => dataRequired(WhatReliefAreYouApplyingForPage, userAnswers, routes.SecuritiesTargetController.onPageLoad(NormalMode))
    case SecuritiesTargetPage => userAnswers => dataRequired(SecuritiesTargetPage, userAnswers, routes.ChargingPointController.onPageLoad(NormalMode))
    case _ => _ => defaultPageF

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
