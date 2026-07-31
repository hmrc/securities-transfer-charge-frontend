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

package base.stubs

import play.api.mvc.Call
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.securitiestransferchargefrontend.models.{Mode, UserAnswers}
import uk.gov.hmrc.securitiestransferchargefrontend.navigation.Navigator
import uk.gov.hmrc.securitiestransferchargefrontend.pages.Page
import uk.gov.hmrc.securitiestransferchargefrontend.pages.*

import scala.concurrent.{ExecutionContext, Future}

class StubNavigator(desiredCall: Call) extends Navigator {

  override def nextPage(page: Page, mode: Mode, userAnswers: UserAnswers, isReturn: Boolean = false): Page = JourneyRecoveryPage

  override def errorPage(forPage: Page): Call = desiredCall

  override def previousPage(page: Page, mode: Mode, userAnswers: UserAnswers): Page = JourneyRecoveryPage

  override def previousPage(page: Page, mode: Mode, userAnswers: Option[UserAnswers]): Page = JourneyRecoveryPage

  override protected def pageToCall(page: Page): Call = desiredCall

  override def nextPageCall(page: Page, mode: Mode, userAnswers: UserAnswers, isReturn: Boolean = false)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Call] = {
    Future.successful(desiredCall)
  }
}
