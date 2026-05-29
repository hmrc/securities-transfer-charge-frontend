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

package uk.gov.hmrc.securitiestransferchargefrontend.models.audit

import play.api.libs.json.*
import uk.gov.hmrc.securitiestransferchargefrontend.models.{Enumerable, WithName}

sealed trait JourneyStatus

object JourneyStatus {

  case object SubmissionStart extends WithName("Start") with JourneyStatus

  val values: Seq[JourneyStatus] = Seq(SubmissionStart)

  val enumerable: Enumerable[JourneyStatus] = Enumerable(values.map(v => v.toString -> v): _*)

  implicit def reads: Reads[JourneyStatus] = Reads[JourneyStatus] {
    case JsString(SubmissionStart.toString) => JsSuccess(SubmissionStart)
    case _ => JsError("error.invalid")
  }

  implicit def writes: Writes[JourneyStatus] =
    Writes(value => JsString(value.toString))
}
