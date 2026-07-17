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
import play.api.mvc.QueryStringBindable

enum JourneyType(val value: String):

  case STF extends JourneyType("stf")
  case SH03 extends JourneyType("sh03")

object JourneyType:

  def fromString(value: String): Option[JourneyType] =
    JourneyType.values.find(_.value.equalsIgnoreCase(value))

  given Reads[JourneyType] =
    Reads.StringReads.collect(JsonValidationError("Invalid journey type")) {
      case value if value.equalsIgnoreCase(STF.value)  => STF
      case value if value.equalsIgnoreCase(SH03.value) => SH03
    }

  given Writes[JourneyType] =
    Writes.StringWrites.contramap(_.value)

 

  given QueryStringBindable[JourneyType] with

    private val stringBinder = summon[QueryStringBindable[String]]

    override def bind(
                       key: String,
                       params: Map[String, Seq[String]]
                     ): Option[Either[String, JourneyType]] =
      stringBinder
        .bind(key, params)
        .map(_.flatMap(value =>
          fromString(value).toRight(s"Invalid journey type: $value")
        ))

    override def unbind(
                         key: String,
                         journeyType: JourneyType
                       ): String =
      stringBinder.unbind(key, journeyType.value)