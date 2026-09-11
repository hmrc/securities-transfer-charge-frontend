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
import play.twirl.api.HtmlFormat
import uk.gov.hmrc.mongo.MongoComponent
import uk.gov.hmrc.mongo.play.json.PlayMongoRepository
import uk.gov.hmrc.securitiestransferchargefrontend.config.FrontendAppConfig
import uk.gov.hmrc.securitiestransferchargefrontend.domain.SubmissionId

import java.time.Instant
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

final case class CyaHtmlData(submissionId: SubmissionId, html: HtmlFormat.Appendable, uploadedAt: Instant = Instant.now())

trait CyaHtmlRepository:
  def store(data: CyaHtmlData): Future[Unit]
  def retrieve(key: SubmissionId): Future[Option[CyaHtmlData]]

final class CyaHtmlRepositoryImpl @Inject() (
  mongoComponent: MongoComponent,
  appConfig: FrontendAppConfig)(
  implicit ec: ExecutionContext)
  extends PlayMongoRepository[CyaHtmlData](
    collectionName = "cya-html-store",
    mongoComponent = mongoComponent,
    domainFormat = CyaHtmlData.given_OFormat_CyaHtmlData,
    indexes = Seq(
      IndexModel(
        Indexes.ascending("uploadedAt"),
        IndexOptions()
          .name("cyaHtml_uploadedAt_ttl_idx")
          .expireAfter(appConfig.cyaHtmlTtl, TimeUnit.DAYS)
      ),
      IndexModel(
        Indexes.ascending("submissionId"),
        IndexOptions()
          .name("submissionId_idx")
      )
    )
  ) with CyaHtmlRepository {

  private def bySubmissionId(submissionId: SubmissionId): Bson = Filters.equal("submissionId", submissionId)
  private val options = new ReplaceOptions().upsert(true)

  def store(data: CyaHtmlData): Future[Unit] =
    collection
      .replaceOne(bySubmissionId(data.submissionId), data, options)
      .toFuture()
      .map(_ => ())

  def retrieve(key: SubmissionId): Future[Option[CyaHtmlData]] =
    collection
      .find(bySubmissionId(key))
      .headOption
}

object CyaHtmlData:
  import play.api.libs.json._
  import play.twirl.api.{Html, HtmlFormat}

  given Writes[HtmlFormat.Appendable] =
    Writes { html => JsString(html.body) }

  given Reads[HtmlFormat.Appendable] =
    Reads {
      case JsString(s) => JsSuccess(Html(s))
      case other => JsError(s"Expected JSON string for HtmlFormat.Appendable, got: $other")
    }
  
  given OFormat[CyaHtmlData] = Json.format[CyaHtmlData]


