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

package clients

import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.securitiestransferchargefrontend.clients.SaveAndReturnClient
import uk.gov.hmrc.securitiestransferchargefrontend.domain.{GroupIdentifier, SubmissionId, UserId}
import uk.gov.hmrc.securitiestransferchargefrontend.models.UserAnswers

import scala.concurrent.Future

class FakeSaveAndReturnClient extends SaveAndReturnClient:
  private val stubUserId: UserId = UserId("bob123")
  private val stubSubmissionId: SubmissionId = SubmissionId.apply("STC-000000001")
  private val stubGroupIdentifier: GroupIdentifier = GroupIdentifier.apply("group-001")
  private val stubUserAnswers: UserAnswers = UserAnswers(stubUserId, stubGroupIdentifier, stubSubmissionId)

  override def save(userAnswers: UserAnswers)(implicit hc: HeaderCarrier): Future[Unit] = Future.successful(())

  override def retrieve(submissionId: SubmissionId)(implicit hc: HeaderCarrier): Future[UserAnswers] = Future.successful(stubUserAnswers)

  override def listByUser(userId: UserId)(implicit hc: HeaderCarrier): Future[List[SubmissionId]] = Future.successful(List(stubSubmissionId))

  override def listByGroup(groupIdentifier: GroupIdentifier)(implicit hc: HeaderCarrier): Future[List[SubmissionId]] = Future.successful(List(stubSubmissionId)) 
object FakeSaveAndReturnClient:
  def apply(): SaveAndReturnClient = new FakeSaveAndReturnClient

