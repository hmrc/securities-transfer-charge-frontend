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

package uk.gov.hmrc.securitiestransferchargefrontend.models.submission

import play.api.libs.json.*

enum Status:
  case Processing, Failed
  case PartiallyFailed(rowFailures: Map[Int, Seq[String]])

object Status:

  val statusReads: Reads[Status] = Reads {
    case JsString("Processing") => JsSuccess(Status.Processing)
    case JsString("Failed") => JsSuccess(Status.Failed)
    case json: JsObject if (json \ "PartiallyFailed").isDefined =>
      (json \ "PartiallyFailed")
        .validate[Map[Int, Seq[String]]]
        .map(Status.PartiallyFailed.apply)
    case _ => JsError("Invalid status")
  }

  val statusWrites: Writes[Status] = Writes {
    case Status.PartiallyFailed(rowFailures) => Json.obj("PartiallyFailed" -> Json.toJson(rowFailures))
    case status => JsString(status.toString)
  }

  given Format[Status] = Format(statusReads, statusWrites)