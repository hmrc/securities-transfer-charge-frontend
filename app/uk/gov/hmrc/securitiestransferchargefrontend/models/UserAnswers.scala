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

package uk.gov.hmrc.securitiestransferchargefrontend.models

import play.api.libs.json.*
import play.api.mvc.Call
import uk.gov.hmrc.mongo.play.json.formats.MongoJavatimeFormats
import uk.gov.hmrc.securitiestransferchargefrontend.domain.{GroupIdentifier, SubmissionId, UserId}
import uk.gov.hmrc.securitiestransferchargefrontend.queries.{Gettable, Settable}

import java.time.Instant
import scala.language.postfixOps
import scala.util.{Failure, Success, Try}

case class UserAnswers(userId: UserId,
                       groupIdentifier: GroupIdentifier,
                       submissionId: SubmissionId,
                       fileUploadReference: Option[String] = None,
                       nextPage: Option[Call] = None,
                       data: JsObject = Json.obj(),
                       lastUpdated: Instant = Instant.now) {

  def setNextPage(call: Call): UserAnswers = this.copy(nextPage = Some(call))

  def get[A](page: Gettable[A])(implicit rds: Reads[A]): Option[A] =
    Reads.optionNoError(Reads.at(page.path)).reads(data).getOrElse(None)

  def exists[A](page: Gettable[A])(implicit rds: Reads[A]): Boolean =
    Reads.optionNoError(Reads.at(page.path)).reads(data).isSuccess
    
  def set[A](page: Settable[A], value: A)(implicit writes: Writes[A]): Try[UserAnswers] = {

    val updatedData = data.setObject(page.path, Json.toJson(value)) match {
      case JsSuccess(jsValue, _) =>
        Success(jsValue)
      case JsError(errors) =>
        Failure(JsResultException(errors))
    }

    updatedData.flatMap {
      d =>
        val updatedAnswers = copy (data = d)
        page.cleanup(Some(value), updatedAnswers)
    }
  }
  
  def setFileUploadReference(reference: String): UserAnswers = this.copy(fileUploadReference = Some(reference))
  
  def getFileUploadReference(): String = this.fileUploadReference.getOrElse(throw new IllegalStateException("fileUploadReference missing "))

  def remove[A](page: Settable[A]): Try[UserAnswers] = {

    val updatedData = data.removeObject(page.path) match {
      case JsSuccess(jsValue, _) =>
        Success(jsValue)
      case JsError(_) =>
        Success(data)
    }

    updatedData.flatMap {
      d =>
        val updatedAnswers = copy (data = d)
        page.cleanup(None, updatedAnswers)
    }
  }
}

object UserAnswers {
  import play.api.libs.functional.syntax.*

  val empty: UserId => GroupIdentifier => SubmissionId => UserAnswers =
    userId => groupIdentifier => submissionId => UserAnswers(userId, groupIdentifier, submissionId)

  implicit val callFormat: Format[Call] = new Format[Call] {

    /* The Call constructor defaults its fragment attribute to null
     * which causes the default serDes to fail. We get around this here
     * by using an Option.
     */

    override def writes(call: Call): JsValue = {
      Json.obj(
        "method"    -> call.method,
        "url"       -> call.url,
        "fragment"  -> Option(call.fragment)
      )
    }

    override def reads(json: JsValue): JsResult[Call] = for {
      method    <- (json \ "method").validate[String]
      url       <- (json \ "url").validate[String]
      fragment  <- (json \ "fragment").validateOpt[String]
    } yield Call(method, url, fragment.orNull)

  }

  val reads: Reads[UserAnswers] = (
      (__ \ "_id").read[UserId] and
      (__ \ "groupIdentifier").read[GroupIdentifier] and
      (__ \ "submissionId").read[SubmissionId] and
      (__ \ "fileUploadReference").readNullable[String] and
      (__ \ "nextPage").readNullable[Call] and
      (__ \ "data").read[JsObject] and
      (__ \ "lastUpdated").read(MongoJavatimeFormats.instantFormat)
    ) (UserAnswers.apply _)

  val writes: OWrites[UserAnswers] = (
    (__ \ "_id").write[UserId] and
      (__ \ "groupIdentifier").write[GroupIdentifier] and
      (__ \ "submissionId").write[SubmissionId] and
      (__ \ "fileUploadReference").writeNullable[String] and
      (__ \ "nextPage").writeNullable[Call] and
      (__ \ "data").write[JsObject] and
      (__ \ "lastUpdated").write(MongoJavatimeFormats.instantFormat)
    ) (ua => (ua.userId, ua.groupIdentifier, ua.submissionId, ua.fileUploadReference, ua.nextPage, ua.data, ua.lastUpdated))

  implicit val format: OFormat[UserAnswers] = OFormat(reads, writes)
}
