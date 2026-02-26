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

import play.api.Logging
import play.api.libs.json.Reads
import play.api.mvc.{Call, Request}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.http.HeaderCarrierConverter
import uk.gov.hmrc.securitiestransferchargefrontend.controllers.routes
import uk.gov.hmrc.securitiestransferchargefrontend.controllers.individuals.{routes => individualRoutes}
import uk.gov.hmrc.securitiestransferchargefrontend.domain.SubmissionId
import uk.gov.hmrc.securitiestransferchargefrontend.models.{Mode, NormalMode, UserAnswers}
import uk.gov.hmrc.securitiestransferchargefrontend.pages.Page
import uk.gov.hmrc.securitiestransferchargefrontend.queries.Gettable
import uk.gov.hmrc.securitiestransferchargefrontend.services.AnswerPersistenceService

import scala.concurrent.{ExecutionContext, Future}

trait Navigator:
  def nextPage(page: Page, mode: Mode, userAnswers: UserAnswers)(implicit request: Request[?]): Future[Call]
  def restore(submissionId: SubmissionId, userId: String)(implicit request: Request[?]): Future[UserAnswers]
  val errorPage: Page => Call

abstract class AbstractNavigator(answerPersistenceService: AnswerPersistenceService)
                                (implicit ex: ExecutionContext) extends Navigator with Logging:

  protected[navigation] val updateUserAnswers: Call => UserAnswers => Future[UserAnswers] = call => ua =>
    val updatedUserAnswers = if (Navigator.errorPages.contains(call)) ua else ua.setNextPage(call)
    Future.successful(updatedUserAnswers)

  protected[navigation] def updateAndPersistUserAnswers(call: Call, ua: UserAnswers)(implicit hc: HeaderCarrier):Future[Call] = for {
    updatedAnswers <- updateUserAnswers(call)(ua)
    _              <- answerPersistenceService.save(updatedAnswers)
  } yield call

  /*
   * Used to navigate when the destination does not depend on the UserAnswers.
   * If UserAnswers are provided, they will be saved.
   */
  protected[navigation] def goTo(success: Call, userAnswers: Option[UserAnswers] = None)(implicit hc: HeaderCarrier): Future[Call] =
    userAnswers
      .fold
        (Future.successful(success))
        (ua => updateAndPersistUserAnswers(success, ua))

  /*
   * Used to navigate when the destination depends on UserAnswers existing for the page,
   * but the value doesn't matter.
   */
  protected[navigation] def dataRequired[A: Reads](page: Page & Gettable[A], userAnswers: UserAnswers, success: Call)(implicit hc: HeaderCarrier): Future[Call] =
    dataDependent(page, userAnswers)(_ => success)

  /*
   * Used to navigate when the destination depends on the value of the UserAnswers for the page,
   */
  protected[navigation] def dataDependent[A: Reads](page: Page & Gettable[A], userAnswers: UserAnswers)(f: A => Call)(implicit hc: HeaderCarrier): Future[Call] =
    userAnswersDependent(userAnswers) { userAnswers =>
      userAnswers
        .get(page)
        .fold(Navigator.defaultPage)(f)
    }

  /*
   * Used to navigate when the destination depends on the value of the UserAnswers
    for a different page than the current one.
   */
  protected[navigation] def userAnswersDependent(userAnswers: UserAnswers)(f: UserAnswers => Call)(implicit hc: HeaderCarrier): Future[Call] = {
    updateAndPersistUserAnswers(f(userAnswers), userAnswers)
  }

  def restore(submissionId: SubmissionId, userId: String)(implicit request: Request[?]): Future[UserAnswers] = {
    implicit val hc: HeaderCarrier = HeaderCarrierConverter.fromRequestAndSession(request, request.session)
    answerPersistenceService.load(submissionId, userId)
  }
  
object Navigator:
  val startPage: Call = individualRoutes.HowToNotifyAboutSecuritiesTransferController.onPageLoad(NormalMode)
  val defaultPage: Call = routes.JourneyRecoveryController.onPageLoad()
  val defaultPageF: Future[Call] = Future.successful(defaultPage)
  val errorPages: Seq[Call] = List(defaultPage)
