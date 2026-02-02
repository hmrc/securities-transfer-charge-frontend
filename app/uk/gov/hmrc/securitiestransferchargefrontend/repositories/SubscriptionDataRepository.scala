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
import play.api.Logging
import play.api.libs.json.Format
import uk.gov.hmrc.mongo.MongoComponent
import uk.gov.hmrc.mongo.play.json.PlayMongoRepository
import uk.gov.hmrc.mongo.play.json.formats.MongoJavatimeFormats
import uk.gov.hmrc.securitiestransferchargefrontend.config.FrontendAppConfig

import java.time.{Clock, Instant}
import java.util.concurrent.TimeUnit
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}
import play.api.libs.json.Json
import uk.gov.hmrc.securitiestransferchargefrontend.clients.registration.Subscription

case class SubscriptionData(
                            stcId:String,
                            subscriptionDetails: Subscription,
                            lastUpdated: Instant = Instant.now,
                           )

object SubscriptionData {
  implicit val format: Format[SubscriptionData] = Json.format[SubscriptionData]
}

trait SubscriptionDataRepository {
  def getSubscriptionData(id: String): Future[Option[SubscriptionData]]
  def saveSubscriptionData(stcId: String, subscriptionDetails: Subscription): Future[Unit]
  def clear(id: String): Future[Unit]
}


@Singleton
class SubscriptionDataRepositoryImpl @Inject()(mongoComponent: MongoComponent,
                                               appConfig: FrontendAppConfig,
                                               clock: Clock
                                              )(implicit ec: ExecutionContext)
  extends PlayMongoRepository[SubscriptionData](
    collectionName = "subscription-data",
    mongoComponent = mongoComponent,
    domainFormat   = SubscriptionData.format,
    indexes        = Seq(
      IndexModel(
        Indexes.ascending("lastUpdated"),
        IndexOptions()
          .name("lastUpdatedIdx")
          .expireAfter(appConfig.cacheTtl, TimeUnit.SECONDS),
      )
    )
  ) with SubscriptionDataRepository with Logging {

  implicit val instantFormat: Format[Instant] = MongoJavatimeFormats.instantFormat

  
  private def byId(id: String): Bson = Filters.equal("_id", id)


  override def getSubscriptionData(stcId: String): Future[Option[SubscriptionData]] =
    collection
      .find(byId(stcId))
      .headOption()

  override def saveSubscriptionData(
                                     stcId: String,
                                     subscriptionDetails: Subscription
                                   ): Future[Unit] = {

    val data = SubscriptionData(
      stcId = stcId,
      subscriptionDetails = subscriptionDetails,
      lastUpdated = Instant.now(clock)
    )
    
    collection
      .replaceOne(
        filter = byId(stcId),
        replacement = data,
        options = ReplaceOptions().upsert(true)
      )
      .toFuture()
      .map(_ => ())
  }


  override def clear(id: String): Future[Unit] =
    collection
      .deleteOne(byId(id))
      .toFuture()
      .map(_ => ())
}
