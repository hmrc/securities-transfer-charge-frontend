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

import play.api.libs.json.{Format, JsError, JsString, JsSuccess, Json, Reads, Writes, JsObject}
import uk.gov.hmrc.securitiestransferchargefrontend.domain.SubmissionId

import scala.concurrent.Future

sealed trait Status

object Status {
  case object Processing extends Status
  case object Failed extends Status
  case class PartiallyFailed(failures: Map[Int, String]) extends Status

  implicit val reads: Reads[Status] = Reads {
    case JsString("Processing") => JsSuccess(Processing)
    case JsString("Failed") => JsSuccess(Failed)
    case JsObject(o) => Json.reads[PartiallyFailed].reads(o("PartiallyFailed"))
    case _ => JsError("Invalid status")
  }

  given Writes[PartiallyFailed] = Json.writes[PartiallyFailed]
  
  implicit val writes: Writes[Status] = Writes {
    case Processing          => JsString("Processing")
    case Failed              => JsString("Failed")
    case pf: PartiallyFailed => Json.writes[PartiallyFailed].writes(pf)
  }

  implicit val format: Format[Status] = Format(reads, writes)
}

trait ProcessingStatusRepository {
  def insert(submissionId: SubmissionId, status: Status): Future[Unit]
  def update(submissionId: SubmissionId, status: Status): Future[Unit]
  def delete(submissionId: SubmissionId): Future[Unit]
  def findBySubmissionIds(submissionIds: Seq[String]): Future[Set[SubmissionId]]
}

final class ProcessingStatusRepositoryImpl extends ProcessingStatusRepository {
  private val unitF = Future.successful(())
  
  def insert(submissionId: SubmissionId, status: Status): Future[Unit] = unitF

  def update(submissionId: SubmissionId, status: Status): Future[Unit] = unitF

  def delete(submissionId: SubmissionId): Future[Unit] = unitF

  def findBySubmissionIds(submissionIds: Seq[String]): Future[Set[SubmissionId]] =
    Future.successful(Set(SubmissionId("STC-123"))) 
}

