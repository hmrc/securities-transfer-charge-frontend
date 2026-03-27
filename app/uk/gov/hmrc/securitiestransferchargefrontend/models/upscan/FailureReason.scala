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

package uk.gov.hmrc.securitiestransferchargefrontend.models.upscan

import play.api.libs.json.*

sealed trait FailureReason {
  def asString: String
}

object FailureReason {

  case object Quarantine extends FailureReason {
    val asString = "QUARANTINE"
  }

  case object Rejected extends FailureReason {
    val asString = "REJECTED"
  }

  case object InvalidArgument extends FailureReason {
    val asString = "INVALID_ARGUMENT"
  }

  case object NotCSV extends FailureReason {
    val asString = "NOT_CSV"
  }

  case object TooLarge extends FailureReason {
    val asString = "TOO_LARGE"
  }

  case object InvalidFileType extends FailureReason {
    val asString = "INVALID_FILE_TYPE"
  }

  case object Unknown extends FailureReason {
    val asString = "UNKNOWN"
  }

  // ✅ Centralised list
  val values: Seq[FailureReason] = Seq(
    Quarantine,
    Rejected,
    InvalidArgument,
    NotCSV,
    TooLarge,
    InvalidFileType,
    Unknown
  )

  // ✅ Safe lookup
  def fromString(value: String): Option[FailureReason] =
    values.find(_.asString == value)

  // ✅ JSON Reads (reuses fromString)
  implicit val reads: Reads[FailureReason] =
    Reads {
      case JsString(value) =>
        JsSuccess(fromString(value).getOrElse(Unknown))
      case _ =>
        JsError("Failure reason must be a string")
    }

  // ✅ JSON Writes
  implicit val writes: Writes[FailureReason] =
    Writes(reason => JsString(reason.asString))
}