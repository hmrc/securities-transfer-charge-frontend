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

import play.api.mvc.{Call, Request}
import uk.gov.hmrc.securitiestransferchargefrontend.models.{Mode, UserAnswers}
import uk.gov.hmrc.securitiestransferchargefrontend.navigation.{Navigator, PersistentNavigator, UserAnswersValidator}
import uk.gov.hmrc.securitiestransferchargefrontend.pages.Page
import uk.gov.hmrc.securitiestransferchargefrontend.domain.{SubmissionId, UserId}

import scala.concurrent.Future

class StubNavigator(desiredCall: Call)(implicit ec: scala.concurrent.ExecutionContext) extends Navigator {

  override def nextPage(page: Page, mode: Mode, userAnswers: UserAnswers, isReturn: Boolean = false)(implicit request: Request[_]): Future[Call] = {
    Future.successful(desiredCall)
  }

  override def errorPage(forPage: Page): Call = desiredCall

  override def previousPage(page: Page, mode: Mode, userAnswers: UserAnswers): Call = desiredCall

  override def previousPage(page: Page, mode: Mode, userAnswers: Option[UserAnswers]): Call = desiredCall

  override val userAnswersValidator: UserAnswersValidator = new StubUserAnswersValidator(this)
}

class StubPersistentNavigator(desiredCall: Call, completeAnswers: UserAnswers)(implicit ec: scala.concurrent.ExecutionContext) 
  extends StubNavigator(desiredCall) with PersistentNavigator {
  
  override def restore(submissionId: SubmissionId, userId: UserId)(implicit request: Request[?]): Future[UserAnswers] = {
    Future.successful(completeAnswers)
  }

  override val userAnswersValidator: UserAnswersValidator = new CyaSuccessValidator(this)
}
