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

import base.Fixtures.testCredentialId
import base.SpecBase
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import play.api.libs.json.{JsObject, Json}
import uk.gov.hmrc.securitiestransferchargefrontend.domain.SubscriptionId
import uk.gov.hmrc.securitiestransferchargefrontend.models.audit.{AuditType, BulkUploadProcessedAuditModel}
import uk.gov.hmrc.securitiestransferchargefrontend.models.stf.upscan.UpscanCallbackRequest

import java.time.Instant

class BulkUploadProcessedAuditModelSpec extends AnyFreeSpec with Matchers with SpecBase {

  private val uploadDetails = UpscanCallbackRequest.UploadDetails(
    uploadTimestamp = Instant.parse("2026-03-24T10:15:30Z"),
    checksum = "abc123",
    fileMimeType = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
    fileName = "bulk-upload.xlsx",
    size = 1234L
  )


  "BulkUploadProcessedAuditModel" - {

    val fileReference = "test-file-reference-123"
    val fileName = uploadDetails.fileName

    "must have the correct audit type" in {
      val model = BulkUploadProcessedAuditModel(
        uploadJourney = "STF",
        affinityGroup = "agent",
        subscriptionId = subscriptionId,
        credentialId = testCredentialId,
        fileType = "xlsx",
        fileUploadStatus = "Success",
        fileSize = uploadDetails.size.toString,
        fileValidationTime = 12L,
        fileName = fileName,
        fileReference = fileReference,
        stcAuditType = AuditType.BulkUploadProcessed
      )

      model.auditType mustBe AuditType.BulkUploadProcessed.value
    }

    "for success cases" - {

      "must serialize to JSON correctly with fileName" in {
        val model = BulkUploadProcessedAuditModel(
          uploadJourney = "STF",
          affinityGroup = "agent",
          subscriptionId = subscriptionId,
          credentialId = testCredentialId,
          fileType = "xlsx",
          fileUploadStatus = "Success",
          fileSize = uploadDetails.size.toString,
          fileValidationTime = 12L,
          fileName = fileName,
          fileReference = fileReference,
          numberOfEntries = Some(100),
          stcAuditType = AuditType.BulkUploadProcessed
        )

        val expectedJson = Json.obj(
          "uploadJourney" -> "STF",
          "affinityGroup" -> "agent",
          "subscriptionId" -> subscriptionId,
          "credentialId" -> testCredentialId,
          "fileType" -> "xlsx",
          "fileUploadStatus" -> "Success",
          "fileSize" -> "1234",
          "fileValidationTime" -> 12,
          "fileName" -> fileName,
          "fileReference" -> fileReference,
          "numberOfEntries" -> 100
        )

        model.detail mustBe expectedJson
      }

      "must include fileName in JSON output" in {
        val model = BulkUploadProcessedAuditModel(
          uploadJourney = "STF",
          affinityGroup = "agent",
          subscriptionId = subscriptionId,
          credentialId = testCredentialId,
          fileType = "xlsx",
          fileUploadStatus = "Success",
          fileSize = uploadDetails.size.toString,
          fileValidationTime = 12L,
          fileName = fileName,
          fileReference = fileReference,
          numberOfEntries = Some(100),
          stcAuditType = AuditType.BulkUploadProcessed
        )

        val json = model.detail.as[JsObject]

        json.keys must contain allOf(
          "uploadJourney", "affinityGroup", "subscriptionId", "credentialId", "fileType", "fileUploadStatus",
          "fileSize", "fileValidationTime", "fileName", "fileReference", "numberOfEntries")
        (json \ "fileName").as[String] mustBe fileName
      }
    }

    "for failure cases" - {

      "must serialize to JSON correctly without fileName" in {
        val model = BulkUploadProcessedAuditModel(
          uploadJourney = "SH03",
          affinityGroup = "organisation",
          subscriptionId = subscriptionId,
          credentialId = testCredentialId,
          fileType = "xlsx",
          fileUploadStatus = "Failure",
          fileSize = uploadDetails.size.toString,
          fileValidationTime = 0L,
          fileName = fileName,
          fileReference = fileReference,
          numberOfEntries = Some(84),
          stcAuditType = AuditType.BulkUploadProcessed
        )

        val expectedJson = Json.obj(
          "uploadJourney" -> "SH03",
          "affinityGroup" -> "organisation",
          "subscriptionId" -> subscriptionId,
          "credentialId" -> testCredentialId,
          "fileType" -> "xlsx",
          "fileUploadStatus" -> "Failure",
          "fileSize" -> "1234",
          "fileValidationTime" -> 0,
          "fileName" -> fileName,
          "fileReference" -> fileReference,
          "numberOfEntries" -> 84
        )

        model.detail mustBe expectedJson
      }

//            "must not include volume field when it is None" in {
//              val model = BulkUploadProcessedAuditModel(
//                upscanStatus = "FAILED",
//                fileReference = fileReference,
//                failureReason = Some(failureReason),
//                failureMessage = Some(failureMessage),
//                stcAuditType = AuditType.UpscanValidation
//              )
//
//              val json = model.detail.as[JsObject]
//
//              json.keys must not contain "fileName"
//            }
      //
      //      "must serialize to JSON correctly with failure reason only (no fileName)" in {
      //        val model = BulkUploadProcessedAuditModel(
      //          upscanStatus = "FAILED",
      //          fileReference = fileReference,
      //          failureReason = Some(failureReason),
      //          failureMessage = None,
      //          stcAuditType = AuditType.UpscanValidation
      //        )
      //
      //        val expectedJson = Json.obj(
      //          "upscanStatus" -> "FAILED",
      //          "fileReference" -> fileReference,
      //          "failureReason" -> failureReason
      //        )
      //
      //        model.detail mustBe expectedJson
      //      }
      //
      //      "must serialize to JSON correctly with failure message only (no fileName)" in {
      //        val model = BulkUploadProcessedAuditModel(
      //          upscanStatus = "FAILED",
      //          fileReference = fileReference,
      //          failureReason = None,
      //          failureMessage = Some(failureMessage),
      //          stcAuditType = AuditType.UpscanValidation
      //        )
      //
      //        val expectedJson = Json.obj(
      //          "upscanStatus" -> "FAILED",
      //          "fileReference" -> fileReference,
      //          "failureMessage" -> failureMessage
      //        )
      //
      //        model.detail mustBe expectedJson
      //      }
      //
      //      "must include failureReason field when it is defined" in {
      //        val model = BulkUploadProcessedAuditModel(
      //          upscanStatus = "FAILED",
      //          fileReference = fileReference,
      //          failureReason = Some(failureReason),
      //          failureMessage = None,
      //          stcAuditType = AuditType.UpscanValidation
      //        )
      //
      //        val json = model.detail.as[JsObject]
      //
      //        json.keys must contain("failureReason")
      //        (json \ "failureReason").as[String] mustBe failureReason
      //      }
      //
      //      "must include failureMessage field when it is defined" in {
      //        val model = BulkUploadProcessedAuditModel(
      //          upscanStatus = "FAILED",
      //          fileReference = fileReference,
      //          failureReason = None,
      //          failureMessage = Some(failureMessage),
      //          stcAuditType = AuditType.UpscanValidation
      //        )
      //
      //        val json = model.detail.as[JsObject]
      //
      //        json.keys must contain("failureMessage")
      //        (json \ "failureMessage").as[String] mustBe failureMessage
      //      }
      //    }
      //
      //    "must handle different upscan statuses correctly" in {
      //      val statuses = Seq("READY", "FAILED", "PROCESSING", "UPLOADED")
      //
      //      statuses.foreach { status =>
      //        val model = BulkUploadProcessedAuditModel(
      //          upscanStatus = status,
      //          fileReference = fileReference,
      //          fileName = Some(fileName),
      //          failureReason = None,
      //          failureMessage = None,
      //          stcAuditType = AuditType.UpscanValidation
      //        )
      //
      //        val json = model.detail.as[JsObject]
      //        (json \ "upscanStatus").as[String] mustBe status
      //      }
      //    }
      //
      //    "must handle different file names correctly" in {
      //      val specialFileName = "test file with spaces & special chars.xlsx"
      //      val model = BulkUploadProcessedAuditModel(
      //        upscanStatus = "READY",
      //        fileReference = fileReference,
      //        fileName = Some(specialFileName),
      //        failureReason = None,
      //        failureMessage = None,
      //        stcAuditType = AuditType.UpscanValidation
      //      )
      //
      //      val json = model.detail.as[JsObject]
      //      (json \ "fileName").as[String] mustBe specialFileName
      //    }
      //
      //    "must work with Stf audit type" in {
      //      val model = BulkUploadProcessedAuditModel(
      //        upscanStatus = "READY",
      //        fileReference = fileReference,
      //        fileName = Some(fileName),
      //        failureReason = None,
      //        failureMessage = None,
      //        stcAuditType = AuditType.Stf
      //      )
      //
      //      model.auditType mustBe AuditType.Stf.value
      //    }
      //
      //    "must work with Sh03 audit type" in {
      //      val model = BulkUploadProcessedAuditModel(
      //        upscanStatus = "READY",
      //        fileReference = fileReference,
      //        fileName = Some(fileName),
      //        failureReason = None,
      //        failureMessage = None,
      //        stcAuditType = AuditType.Sh03
      //      )
      //
      //      model.auditType mustBe AuditType.Sh03.value
      //    }
    }
  }
}