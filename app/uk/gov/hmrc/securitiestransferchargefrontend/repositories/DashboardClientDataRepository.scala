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
import org.mongodb.scala.model.{Filters, IndexModel, IndexOptions, Indexes, ReplaceOptions}
import play.api.libs.json.{Json, OFormat}
import uk.gov.hmrc.mongo.MongoComponent
import uk.gov.hmrc.mongo.play.json.PlayMongoRepository
import uk.gov.hmrc.securitiestransferchargefrontend.clients.DashboardClientDataCache
import uk.gov.hmrc.securitiestransferchargefrontend.config.FrontendAppConfig
import uk.gov.hmrc.securitiestransferchargefrontend.models.search.EtmpTransactionSummaryResponse

import java.time.Instant
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

final case class DashboardClientData(
  key: String, 
  value: EtmpTransactionSummaryResponse, 
  createdAt: Instant = Instant.now
)

object DashboardClientData {
  given OFormat[DashboardClientData] = Json.format[DashboardClientData]
}

class DashboardClientDataRepository @Inject() (
  mongoComponent: MongoComponent,
  appConfig: FrontendAppConfig)(
  implicit ec: ExecutionContext)
  extends PlayMongoRepository[DashboardClientData](
    collectionName = "dashboard-client-cache-store",
    mongoComponent = mongoComponent,
    domainFormat = DashboardClientData.given_OFormat_DashboardClientData,
    indexes = Seq(
      IndexModel(
        Indexes.ascending("uploadedAt"),
        IndexOptions()
          .name("dashboard_client_cache_uploadedAt_ttl_idx")
          .expireAfter(appConfig.dashboardClientCacheTtlMins, TimeUnit.MINUTES)
      ),
      IndexModel(
        Indexes.ascending("key"),
        IndexOptions()
          .name("key_idx")
      )
    )
  ) with DashboardClientDataCache {

  private def byKey(key: String): Bson = Filters.equal("key", key)

  private[repositories] def dropCollection(): Future[Unit] = {
    collection
      .drop()
      .toFuture()
  }

  def store(key: String, value: EtmpTransactionSummaryResponse): Future[Unit] =
    collection
      .replaceOne(
        filter      = byKey(key),
        replacement = DashboardClientData(key, value),
        options     = ReplaceOptions().upsert(true))
      .toFuture()
      .map(_ => ())

  def retrieve(key: String): Future[Option[EtmpTransactionSummaryResponse]] =
    collection
      .find(byKey(key))
      .map(_.value)
      .headOption()
}
