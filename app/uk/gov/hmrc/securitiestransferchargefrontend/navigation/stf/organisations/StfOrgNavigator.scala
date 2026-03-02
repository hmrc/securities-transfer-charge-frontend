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

import com.google.inject.Singleton
import play.api.mvc.{Call, Request}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.http.HeaderCarrierConverter
import uk.gov.hmrc.securitiestransferchargefrontend.controllers.routes
import uk.gov.hmrc.securitiestransferchargefrontend.controllers.stf.organisations.routes as orgRoutes
import uk.gov.hmrc.securitiestransferchargefrontend.domain.SubmissionId
import uk.gov.hmrc.securitiestransferchargefrontend.models.UserAnswers
import uk.gov.hmrc.securitiestransferchargefrontend.navigation.stf.organisations.{BackwardsRoutes, ForwardRoutes}
import uk.gov.hmrc.securitiestransferchargefrontend.navigation.{AbstractModeNavigator, PersistentNavigator}
import uk.gov.hmrc.securitiestransferchargefrontend.pages.*
import uk.gov.hmrc.securitiestransferchargefrontend.services.AnswerPersistenceService

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class StfOrgNavigator @Inject()(answerPersistenceService: AnswerPersistenceService)
                               (implicit ec: ExecutionContext) extends AbstractModeNavigator with PersistentNavigator:

  val forwardRoutes: ForwardRoutes = new ForwardRoutes(answerPersistenceService)

  override def forwardRoutes(page: Page)(implicit hc: HeaderCarrier): UserAnswers => Future[Call] = {
    forwardRoutes.forwardRoutes(page)(hc)
  }

  override def predecessorRoutes(page: Page): UserAnswers => Call =
    BackwardsRoutes.predecessorRoutes(page)

  def errorPage(forPage: Page): Call = forPage match {
    case _ => StfOrgNavigator.defaultPage
  }

  val checkRouteMap: Page => UserAnswers => Call = _ => _ => routes.CheckYourAnswersController.onPageLoad()

  def restore(submissionId: SubmissionId, userId: String)(implicit request: Request[?]): Future[UserAnswers] = {
    implicit val hc: HeaderCarrier = HeaderCarrierConverter.fromRequestAndSession(request, request.session)
    answerPersistenceService.load(submissionId, userId)
  }

object StfOrgNavigator:
  val startPage: Call = orgRoutes.ConfirmAddressController.onPageLoad()
  val defaultPage: Call = routes.JourneyRecoveryController.onPageLoad()
  val defaultPageF: Future[Call] = Future.successful(defaultPage)
  val errorPages: Seq[Call] = List(defaultPage)
