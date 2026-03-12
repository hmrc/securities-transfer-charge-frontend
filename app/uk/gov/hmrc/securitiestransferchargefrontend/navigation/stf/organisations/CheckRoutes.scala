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
import uk.gov.hmrc.securitiestransferchargefrontend.controllers.stf.organisations.routes as orgRoutes
import uk.gov.hmrc.securitiestransferchargefrontend.models.{CheckMode, UserAnswers, WhatTypeOfSecurities}
import uk.gov.hmrc.securitiestransferchargefrontend.navigation.*
import uk.gov.hmrc.securitiestransferchargefrontend.pages.*
import uk.gov.hmrc.securitiestransferchargefrontend.services.AnswerPersistenceService

import scala.concurrent.{ExecutionContext, Future}


class CheckRoutes(answerPersistenceService: AnswerPersistenceService,
                  defaultPage: Call,
                  errorPages: Seq[Call])
                 (implicit ec: ExecutionContext):
  
  val helper = new PersistentNavigationHelper(answerPersistenceService, defaultPage, errorPages)
  import helper.*

  def checkRoutes(page: Page)(implicit hc: HeaderCarrier): UserAnswers => Future[Call] = page match {
    case ApplyingForReliefPage =>
      userAnswers =>

        val nextPage = userAnswers.get(ApplyingForReliefPage) match {

          case Some(true) if userAnswers.get(WhatReliefAreYouApplyingForPage).isEmpty =>
            orgRoutes.WhatReliefAreYouApplyingForController.onPageLoad(CheckMode)

          case _ =>
            orgRoutes.CheckYourAnswersController.onPageLoad()
        }

        goTo(nextPage, Some(userAnswers))

    case WhatTypeOfSecuritiesPage =>
      userAnswers =>
        val nextPage =
          userAnswers.get(WhatTypeOfSecuritiesPage) match {

            case Some(WhatTypeOfSecurities.Other) if userAnswers.get(OtherSecuritiesTypePage).isEmpty =>
              orgRoutes.OtherSecuritiesTypeController.onPageLoad(CheckMode)

            case Some(WhatTypeOfSecurities.Shares) if userAnswers.get(DetailsOfThisTransferPage).isEmpty =>
              orgRoutes.DetailsOfThisTransferController.onPageLoad(CheckMode)

            case _ =>
              orgRoutes.CheckYourAnswersController.onPageLoad()
          }

        goTo(nextPage, Some(userAnswers))

    case OtherSecuritiesTypePage =>
      userAnswers =>

        val nextPage =
          userAnswers.get(AmountPaidForSecuritiesPage) match {
            case None =>
              orgRoutes.AmountPaidForSecuritiesController.onPageLoad(CheckMode)

            case Some(_) =>
              orgRoutes.CheckYourAnswersController.onPageLoad()
          }

        goTo(nextPage, Some(userAnswers))

    case AmountPaidForSecuritiesPage =>
      userAnswers =>
        val nextPage =
          (userAnswers.get(ConnectedPersonsPage), userAnswers.get(TotalMarketValuePage)) match {
            case (Some(true), None) =>
              orgRoutes.TotalMarketValueController.onPageLoad(CheckMode)

            case _ =>
              orgRoutes.CheckYourAnswersController.onPageLoad()
          }

        goTo(nextPage, Some(userAnswers))

    case ConnectedPersonsPage =>
      userAnswers =>

        val nextPage = userAnswers.get(ConnectedPersonsPage) match {

          case Some(true) if userAnswers.get(DetailsOfThisTransferPage).forall(_.marketValue.isEmpty) =>
            orgRoutes.DetailsOfThisTransferController.onPageLoad(CheckMode)

          case _ =>
            orgRoutes.CheckYourAnswersController.onPageLoad()
        }

        goTo(nextPage, Some(userAnswers))


    case _ => userAnswers => goTo(orgRoutes.CheckYourAnswersController.onPageLoad(), Some(userAnswers))
  }

