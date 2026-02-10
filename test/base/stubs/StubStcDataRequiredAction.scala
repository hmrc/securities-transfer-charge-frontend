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

import base.Fixtures.emptyUserAnswers
import play.api.mvc.Result
import uk.gov.hmrc.securitiestransferchargefrontend.controllers.actions.StcDataRequiredAction
import uk.gov.hmrc.securitiestransferchargefrontend.controllers.actions.requests.{StcDataRequest, StcOptionalDataRequest}
import uk.gov.hmrc.securitiestransferchargefrontend.models.UserAnswers

import scala.concurrent.{ExecutionContext, Future}

class StubStcDataRequiredAction(userAnswers: UserAnswers) extends StcDataRequiredAction {
  override protected def executionContext: ExecutionContext = ExecutionContext.global

  override protected def refine[A](request: StcOptionalDataRequest[A]): Future[Either[Result, StcDataRequest[A]]] =
    Future.successful(Right(StcDataRequest(request.request, userAnswers)))
}
object StubStcDataRequiredAction {
  def apply(userAnswers: Option[UserAnswers]): StubStcDataRequiredAction = new StubStcDataRequiredAction(userAnswers.getOrElse(emptyUserAnswers))
}
