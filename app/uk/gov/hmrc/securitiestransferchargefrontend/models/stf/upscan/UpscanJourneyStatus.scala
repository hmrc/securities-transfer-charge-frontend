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

package uk.gov.hmrc.securitiestransferchargefrontend.models.stf.upscan

import play.api.libs.json.*

sealed trait UpscanJourneyStatus

object UpscanJourneyStatus {

  case object Ready extends UpscanJourneyStatus
  case object Initiated extends UpscanJourneyStatus
  case object Failed    extends UpscanJourneyStatus
  case object Processing    extends UpscanJourneyStatus
  case object Completed    extends UpscanJourneyStatus
  case object RowLimitExceeded    extends UpscanJourneyStatus
  case object EmptyFile    extends UpscanJourneyStatus
  case object TooManyErrors    extends UpscanJourneyStatus
  case object FormatingErrors    extends UpscanJourneyStatus
  case object UpscanDownloadError   extends UpscanJourneyStatus
  case object InvalidTemplate   extends UpscanJourneyStatus
  case object FileParseError   extends UpscanJourneyStatus

  implicit val format: Format[UpscanJourneyStatus] = new Format[UpscanJourneyStatus] {

    override def reads(json: JsValue): JsResult[UpscanJourneyStatus] =
      json.validate[String].flatMap {
        case "Initiated" => JsSuccess(Initiated)
        case "Failed" => JsSuccess(Failed)
        case "Ready" => JsSuccess(Ready)
        case "Processing" => JsSuccess(Processing)
        case "Completed" => JsSuccess(Completed)
        case "RowLimitExceeded" => JsSuccess(RowLimitExceeded)
        case "EmptyFile" => JsSuccess(EmptyFile)
        case "TooManyErrors" => JsSuccess(TooManyErrors)
        case "FormatingErrors" => JsSuccess(FormatingErrors)
        case "UpscanDownloadError" => JsSuccess(UpscanDownloadError)
        case "InvalidTemplate" => JsSuccess(InvalidTemplate)
        case "FileParseError" => JsSuccess(FileParseError)
        case other => JsError(s"Invalid UpscanJourneyStatus: $other")
      }

    override def writes(status: UpscanJourneyStatus): JsValue =
      JsString(status.toString)
  }
}
