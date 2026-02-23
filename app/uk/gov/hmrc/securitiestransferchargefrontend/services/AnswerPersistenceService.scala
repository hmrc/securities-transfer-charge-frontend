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

package uk.gov.hmrc.securitiestransferchargefrontend.services

import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.securitiestransferchargefrontend.clients.SaveAndReturnClient
import uk.gov.hmrc.securitiestransferchargefrontend.domain.SubmissionId
import uk.gov.hmrc.securitiestransferchargefrontend.models.UserAnswers
import uk.gov.hmrc.securitiestransferchargefrontend.repositories.SessionRepository

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

trait AnswerPersistenceService:
  def save(userAnswers: UserAnswers)(implicit hc: HeaderCarrier): Future[Unit]
  def load(submissionId: SubmissionId, userId: String)(implicit hc: HeaderCarrier): Future[UserAnswers]

class AnswerPersistenceServiceImpl @Inject()(sessionRepository: SessionRepository,
                                             saveAndReturnClient: SaveAndReturnClient)
                                            (implicit ec: ExecutionContext) extends AnswerPersistenceService:

  override def save(userAnswers: UserAnswers)(implicit hc: HeaderCarrier): Future[Unit] =
    for {
      _ <- sessionRepository.set(userAnswers)
      _ <- saveAndReturnClient.save(userAnswers)
    } yield ()

  override def load(submissionId: SubmissionId, userId: String)(implicit hc: HeaderCarrier): Future[UserAnswers] =
    for {
      userAnswers <- saveAndReturnClient.retrieve(userId, submissionId)
      _           <- sessionRepository.clear(userId)
      _           <- sessionRepository.set(userAnswers)
    } yield userAnswers
