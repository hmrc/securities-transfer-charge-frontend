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

final case class YnBoolean(value: Boolean):
  def toBoolean: Boolean = value

object YnBoolean:
  val Yes: YnBoolean = YnBoolean(value = true)
  val No: YnBoolean = YnBoolean(value = false)

  def fromBoolean(value: Boolean): YnBoolean = if value then Yes else No

  extension (value: Boolean)
    def toYnBoolean: YnBoolean = fromBoolean(value)

  given Format[YnBoolean] = Format(
    Reads {
      case JsString("Y") => JsSuccess(Yes)
      case JsString("N") => JsSuccess(No)
      case JsString(other) => JsError(s"Expected 'Y' or 'N' for YnBoolean, got '$other'")
      case other => JsError(s"Expected JSON string 'Y' or 'N' for YnBoolean, got: $other")
    },
    Writes(value => JsString(if value.value then "Y" else "N"))
  )
