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

import uk.gov.hmrc.securitiestransferchargefrontend.domain.{GroupIdentifier, SubmissionId}
import uk.gov.hmrc.securitiestransferchargefrontend.models.submission.Status
import uk.gov.hmrc.securitiestransferchargefrontend.models.submission.Status.*
import uk.gov.hmrc.securitiestransferchargefrontend.repositories.ProcessingStatusRepository

import javax.inject.Inject
import scala.concurrent.Future

type RowNumber = Int
type FailureReason = String
type RowToFailureMap = Map[RowNumber, FailureReason]

trait BulkStatusService {
  def getStatus(groupId: GroupIdentifier, submissionId: SubmissionId): Future[Option[Status]]
  def markAsProcessing(groupId: GroupIdentifier, submissionId: SubmissionId): Future[Unit]
  def markAsSuccessful(submissionId: SubmissionId): Future[Unit]
  def markAsFailed(groupId: GroupIdentifier, submissionId: SubmissionId): Future[Unit]
  def markAsPartiallyFailed(groupId: GroupIdentifier, submissionId: SubmissionId, failures: RowToFailureMap): Future[Unit]
}

final class BulkStatusServiceImpl @Inject() 
  (processingStatusRepository: ProcessingStatusRepository) extends BulkStatusService {

  override def getStatus(groupId: GroupIdentifier, submissionId: SubmissionId): Future[Option[Status]] =
    processingStatusRepository.findBySubmissionId(groupId, submissionId)

  override def markAsProcessing(groupId: GroupIdentifier, submissionId: SubmissionId): Future[Unit] =
    processingStatusRepository.insert(groupId, submissionId, Processing)

  override def markAsSuccessful(submissionId: SubmissionId): Future[Unit] =
    processingStatusRepository.delete(submissionId)

  override def markAsFailed(groupId: GroupIdentifier, submissionId: SubmissionId): Future[Unit] =
    processingStatusRepository.update(groupId, submissionId, Failed)

  override def markAsPartiallyFailed(groupId: GroupIdentifier, submissionId: SubmissionId, failures: RowToFailureMap): Future[Unit] =
    processingStatusRepository.update(groupId, submissionId, PartiallyFailed(failures))
}

