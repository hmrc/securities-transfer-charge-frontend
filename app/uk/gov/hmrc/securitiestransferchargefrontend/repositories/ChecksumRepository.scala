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
import uk.gov.hmrc.securitiestransferchargefrontend.models.stf.fileupload.UploadedFileChecksum

import java.util.concurrent.TimeUnit
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

trait ChecksumRepository {

  def exists(checksum: String): Future[Boolean]

  def insert(record: UploadedFileChecksum): Future[Unit]
}

@Singleton
class ChecksumRepositoryImpl @Inject()(
                                        mongoComponent: MongoComponent,
                                        appConfig: FrontendAppConfig
                                      )(implicit ec: ExecutionContext)
  extends PlayMongoRepository[UploadedFileChecksum](
    collectionName = "uploaded-file-checksums",
    mongoComponent = mongoComponent,
    domainFormat = UploadedFileChecksum.format,
    indexes = Seq(
      IndexModel(
        Indexes.ascending("uploadedAt"),
        IndexOptions()
          .name("uploadedAt_ttl_idx")
          .expireAfter(appConfig.checksumTtl, TimeUnit.DAYS)
      ),
      IndexModel(
        Indexes.ascending("checksum"),
        IndexOptions()
          .name("checksum_idx")
      )
    )
  )
    with ChecksumRepository {

  private def byChecksum(checksum: String) =
    Filters.equal("checksum", checksum)

  override def insert(record: UploadedFileChecksum): Future[Unit] =
    collection
      .insertOne(record)
      .toFuture()
      .map(_ => ())


  override def exists(checksum: String): Future[Boolean] =
    collection
      .countDocuments(
        byChecksum(checksum)
      ).toFuture().map(_ > 0)

  def dropCollection(): Future[Unit] =
    collection
      .drop()
      .toFuture()
      .map(_ => ())
}
