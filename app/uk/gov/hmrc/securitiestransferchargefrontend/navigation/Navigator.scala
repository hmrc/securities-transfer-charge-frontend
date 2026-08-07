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
import uk.gov.hmrc.securitiestransferchargefrontend.models.{Mode, UserAnswers}
import uk.gov.hmrc.securitiestransferchargefrontend.pages.Page

import scala.concurrent.{ExecutionContext, Future}

trait Navigator:
  def nextPage(page: Page, mode: Mode, userAnswers: UserAnswers, isReturn: Boolean = false)(implicit request: Request[?]): Future[Call]
  def previousPage(page: Page, mode: Mode, userAnswers: UserAnswers): Call
  def previousPage(page: Page, mode: Mode, userAnswers: Option[UserAnswers]): Call
  def errorPage(forPage: Page): Call
  val userAnswersValidator: UserAnswersValidator


abstract class AbstractModeNavigator(implicit ex: ExecutionContext) extends Navigator:

  def forwardRoutes(page: Page, mode: Mode)(implicit hc: HeaderCarrier): UserAnswers => Future[Call]
  def predecessorRoutes(page: Page, mode: Mode): Option[UserAnswers] => Call

  lazy val dashboardPage: Call

  def nextPage(page: Page, mode: Mode, userAnswers: UserAnswers, isReturn: Boolean)(implicit request: Request[?]): Future[Call] = {
    implicit lazy val hc: HeaderCarrier = HeaderCarrierConverter.fromRequestAndSession(request, request.session)
    val maybeRedirectToDashboard: Call => Call = if (isReturn) _ => dashboardPage else identity
    forwardRoutes(page, mode)(hc)(userAnswers).map(maybeRedirectToDashboard)
  }

  override def previousPage(page: Page, mode: Mode, userAnswers: UserAnswers): Call =
    previousPage(page, mode, Some(userAnswers))

  override def previousPage(page: Page, mode: Mode, userAnswers: Option[UserAnswers]): Call =
    predecessorRoutes(page, mode)(userAnswers)
