/*
 * Copyright 2025 HM Revenue & Customs
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

import java.net.URI
import java.time.Instant

sealed trait UpscanCallbackRequest {
  def reference: String
}

object UpscanCallbackRequest {
  
  
  case class Ready(
                    reference: String,
                    downloadUrl: URI,
                    uploadDetails: UploadDetails
                  ) extends UpscanCallbackRequest

  case class Failed(
                     reference: String,
                     failureDetails: ErrorDetails
                   ) extends UpscanCallbackRequest

  case class UploadDetails(
                            uploadTimestamp: Instant,
                            checksum: String,
                            fileMimeType: String,
                            fileName: String,
                            size: Long
                          )

  case class ErrorDetails(
                           failureReason: String,
                           message: String
                         )
  
  implicit val uploadDetailsFormat: OFormat[UploadDetails] = Json.format[UploadDetails]
  implicit val errorDetailsFormat: OFormat[ErrorDetails] = Json.format[ErrorDetails]
  implicit val uriFormat: Format[URI] = Format(
    Reads.StringReads.map(new URI(_)),
    Writes[URI](uri => JsString(uri.toString))
  )
  implicit val readyCallbackBodyFormat: OFormat[Ready] = Json.format[Ready]
  implicit val failedCallbackBodyFormat: OFormat[Failed] = Json.format[Failed]

  implicit val callbackReads: Reads[UpscanCallbackRequest] = (json: JsValue) => json \ "fileStatus" match {
    case JsDefined(JsString("READY")) => json.validate[Ready]
    case JsDefined(JsString("FAILED")) => json.validate[Failed]
    case JsDefined(value) => JsError(s"Invalid type discriminator: $value")
    case _ => JsError(s"Missing type discriminator")
  }

}