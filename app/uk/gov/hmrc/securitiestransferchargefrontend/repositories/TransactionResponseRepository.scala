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

import org.mongodb.scala.bson.conversions.Bson
import org.mongodb.scala.model.*
import play.api.libs.json.{Format, Json, OFormat}
import uk.gov.hmrc.mongo.MongoComponent
import uk.gov.hmrc.mongo.play.json.PlayMongoRepository
import uk.gov.hmrc.mongo.play.json.formats.MongoJavatimeFormats
import uk.gov.hmrc.securitiestransferchargefrontend.config.FrontendAppConfig
import uk.gov.hmrc.securitiestransferchargefrontend.domain.SubmissionId
import uk.gov.hmrc.securitiestransferchargefrontend.domain.SubmissionId.{submissionIdReads, submissionIdWrites}
import uk.gov.hmrc.securitiestransferchargefrontend.services.SubmissionCreateResponseSuccess

import java.time.{Clock, Instant}
import java.util.concurrent.TimeUnit
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

case class TransactionResponseData(
                                    submissionId: SubmissionId,
                                    responseDetails: SubmissionCreateResponseSuccess,
                                    lastUpdated: Instant = Instant.now
                                  )

object TransactionResponseData {

  implicit val submissionCreateResponseSuccessFormat: OFormat[SubmissionCreateResponseSuccess] = Json.format[SubmissionCreateResponseSuccess]

  implicit val format: OFormat[TransactionResponseData] = Json.format[TransactionResponseData]
}

trait TransactionResponseRepository:
  def store(key: SubmissionId, value: SubmissionCreateResponseSuccess): Future[Unit]

  def retrieve(key: SubmissionId): Future[SubmissionCreateResponseSuccess]

@Singleton
final class TransactionResponseRepositoryImpl @Inject()(
                                                         mongoComponent: MongoComponent,
                                                         appConfig: FrontendAppConfig,
                                                         clock: Clock
                                                       )(implicit ec: ExecutionContext)
  extends PlayMongoRepository[TransactionResponseData](
    collectionName = "transaction-response-data",
    mongoComponent = mongoComponent,
    domainFormat = TransactionResponseData.format,
    indexes = Seq(
      IndexModel(
        Indexes.ascending("lastUpdated"),
        IndexOptions()
          .name("lastUpdatedIdx")
          .expireAfter(appConfig.cacheTtl, TimeUnit.SECONDS)
      )
    )
  ) with TransactionResponseRepository {

  implicit val instantFormat: Format[Instant] = MongoJavatimeFormats.instantFormat

  private def byId(submissionId: SubmissionId): Bson = Filters.equal("_id", submissionId)

  override def store(key: SubmissionId, value: SubmissionCreateResponseSuccess): Future[Unit] = {
    val data = TransactionResponseData(
      submissionId = key,
      responseDetails = value,
      lastUpdated = Instant.now(clock)
    )

    collection
      .replaceOne(
        filter = byId(key),
        replacement = data,
        options = ReplaceOptions().upsert(true)
      )
      .toFuture()
      .map(_ => ())
  }

  override def retrieve(key: SubmissionId): Future[SubmissionCreateResponseSuccess] =
    collection
      .find(byId(key))
      .headOption()
      .map {
        case Some(data) => data.responseDetails
        case None => throw new NoSuchElementException(s"No transaction response found for submission ID: $key")
      }
}