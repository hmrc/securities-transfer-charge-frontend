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


import org.mongodb.scala.bson.BsonDocument
import org.mongodb.scala.bson.conversions.Bson
import org.mongodb.scala.model.Updates.{combine, set}
import org.mongodb.scala.model.*
import play.api.libs.json.Json
import uk.gov.hmrc.mongo.MongoComponent
import uk.gov.hmrc.mongo.play.json.PlayMongoRepository
import uk.gov.hmrc.securitiestransferchargefrontend.config.FrontendAppConfig
import uk.gov.hmrc.securitiestransferchargefrontend.models.stf.upscan.UpscanCallbackRequest.UploadDetails
import uk.gov.hmrc.securitiestransferchargefrontend.models.stf.upscan.{FileUpload, UpscanDocument, UpscanJourneyStatus}

import java.util.concurrent.TimeUnit
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}


trait UpscanJourneyRepository {

  def insert(journey: UpscanDocument): Future[Unit]

  def markUploadAsSuccessful(reference: String, downloadUrl: String, uploadDetails: UploadDetails): Future[Unit]

  def markUploadAsFailed(reference: String, failureReason: String, message: String): Future[Unit]

  def find(reference:String): Future[Option[FileUpload]]

  def delete(reference:String):Future[Unit]

  def updateStatus(reference: String, status: UpscanJourneyStatus): Future[Unit]

}

@Singleton
class UpscanJourneyRepositoryImpl @Inject()(
                                             mongoComponent: MongoComponent,
                                             appConfig: FrontendAppConfig
                                           )(implicit ec: ExecutionContext)
  extends PlayMongoRepository[UpscanDocument](
    collectionName = "file-uploads",
    mongoComponent = mongoComponent,
    domainFormat = UpscanDocument.format,
    indexes = Seq(
      IndexModel(
        Indexes.ascending("createdAt"),
        IndexOptions()
          .name("createdAtIdx")
          .expireAfter(appConfig.upscanTtl, TimeUnit.HOURS)
      )
    )) with UpscanJourneyRepository {

  
  private def byReference(reference:String): Bson = Filters.equal("_id",reference)

  override def insert(journey: UpscanDocument): Future[Unit] =
    collection.replaceOne(
        byReference(journey.fileUpload.reference),
        journey,
        ReplaceOptions().upsert(true)
      )
      .toFuture()
      .map(_ => ())
  
  override def markUploadAsSuccessful(reference: String, downloadUrl: String, uploadDetails: UploadDetails): Future[Unit] =
    collection.updateOne(
     byReference(reference),
      combine(
        set("fileUpload.status", UpscanJourneyStatus.Ready.toString),
        set("fileUpload.downloadUrl", downloadUrl),
        set("fileUpload.uploadDetails",BsonDocument(Json.toJson(uploadDetails).toString())),
      )
    ).toFuture().map(_ => ())


  override def markUploadAsFailed(reference: String, failureReason: String, message: String): Future[Unit] =
    collection.updateOne(
      byReference(reference),
      combine(
        set("fileUpload.status", UpscanJourneyStatus.Failed.toString),
        set("fileUpload.failureReason", failureReason),
        set("fileUpload.message", message)
      )
    ).toFuture().map(_ => ())
  
  override def find(reference:String): Future[Option[FileUpload]] = collection.find(byReference(reference)).map(_.fileUpload).headOption()

  override def delete(reference: String): Future[Unit] = collection.deleteOne(byReference(reference)).toFuture().map(_=>())

  override def updateStatus(reference: String, status: UpscanJourneyStatus): Future[Unit] =
    collection.updateOne(
      byReference(reference),
      combine(set("fileUpload.status", status.toString)))
      .toFuture().map(_=> ())
}