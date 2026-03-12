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
import uk.gov.hmrc.securitiestransferchargefrontend.controllers.stf.individuals.routes as individualRoutes
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
            individualRoutes.WhatReliefAreYouApplyingForController.onPageLoad(CheckMode)

          case _ =>
            individualRoutes.CheckYourAnswersController.onPageLoad()
        }

        goTo(nextPage, Some(userAnswers))

    case WhatTypeOfSecuritiesPage =>
      userAnswers =>
        val nextPage =
          userAnswers.get(WhatTypeOfSecuritiesPage) match {

            case Some(WhatTypeOfSecurities.Other) if userAnswers.get(OtherSecuritiesTypePage).isEmpty =>
              individualRoutes.OtherSecuritiesTypeController.onPageLoad(CheckMode)

            case Some(WhatTypeOfSecurities.Shares) if userAnswers.get(DetailsOfThisTransferPage).isEmpty =>
              individualRoutes.DetailsOfThisTransferController.onPageLoad(CheckMode)

            case _ =>
              individualRoutes.CheckYourAnswersController.onPageLoad()
          }

        goTo(nextPage, Some(userAnswers))

    case OtherSecuritiesTypePage =>
      userAnswers =>

        val nextPage =
          userAnswers.get(AmountPaidForSecuritiesPage) match {
            case None =>
              individualRoutes.AmountPaidForSecuritiesController.onPageLoad(CheckMode)

            case Some(_) =>
              individualRoutes.CheckYourAnswersController.onPageLoad()
          }

        goTo(nextPage, Some(userAnswers))

    case AmountPaidForSecuritiesPage =>
      userAnswers =>
        val nextPage =
          (userAnswers.get(ConnectedPersonsPage), userAnswers.get(TotalMarketValuePage)) match {
            case (Some(true), None) =>
              individualRoutes.TotalMarketValueController.onPageLoad(CheckMode)

            case _ =>
              individualRoutes.CheckYourAnswersController.onPageLoad()
          }

        goTo(nextPage, Some(userAnswers))

    case ConnectedPersonsPage =>
      userAnswers =>

        val nextPage = userAnswers.get(ConnectedPersonsPage) match {

          case Some(true) if userAnswers.get(DetailsOfThisTransferPage).forall(_.marketValue.isEmpty) =>
            individualRoutes.DetailsOfThisTransferController.onPageLoad(CheckMode)

          case _ =>
            individualRoutes.CheckYourAnswersController.onPageLoad()
        }

        goTo(nextPage, Some(userAnswers))


    case _ => userAnswers => goTo(individualRoutes.CheckYourAnswersController.onPageLoad(), Some(userAnswers))
  }
