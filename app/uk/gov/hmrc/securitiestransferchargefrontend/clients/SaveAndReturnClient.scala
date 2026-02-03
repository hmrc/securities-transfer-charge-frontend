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

package uk.gov.hmrc.securitiestransferchargefrontend.clients

import uk.gov.hmrc.securitiestransferchargefrontend.models.UserAnswers
import uk.gov.hmrc.securitiestransferchargefrontend.domain.SubmissionId

import javax.inject.Inject
import scala.concurrent.Future

trait SaveAndReturnClient:
  def save(userAnswers: UserAnswers): Future[Unit]
  def retrieve(userId: String, submissionId: SubmissionId): Future[UserAnswers]
  def list(userId: String): Future[List[SubmissionId]]


class SaveAndReturnClientImpl @Inject() extends SaveAndReturnClient {

  private val stubUserId = "bob123"
  private val stubSubmissionId: SubmissionId = SubmissionId.apply("STC-000000001")
  private val stubUserAnswers: UserAnswers = UserAnswers(stubUserId, stubSubmissionId)

  override def save(userAnswers: UserAnswers): Future[Unit] = Future.successful(())
  override def retrieve(userId: String, submissionId: SubmissionId): Future[UserAnswers] = Future.successful(stubUserAnswers)
  override def list(userId: String): Future[List[SubmissionId]] = Future.successful(List(stubSubmissionId))
}
