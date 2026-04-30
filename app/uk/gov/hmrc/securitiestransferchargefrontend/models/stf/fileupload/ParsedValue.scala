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

package uk.gov.hmrc.securitiestransferchargefrontend.models.stf.fileupload

import play.api.libs.functional.syntax._
import play.api.libs.json._

sealed trait ParsedValue[+A]

object ParsedValue {

  case object Missing extends ParsedValue[Nothing]

  final case class Valid[A](value: A) extends ParsedValue[A]

  final case class Invalid(rawValue: String, reason: String) extends ParsedValue[Nothing]

  implicit def format[A: Format]: Format[ParsedValue[A]] = {
    val missingReads: Reads[ParsedValue[A]] =
      (__ \ "type").read[String]
        .filter(JsonValidationError("type must be missing"))(_ == "missing")
        .map(_ => Missing)

    val validReads: Reads[ParsedValue[A]] =
      (
        (__ \ "type").read[String]
          .filter(JsonValidationError("type must be valid"))(_ == "valid") and
          (__ \ "value").read[A]
        )((_, value) => Valid(value))

    val invalidReads: Reads[ParsedValue[A]] =
      (
        (__ \ "type").read[String]
          .filter(JsonValidationError("type must be invalid"))(_ == "invalid") and
          (__ \ "rawValue").read[String] and
          (__ \ "reason").read[String]
        )((_, rawValue, reason) => Invalid(rawValue, reason))

    val reads: Reads[ParsedValue[A]] =
      missingReads.orElse(validReads).orElse(invalidReads)

    val writes: Writes[ParsedValue[A]] = Writes {
      case Missing =>
        Json.obj("type" -> "missing")

      case Valid(value) =>
        Json.obj(
          "type"  -> "valid",
          "value" -> Json.toJson[A](value)
        )

      case Invalid(rawValue, reason) =>
        Json.obj(
          "type"     -> "invalid",
          "rawValue" -> rawValue,
          "reason"   -> reason
        )
    }

    Format(reads, writes)
  }
}