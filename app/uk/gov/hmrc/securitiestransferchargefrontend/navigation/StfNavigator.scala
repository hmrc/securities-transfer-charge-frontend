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

import play.api.mvc.Call
import uk.gov.hmrc.securitiestransferchargefrontend.clients.SaveAndReturnClient
import uk.gov.hmrc.securitiestransferchargefrontend.controllers.routes
import uk.gov.hmrc.securitiestransferchargefrontend.models.{CheckMode, Mode, NormalMode, UserAnswers}
import uk.gov.hmrc.securitiestransferchargefrontend.pages.*
import uk.gov.hmrc.securitiestransferchargefrontend.queries.Gettable
import uk.gov.hmrc.securitiestransferchargefrontend.repositories.SessionRepository

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class StfNavigator @Inject()(sessionRepository: SessionRepository,
                             saveAndReturnClient: SaveAndReturnClient)
                            (implicit ec: ExecutionContext) extends AbstractNavigator(sessionRepository, saveAndReturnClient) {

  private val normalRoutes: Page => UserAnswers => Future[Call] = {

    case SubmissionsDashboardPage => userAnswers => goTo(routes.HowToNotifyAboutSecuritiesTransferController.onPageLoad(NormalMode), Some(userAnswers))
    case HowToNotifyAboutSecuritiesTransferPage => userAnswers => goTo(routes.JourneyRecoveryController.onPageLoad(), Some(userAnswers))
    
    case _ => _ => defaultPage  
  }

  private val checkRouteMap: Page => UserAnswers => Call = (_ => _ => routes.CheckYourAnswersController.onPageLoad())
  
  def nextPage(page: Page, mode: Mode, userAnswers: UserAnswers): Future[Call] = {
    mode match {
      case NormalMode => normalRoutes(page)(userAnswers)
      case CheckMode => Future.successful(checkRouteMap(page)(userAnswers))
    }
  }

  val errorPage: Page => Call = {
    case _: Gettable[?] => ???
    case _ => routes.JourneyRecoveryController.onPageLoad()
  }
}
