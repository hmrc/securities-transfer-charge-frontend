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

import org.mongodb.scala.model.{Filters, ReplaceOptions}
import play.api.libs.json.{Format, Json, OFormat}
import uk.gov.hmrc.mongo.MongoComponent
import uk.gov.hmrc.mongo.play.json.PlayMongoRepository
import uk.gov.hmrc.mongo.play.json.formats.MongoJavatimeFormats
import uk.gov.hmrc.securitiestransferchargefrontend.models.Relief

import java.time.Instant
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

/** The reliefs last fetched from the submissions service. There is only ever one of these. */
final case class CachedReliefs(reliefs: Seq[Relief], fetchedAt: Instant, key: String = CachedReliefs.documentKey)

object CachedReliefs:
  val documentKey = "reliefs"

  private given Format[Instant] = MongoJavatimeFormats.instantFormat
  private given OFormat[Relief] = Json.format[Relief]
  given OFormat[CachedReliefs]  = Json.format[CachedReliefs]

trait ReliefsCache:
  def retrieve(): Future[Option[CachedReliefs]]
  def store(reliefs: Seq[Relief], fetchedAt: Instant): Future[Unit]

// There is deliberately no TTL index. How long data stays "fresh" is decided by ReliefsService, and expired data is kept
// so that it can be used as a backup when the submissions service cannot supply the reliefs.
@Singleton
class ReliefsCacheRepository @Inject() (
  mongoComponent: MongoComponent
)(implicit ec: ExecutionContext)
  extends PlayMongoRepository[CachedReliefs](
    collectionName = "reliefs-cache",
    mongoComponent = mongoComponent,
    domainFormat   = CachedReliefs.given_OFormat_CachedReliefs,
    indexes        = Seq.empty
  ) with ReliefsCache {

  private[repositories] def dropCollection(): Future[Unit] =
    collection.drop().toFuture().map(_ => ())

  override def retrieve(): Future[Option[CachedReliefs]] =
    collection.find(Filters.equal("key", CachedReliefs.documentKey)).headOption()

  override def store(reliefs: Seq[Relief], fetchedAt: Instant): Future[Unit] =
    collection
      .replaceOne(
        filter      = Filters.equal("key", CachedReliefs.documentKey),
        replacement = CachedReliefs(reliefs, fetchedAt),
        options     = ReplaceOptions().upsert(true)
      )
      .toFuture()
      .map(_ => ())
}
