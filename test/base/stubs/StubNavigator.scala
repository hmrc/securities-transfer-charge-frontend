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
import uk.gov.hmrc.securitiestransferchargefrontend.domain.SubmissionId
import uk.gov.hmrc.securitiestransferchargefrontend.models.{Mode, UserAnswers}
import uk.gov.hmrc.securitiestransferchargefrontend.navigation.Navigator
import uk.gov.hmrc.securitiestransferchargefrontend.pages.Page

import scala.concurrent.Future

class StubNavigator(desiredCall: Call) extends Navigator {

  override def nextPage(page: Page, mode: Mode, userAnswers: UserAnswers)(implicit request: Request[_]): Future[Call] =
    Future.successful(desiredCall)


  val errorPage: Page => Call = _ => desiredCall

  override def restore(submissionId: SubmissionId, userId: String)(implicit request: Request[_]): Future[UserAnswers] =
    Future.successful(UserAnswers.empty(userId)(submissionId))
}
