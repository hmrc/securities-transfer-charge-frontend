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

package models.audit

import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import play.api.libs.json.{JsObject, Json}
import uk.gov.hmrc.securitiestransferchargefrontend.models.audit.{AuditType, UpscanValidationAuditModel}

class UpscanValidationAuditModelSpec extends AnyFreeSpec with Matchers {

  "UpscanValidationAuditModel" - {

    val fileReference = "test-file-reference-123"
    val fileName = "test-file.xlsx"
    val failureReason = "QUARANTINE"
    val failureMessage = "File contains virus"

    "must have the correct audit type" in {
      val model = UpscanValidationAuditModel(
        upscanStatus = "READY",
        fileReference = fileReference,
        fileName = Some(fileName),
        failureReason = None,
        failureMessage = None,
        stcAuditType = AuditType.UpscanValidation
      )

      model.auditType mustBe AuditType.UpscanValidation.value
    }

    "for success cases" - {

      "must serialize to JSON correctly with fileName" in {
        val model = UpscanValidationAuditModel(
          upscanStatus = "READY",
          fileReference = fileReference,
          fileName = Some(fileName),
          failureReason = None,
          failureMessage = None,
          stcAuditType = AuditType.UpscanValidation
        )

        val expectedJson = Json.obj(
          "upscanStatus" -> "READY",
          "fileReference" -> fileReference,
          "fileName" -> fileName
        )

        model.detail mustBe expectedJson
      }

      "must include fileName in JSON output" in {
        val model = UpscanValidationAuditModel(
          upscanStatus = "READY",
          fileReference = fileReference,
          fileName = Some(fileName),
          failureReason = None,
          failureMessage = None,
          stcAuditType = AuditType.UpscanValidation
        )

        val json = model.detail.as[JsObject]
        
        json.keys must contain allOf("upscanStatus", "fileReference", "fileName")
        (json \ "fileName").as[String] mustBe fileName
      }
    }

    "for failure cases" - {

      "must serialize to JSON correctly without fileName" in {
        val model = UpscanValidationAuditModel(
          upscanStatus = "FAILED",
          fileReference = fileReference,
          failureReason = Some(failureReason),
          failureMessage = Some(failureMessage),
          stcAuditType = AuditType.UpscanValidation
        )

        val expectedJson = Json.obj(
          "upscanStatus" -> "FAILED",
          "fileReference" -> fileReference,
          "failureReason" -> failureReason,
          "failureMessage" -> failureMessage
        )

        model.detail mustBe expectedJson
      }

      "must not include fileName field when it is None" in {
        val model = UpscanValidationAuditModel(
          upscanStatus = "FAILED",
          fileReference = fileReference,
          failureReason = Some(failureReason),
          failureMessage = Some(failureMessage),
          stcAuditType = AuditType.UpscanValidation
        )

        val json = model.detail.as[JsObject]
        
        json.keys must not contain "fileName"
      }

      "must serialize to JSON correctly with failure reason only (no fileName)" in {
        val model = UpscanValidationAuditModel(
          upscanStatus = "FAILED",
          fileReference = fileReference,
          failureReason = Some(failureReason),
          failureMessage = None,
          stcAuditType = AuditType.UpscanValidation
        )

        val expectedJson = Json.obj(
          "upscanStatus" -> "FAILED",
          "fileReference" -> fileReference,
          "failureReason" -> failureReason
        )

        model.detail mustBe expectedJson
      }

      "must serialize to JSON correctly with failure message only (no fileName)" in {
        val model = UpscanValidationAuditModel(
          upscanStatus = "FAILED",
          fileReference = fileReference,
          failureReason = None,
          failureMessage = Some(failureMessage),
          stcAuditType = AuditType.UpscanValidation
        )

        val expectedJson = Json.obj(
          "upscanStatus" -> "FAILED",
          "fileReference" -> fileReference,
          "failureMessage" -> failureMessage
        )

        model.detail mustBe expectedJson
      }

      "must include failureReason field when it is defined" in {
        val model = UpscanValidationAuditModel(
          upscanStatus = "FAILED",
          fileReference = fileReference,
          failureReason = Some(failureReason),
          failureMessage = None,
          stcAuditType = AuditType.UpscanValidation
        )

        val json = model.detail.as[JsObject]
        
        json.keys must contain("failureReason")
        (json \ "failureReason").as[String] mustBe failureReason
      }

      "must include failureMessage field when it is defined" in {
        val model = UpscanValidationAuditModel(
          upscanStatus = "FAILED",
          fileReference = fileReference,
          failureReason = None,
          failureMessage = Some(failureMessage),
          stcAuditType = AuditType.UpscanValidation
        )

        val json = model.detail.as[JsObject]
        
        json.keys must contain("failureMessage")
        (json \ "failureMessage").as[String] mustBe failureMessage
      }
    }

    "must handle different upscan statuses correctly" in {
      val statuses = Seq("READY", "FAILED", "PROCESSING", "UPLOADED")

      statuses.foreach { status =>
        val model = UpscanValidationAuditModel(
          upscanStatus = status,
          fileReference = fileReference,
          fileName = Some(fileName),
          failureReason = None,
          failureMessage = None,
          stcAuditType = AuditType.UpscanValidation
        )

        val json = model.detail.as[JsObject]
        (json \ "upscanStatus").as[String] mustBe status
      }
    }

    "must handle different file names correctly" in {
      val specialFileName = "test file with spaces & special chars.xlsx"
      val model = UpscanValidationAuditModel(
        upscanStatus = "READY",
        fileReference = fileReference,
        fileName = Some(specialFileName),
        failureReason = None,
        failureMessage = None,
        stcAuditType = AuditType.UpscanValidation
      )

      val json = model.detail.as[JsObject]
      (json \ "fileName").as[String] mustBe specialFileName
    }

    "must work with Stf audit type" in {
      val model = UpscanValidationAuditModel(
        upscanStatus = "READY",
        fileReference = fileReference,
        fileName = Some(fileName),
        failureReason = None,
        failureMessage = None,
        stcAuditType = AuditType.Stf
      )

      model.auditType mustBe AuditType.Stf.value
    }

    "must work with Sh03 audit type" in {
      val model = UpscanValidationAuditModel(
        upscanStatus = "READY",
        fileReference = fileReference,
        fileName = Some(fileName),
        failureReason = None,
        failureMessage = None,
        stcAuditType = AuditType.Sh03
      )

      model.auditType mustBe AuditType.Sh03.value
    }
  }
}