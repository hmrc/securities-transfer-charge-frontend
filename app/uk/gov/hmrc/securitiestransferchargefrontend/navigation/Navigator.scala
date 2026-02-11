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

import play.api.libs.json.Reads
import play.api.mvc.{Call, Request}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.securitiestransferchargefrontend.clients.SaveAndReturnClient
import uk.gov.hmrc.securitiestransferchargefrontend.controllers.routes
import uk.gov.hmrc.securitiestransferchargefrontend.models.{Mode, UserAnswers}
import uk.gov.hmrc.securitiestransferchargefrontend.pages.Page
import uk.gov.hmrc.securitiestransferchargefrontend.queries.Gettable
import uk.gov.hmrc.securitiestransferchargefrontend.repositories.SessionRepository

import scala.concurrent.{ExecutionContext, Future}

trait Navigator:
  def nextPage(page: Page, mode: Mode, userAnswers: UserAnswers)(implicit request: Request[?]): Future[Call]
  val errorPage: Page => Call

abstract class AbstractNavigator(sessionRepository: SessionRepository,
                                 saveAndReturnClient: SaveAndReturnClient)
                                (implicit ec: ExecutionContext) extends Navigator:

  protected[navigation] val defaultPage: Call = routes.JourneyRecoveryController.onPageLoad()
  protected[navigation] val defaultPageF: Future[Call] = Future.successful(routes.JourneyRecoveryController.onPageLoad())

  private def persistUserAnswers(userAnswers: UserAnswers)(implicit hc: HeaderCarrier): Future[Unit] = for {
    _ <- sessionRepository.set(userAnswers)
    _ <- saveAndReturnClient.save(userAnswers)
  } yield ()

  /*
   * Used to navigate when the destination does not depend on the UserAnswers.
   * If UserAnswers are provided, they will be saved.
   */
  protected[navigation] def goTo(success: Call, userAnswers: Option[UserAnswers] = None)(implicit hc: HeaderCarrier): Future[Call] =
    userAnswers
      .fold
        (Future.successful(success))
        (ua => persistUserAnswers(ua).map(_ => success))

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
        .fold(defaultPage)(f)
    }

  /*
   * Used to navigate when the destination depends on the value of the UserAnswers
    for a different page than the current one.
   */
  protected[navigation] def userAnswersDependent(userAnswers: UserAnswers)(f: UserAnswers => Call)(implicit hc: HeaderCarrier): Future[Call] =
    persistUserAnswers(userAnswers)
      .map(_ => f(userAnswers))
