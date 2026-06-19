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

package uk.gov.hmrc.securitiestransferchargefrontend.navigation.sh03.agents.single

import play.api.mvc.Call
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.securitiestransferchargefrontend.controllers.routes
import uk.gov.hmrc.securitiestransferchargefrontend.models.{NormalMode, UserAnswers}
import uk.gov.hmrc.securitiestransferchargefrontend.models.sh03.shared.ReasonForPurchase
import uk.gov.hmrc.securitiestransferchargefrontend.controllers.sh03.agents.single.routes as sh03AgentSingleRoutes
import uk.gov.hmrc.securitiestransferchargefrontend.navigation.PersistentNavigationHelper
import uk.gov.hmrc.securitiestransferchargefrontend.pages.Page
import uk.gov.hmrc.securitiestransferchargefrontend.pages.sh03.shared.ConnectedPersonsPage
import uk.gov.hmrc.securitiestransferchargefrontend.pages.sh03.single.{ReasonForPurchasePage, TreasurySharesPage}
import uk.gov.hmrc.securitiestransferchargefrontend.services.AnswerPersistenceService

import scala.concurrent.{ExecutionContext, Future}

class ForwardRoutes(answerPersistenceService: AnswerPersistenceService,
                    defaultPage: Call,
                    errorPages: Seq[Call])
                   (implicit ec: ExecutionContext):

  val helper = new PersistentNavigationHelper(answerPersistenceService, defaultPage, errorPages)

  import helper.*


  def forwardRoutes(page: Page)(implicit hc: HeaderCarrier): UserAnswers => Future[Call] = page match {

    case ReasonForPurchasePage => userAnswers => dataDependent(ReasonForPurchasePage, userAnswers) {
      case ReasonForPurchase.ForCancellation  => sh03AgentSingleRoutes.TreasurySharesController.onPageLoad(NormalMode)
      case ReasonForPurchase.ToPlaceIntoTreasury => sh03AgentSingleRoutes.ConnectedPersonsController.onPageLoad(NormalMode)
    }
    case TreasurySharesPage => userAnswers => dataRequired(TreasurySharesPage, userAnswers,sh03AgentSingleRoutes.ConnectedPersonsController.onPageLoad(NormalMode))
    case ConnectedPersonsPage  => userAnswers => goTo(routes.JourneyRecoveryController.onPageLoad(),Some(userAnswers))

    case _ => _ => Future.successful(defaultPage)
  }
