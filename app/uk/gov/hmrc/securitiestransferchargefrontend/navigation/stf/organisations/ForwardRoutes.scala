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
import uk.gov.hmrc.securitiestransferchargefrontend.controllers.routes
import uk.gov.hmrc.securitiestransferchargefrontend.controllers.stf.organisations.routes as orgRoutes
import uk.gov.hmrc.securitiestransferchargefrontend.models.HowToNotifyAboutSecuritiesTransfer.{MoreThanOneAtATime, OneAtATime}
import uk.gov.hmrc.securitiestransferchargefrontend.models.{NormalMode, UserAnswers}
import uk.gov.hmrc.securitiestransferchargefrontend.navigation.PersistentNavigationHelper
import uk.gov.hmrc.securitiestransferchargefrontend.pages.*
import uk.gov.hmrc.securitiestransferchargefrontend.services.AnswerPersistenceService

import scala.concurrent.{ExecutionContext, Future}

class ForwardRoutes(answerPersistenceService: AnswerPersistenceService,
                    defaultPage: Call,
                    errorPages: Seq[Call])
                   (implicit ec: ExecutionContext):

  val helper = new PersistentNavigationHelper(answerPersistenceService, defaultPage, errorPages)

  import helper.*

  def forwardRoutes(page: Page)(implicit hc: HeaderCarrier): UserAnswers => Future[Call] = page match {

    case SubmissionsDashboardPage => userAnswers => goTo(orgRoutes.HowToNotifyAboutSecuritiesTransferController.onPageLoad(NormalMode), Some(userAnswers))
    case HowToNotifyAboutSecuritiesTransferPage => userAnswers => {
      dataDependent(HowToNotifyAboutSecuritiesTransferPage, userAnswers) {
        case OneAtATime => orgRoutes.ConfirmAddressController.onPageLoad()
        case MoreThanOneAtATime => defaultPage
      }
    }
    case ConfirmAddressPage => userAnswers => dataRequired(ConfirmAddressPage, userAnswers, orgRoutes.NameOfSellerController.onPageLoad(NormalMode))
    case StfBuyersAddressPage => userAnswers => dataRequired(StfBuyersAddressPage, userAnswers, orgRoutes.NameOfSellerController.onPageLoad(NormalMode))
    case NameOfSellerPage => userAnswers => dataRequired(NameOfSellerPage, userAnswers, orgRoutes.StfSellerAddressController.onPageLoad())
    case StfSellerAddressPage => userAnswers => dataRequired(StfSellerAddressPage, userAnswers, orgRoutes.ConnectedPersonsController.onPageLoad(NormalMode))
    case ConnectedPersonsPage => userAnswers => dataRequired(ConnectedPersonsPage, userAnswers, orgRoutes.ApplyingForReliefController.onPageLoad(NormalMode))
    case ApplyingForReliefPage => userAnswers =>
      dataDependent(ApplyingForReliefPage, userAnswers) {
        case true => orgRoutes.WhatReliefAreYouApplyingForController.onPageLoad(NormalMode)
        case false => orgRoutes.SecuritiesTargetController.onPageLoad(NormalMode)
      }
    case WhatReliefAreYouApplyingForPage => userAnswers => dataRequired(WhatReliefAreYouApplyingForPage, userAnswers, routes.SubmissionsDashboardController.onPageLoad())
    case SecuritiesTargetPage => userAnswers => dataRequired(SecuritiesTargetPage, userAnswers, routes.JourneyRecoveryController.onPageLoad())
    case TaxRatePage => userAnswers => dataRequired(TaxRatePage, userAnswers, orgRoutes.WhatTypeOfSecuritiesController.onPageLoad(NormalMode))
    case WhatTypeOfSecuritiesPage => userAnswers => dataRequired(SecuritiesTargetPage, userAnswers, routes.JourneyRecoveryController.onPageLoad())
    case OtherSecuritiesTypePage => userAnswers => dataRequired(OtherSecuritiesTypePage, userAnswers, orgRoutes.AmountPaidForSecuritiesController.onPageLoad(NormalMode))
    case AmountPaidForSecuritiesPage => userAnswers =>
      userAnswersDependent(userAnswers) {
        userAnswers =>
          userAnswers.get(ConnectedPersonsPage).fold(defaultPage) {
            isConnected =>
              if (isConnected) routes.JourneyRecoveryController.onPageLoad()
              else routes.CheckYourAnswersController.onPageLoad()
          }
      }
    case _ => _ => Future.successful(defaultPage)
  }
