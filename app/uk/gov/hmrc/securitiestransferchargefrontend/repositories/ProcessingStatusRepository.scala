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

package uk.gov.hmrc.securitiestransferchargefrontend.repositories

import org.mongodb.scala.model.*
import uk.gov.hmrc.mongo.MongoComponent
import uk.gov.hmrc.mongo.play.json.PlayMongoRepository
import uk.gov.hmrc.securitiestransferchargefrontend.config.FrontendAppConfig
import uk.gov.hmrc.securitiestransferchargefrontend.domain.{GroupIdentifier, SubmissionId}
import uk.gov.hmrc.securitiestransferchargefrontend.models.submission.{ProcessingStatus, Status}

import java.util.concurrent.TimeUnit
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

trait ProcessingStatusRepository {
  def insert(groupId: GroupIdentifier,submissionId: SubmissionId, status: Status): Future[Unit]
  def update(groupId: GroupIdentifier,submissionId: SubmissionId, status: Status): Future[Unit]
  def delete(submissionId: SubmissionId): Future[Unit]
  def findBySubmissionIds(submissionIds: Seq[String]): Future[Set[SubmissionId]]

  def findByGroup(group: GroupIdentifier): Future[Map[SubmissionId, Status]]
}

@Singleton
class ProcessingStatusRepositoryImpl @Inject()(mongoComponent: MongoComponent, appConfig: FrontendAppConfig)(implicit ec: ExecutionContext)
  extends PlayMongoRepository[ProcessingStatus](
    collectionName = "processing-status",
    mongoComponent = mongoComponent,
    domainFormat = ProcessingStatus.format,
    indexes = Seq(
      IndexModel(
        Indexes.ascending("uploadedAt"),
        IndexOptions()
          .name("processing_status_uploadedAt_ttl_idx")
          .expireAfter(appConfig.processingStatusTtl, TimeUnit.DAYS)
      ),
      IndexModel(
        Indexes.ascending("submissionId"),
        IndexOptions().name("processing_status_submissionId_idx")
      )
    )
  )
    with ProcessingStatusRepository {

  private def bySubmissionId(submissionId: SubmissionId) = Filters.equal("submissionId", submissionId.value)

  override def insert(groupId: GroupIdentifier,submissionId: SubmissionId, status: Status): Future[Unit] = {
    val processingStatus = ProcessingStatus(groupId= groupId,submissionId = submissionId, status = status)

    collection
      .insertOne(processingStatus)
      .toFuture()
      .map(_ => ())
  }

  override def update(groupId: GroupIdentifier,submissionId: SubmissionId, status: Status): Future[Unit] =
    collection
      .updateOne(bySubmissionId(submissionId), Updates.set("status", status.toString))
      .toFuture()
      .map(_ => ())

  override def delete(submissionId: SubmissionId): Future[Unit] =
    collection
      .deleteOne(bySubmissionId(submissionId))
      .toFuture()
      .map(_ => ())

  override def findBySubmissionIds(submissionIds: Seq[String]): Future[Set[SubmissionId]] =
    if (submissionIds.isEmpty) {
      Future.successful(Set.empty)
    } else {
      collection
        .find(Filters.in("submissionId", submissionIds *))
        .toFuture()
        .map(_.map(_.submissionId).toSet)
    }

  override def findByGroup(group: GroupIdentifier): Future[Map[SubmissionId, Status]] = ???  

  def dropCollection(): Future[Unit] =
    collection
      .drop()
      .toFuture()
      .map(_ => ())  
}