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

import base.Fixtures
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.securitiestransferchargefrontend.clients.SaveAndReturnClient
import uk.gov.hmrc.securitiestransferchargefrontend.domain.{GroupIdentifier, SubmissionId, UserId}
import uk.gov.hmrc.securitiestransferchargefrontend.models.JourneyType.STF
import uk.gov.hmrc.securitiestransferchargefrontend.models.{JourneyType, UserAnswers, UserAnswersSummary}

import java.time.Instant
import scala.concurrent.Future

class FakeSaveAndReturnClient extends SaveAndReturnClient:
  private val stubUserId: UserId = Fixtures.testInternalId
  private val stubSummary: UserAnswersSummary = UserAnswersSummary(Fixtures.testSubmissionId, JourneyType.STF, Instant.now())
  private val stubGroupIdentifier: GroupIdentifier = Fixtures.testGroupIdentifier
  private val stubUserAnswers: UserAnswers = UserAnswers(stubUserId, stubGroupIdentifier, Fixtures.testSubmissionId, STF)

  override def save(userAnswers: UserAnswers)(implicit hc: HeaderCarrier): Future[Unit] = Future.successful(())

  override def retrieve(submissionId: SubmissionId)(implicit hc: HeaderCarrier): Future[UserAnswers] = Future.successful(stubUserAnswers)

  override def listByUser(userId: UserId)(implicit hc: HeaderCarrier): Future[List[UserAnswersSummary]] = Future.successful(List(stubSummary))

  override def listByGroup(groupIdentifier: GroupIdentifier)(implicit hc: HeaderCarrier): Future[List[UserAnswersSummary]] = Future.successful(List(stubSummary))

  override def deleteDraft(submissionId: SubmissionId)(implicit hc: HeaderCarrier): Future[Unit] = Future.successful(())

object FakeSaveAndReturnClient:
  def apply(): SaveAndReturnClient = new FakeSaveAndReturnClient

