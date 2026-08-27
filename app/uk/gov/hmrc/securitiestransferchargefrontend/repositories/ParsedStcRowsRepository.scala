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
import uk.gov.hmrc.securitiestransferchargefrontend.models.stf.fileupload.{ParsedStcRow, ParsedStcRowsDocument}

import java.util.concurrent.TimeUnit
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

trait ParsedStcRowsRepository {

  def save(reference: String, rows: Seq[ParsedStcRow], fileName: String): Future[Unit]

  def findByReference(reference: String): Future[Seq[ParsedStcRow]]

  def delete(reference: String): Future[Unit]
  
  def findDocumentByReference(reference: String):Future[Option[ParsedStcRowsDocument]]
}

@Singleton
class ParsedStcRowsRepositoryImpl @Inject()(
                                             mongoComponent: MongoComponent,
                                             appConfig: FrontendAppConfig
                                           )(implicit ec: ExecutionContext)
  extends PlayMongoRepository[ParsedStcRowsDocument](
    collectionName = "parsedStcRows",
    mongoComponent = mongoComponent,
    domainFormat = ParsedStcRowsDocument.format,
    indexes = Seq(
      IndexModel(
        Indexes.ascending("createdAt"),
        IndexOptions()
          .name("parsedStcRows_createdAtIdx")
          .expireAfter(appConfig.parsedStcRowsTtl, TimeUnit.DAYS)
      )
    )
  ) with ParsedStcRowsRepository {

  private def byReference(reference: String) = Filters.equal("_id", reference)

  override def save(reference: String, rows: Seq[ParsedStcRow], fileName: String): Future[Unit] =
    collection
      .insertOne(ParsedStcRowsDocument(_id = reference, rows = rows, fileName = fileName))
      .toFuture()
      .map(_ => ())

  override def findByReference(reference: String): Future[Seq[ParsedStcRow]] =
    collection
      .find(byReference(reference))
      .headOption()
      .map(_.map(_.rows).getOrElse(Seq.empty))

  override def delete(reference: String): Future[Unit] =
    collection
      .deleteOne(byReference(reference))
      .toFuture()
      .map(_ => ())

  def dropCollection(): Future[Unit] =
    collection
      .drop()
      .toFuture()
      .map(_ => ())

  override def findDocumentByReference(reference: String): Future[Option[ParsedStcRowsDocument]] =
    collection.
      find(byReference(reference))
      .headOption()
      
}
