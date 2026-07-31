/*
 * Copyright 2024 HM Revenue & Customs
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
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.securitiestransferchargefrontend.models.{Mode, UserAnswers}
import uk.gov.hmrc.securitiestransferchargefrontend.pages.Page

import scala.concurrent.{ExecutionContext, Future}

trait Navigator:
  def nextPage(page: Page, mode: Mode, userAnswers: UserAnswers, isReturn: Boolean = false): Page
  def previousPage(page: Page, mode: Mode, userAnswers: UserAnswers): Page
  def previousPage(page: Page, mode: Mode, userAnswers: Option[UserAnswers]): Page
  def errorPage(forPage: Page): Call
  
  def nextPageCall(page: Page, mode: Mode, userAnswers: UserAnswers, isReturn: Boolean = false)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Call]
  
  def previousPageCall(page: Page, mode: Mode, userAnswers: UserAnswers): Call = {
    val previousPg = previousPage(page, mode, userAnswers)
    pageToCall(previousPg)
  }
  
  def previousPageCall(page: Page, mode: Mode, userAnswers: Option[UserAnswers]): Call = {
    val previousPg = previousPage(page, mode, userAnswers)
    pageToCall(previousPg)
  }
  
  protected def pageToCall(page: Page): Call

abstract class AbstractModeNavigator extends Navigator:

  def forwardRoutes(page: Page)(implicit hc: HeaderCarrier, ec: ExecutionContext): UserAnswers => Future[Call]
  def predecessorRoutes(page: Page): Option[UserAnswers] => Call
  lazy val dashboardPage: Call
  val checkRouteMap: Page => UserAnswers => Call

  override def previousPage(page: Page, mode: Mode, userAnswers: UserAnswers): Page =
    previousPage(page, mode, Some(userAnswers))

  override def previousPage(page: Page, mode: Mode, userAnswers: Option[UserAnswers]): Page = predecessorRoutesPage(page, userAnswers)
    
  protected def predecessorRoutesPage(page: Page, userAnswers: Option[UserAnswers]): Page