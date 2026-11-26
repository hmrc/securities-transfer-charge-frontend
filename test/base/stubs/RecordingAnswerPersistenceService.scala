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

import base.Fixtures.testGroupIdentifier
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.securitiestransferchargefrontend.domain.{SubmissionId, UserId}
import uk.gov.hmrc.securitiestransferchargefrontend.models.JourneyType.SH03
import uk.gov.hmrc.securitiestransferchargefrontend.models.UserAnswers
import uk.gov.hmrc.securitiestransferchargefrontend.services.AnswerPersistenceService

import scala.collection.mutable.ListBuffer
import scala.concurrent.Future

/* Records every UserAnswers passed to save, so tests can check what would be persisted. */
class RecordingAnswerPersistenceService extends AnswerPersistenceService:
  private val buffer = ListBuffer.empty[UserAnswers]

  def saved: Seq[UserAnswers] = buffer.toList

  override def save(userAnswers: UserAnswers)(implicit hc: HeaderCarrier): Future[Unit] = {
    buffer += userAnswers
    Future.successful(())
  }

  override def load(submissionId: SubmissionId, userId: UserId)(implicit hc: HeaderCarrier): Future[UserAnswers] =
    Future.successful(UserAnswers.empty(userId)(testGroupIdentifier)(submissionId)(SH03))
