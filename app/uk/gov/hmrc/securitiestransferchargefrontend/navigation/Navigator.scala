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
import uk.gov.hmrc.securitiestransferchargefrontend.models.{CheckMode, Mode, NormalMode, UserAnswers}
import uk.gov.hmrc.securitiestransferchargefrontend.pages.Page

import scala.concurrent.Future

trait Navigator:
  def nextPage(page: Page, mode: Mode, userAnswers: UserAnswers)(implicit request: Request[?]): Future[Call]
  def errorPage(forPage: Page): Call

abstract class AbstractModeNavigator extends Navigator:

  def normalRoutes(page: Page)(implicit hc: HeaderCarrier): UserAnswers => Future[Call]
  
  val checkRouteMap: Page => UserAnswers => Call

  def nextPage(page: Page, mode: Mode, userAnswers: UserAnswers)(implicit request: Request[?]): Future[Call] = {
    implicit lazy val hc: HeaderCarrier = HeaderCarrierConverter.fromRequestAndSession(request, request.session)
    mode match {
      case NormalMode => normalRoutes(page)(hc)(userAnswers)
      case CheckMode  => Future.successful(checkRouteMap(page)(userAnswers))
    }
  }
