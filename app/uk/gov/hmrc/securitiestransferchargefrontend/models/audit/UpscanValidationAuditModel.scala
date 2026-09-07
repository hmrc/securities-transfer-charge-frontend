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

import play.api.libs.json.{JsObject, Json}

case class UpscanValidationAuditModel(
                                       upscanStatus: String,
                                       fileReference: String,
                                       fileName: Option[String] = None,
                                       failureReason: Option[String] = None,
                                       failureMessage: Option[String] = None,
                                       stcAuditType: AuditType
                                     ) extends JsonAuditModel {

  override val auditType: String = stcAuditType.value

  override val detail: JsObject = {

    val baseDetail = Json.obj(
      "upscanStatus" -> upscanStatus,
      "fileReference" -> fileReference
    )

    val withFileName = fileName.fold(baseDetail) { name =>
      baseDetail ++ Json.obj("fileName" -> name)
    }

    val withFailureReason = failureReason.fold(withFileName) { reason =>
      withFileName ++ Json.obj("failureReason" -> reason)
    }

    failureMessage.fold(withFailureReason) { message =>
      withFailureReason ++ Json.obj("failureMessage" -> message)
    }
  }
}
