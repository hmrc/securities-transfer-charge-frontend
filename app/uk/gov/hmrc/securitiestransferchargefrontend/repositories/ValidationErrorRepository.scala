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
import play.api.libs.json.Format
import uk.gov.hmrc.mongo.MongoComponent
import uk.gov.hmrc.mongo.play.json.PlayMongoRepository
import uk.gov.hmrc.mongo.play.json.formats.MongoJavatimeFormats
import uk.gov.hmrc.securitiestransferchargefrontend.config.FrontendAppConfig
import uk.gov.hmrc.securitiestransferchargefrontend.models.stf.fileupload.{StcRowValidationError, ValidationErrorsDocument}

import java.time.Instant
import java.util.concurrent.TimeUnit
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

trait ValidationErrorRepository {

  def save(reference: String, errors: Seq[StcRowValidationError]): Future[Unit]

  def findByReference(reference: String): Future[Seq[StcRowValidationError]]

  def delete(reference: String): Future[Unit]
}

@Singleton
class ValidationErrorRepositoryImpl @Inject()(
                                               mongoComponent: MongoComponent,
                                               appConfig: FrontendAppConfig
                                             )(implicit ec: ExecutionContext)
  extends PlayMongoRepository[ValidationErrorsDocument](
    collectionName = "validation-errors",
    mongoComponent = mongoComponent,
    domainFormat = ValidationErrorsDocument.format,
    indexes = Seq(
      IndexModel(
        Indexes.ascending("createdAt"),
        IndexOptions()
          .name("validation_createdAtIdx")
          .expireAfter(appConfig.validationErrorTtl, TimeUnit.MINUTES)
      )
    )
  ) with ValidationErrorRepository {

  implicit val instantFormat: Format[Instant] = MongoJavatimeFormats.instantFormat

  private def byReference(reference: String) =
    Filters.equal("_id", reference)

  override def save(
                     reference: String,
                     errors: Seq[StcRowValidationError]
                   ): Future[Unit] =
    collection
      .replaceOne(
        byReference(reference),
        ValidationErrorsDocument(
          _id = reference,
          errors = errors
        ),
        ReplaceOptions().upsert(true)
      )
      .toFuture()
      .map(_ => ())

  override def findByReference(
                                reference: String
                              ): Future[Seq[StcRowValidationError]] =
    collection
      .find(byReference(reference))
      .headOption()
      .map(_.map(_.errors).getOrElse(Seq.empty))

  override def delete(
                       reference: String
                     ): Future[Unit] =
    collection
      .deleteOne(byReference(reference))
      .toFuture()
      .map(_ => ())
}
