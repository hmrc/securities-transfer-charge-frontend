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

import base.Fixtures.{organisationAffinity, testCredentialId}
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
        affinityGroup = agentAffinity,
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
          affinityGroup = agentAffinity,
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
          "affinityGroup" -> agentAffinity,
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
          affinityGroup = agentAffinity,
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

      "must handle different file names correctly" in {
        val specialFileName = "test file with spaces & special chars.xlsx"
        val model = BulkUploadProcessedAuditModel(
          uploadJourney = "SH03",
          affinityGroup = organisationAffinity,
          subscriptionId = subscriptionId,
          credentialId = testCredentialId,
          fileType = "excel",
          fileUploadStatus = "Success",
          fileSize = uploadDetails.size.toString,
          fileValidationTime = 24L,
          fileName = specialFileName,
          fileReference = fileReference,
          numberOfEntries = Some(100),
          stcAuditType = AuditType.BulkUploadProcessed
        )

        val json = model.detail.as[JsObject]
        (json \ "fileName").as[String] mustBe specialFileName
      }
    }

    "for failure cases" - {

      "must serialize to JSON correctly with errorType" in {
        val model = BulkUploadProcessedAuditModel(
          uploadJourney = "SH03",
          affinityGroup = organisationAffinity,
          subscriptionId = subscriptionId,
          credentialId = testCredentialId,
          fileType = "excel",
          fileUploadStatus = "Failure",
          fileSize = uploadDetails.size.toString,
          fileValidationTime = 0L,
          fileName = fileName,
          fileReference = fileReference,
          errorType = Some("sellerName - Enter the seller's name"),
          volume = Some("1"),
          stcAuditType = AuditType.BulkUploadProcessed
        )

        val expectedJson = Json.obj(
          "uploadJourney" -> "SH03",
          "affinityGroup" -> organisationAffinity,
          "subscriptionId" -> subscriptionId,
          "credentialId" -> testCredentialId,
          "fileType" -> "excel",
          "fileUploadStatus" -> "Failure",
          "fileSize" -> "1234",
          "fileValidationTime" -> 0,
          "fileName" -> fileName,
          "fileReference" -> fileReference,
          "errorType" -> "sellerName - Enter the seller's name",
          "volume" -> "1"
        )

        model.detail mustBe expectedJson
      }

      "must serialize to JSON correctly with multiple in errorType and volume as moreThan25" in {
        val model = BulkUploadProcessedAuditModel(
          uploadJourney = "SH03",
          affinityGroup = organisationAffinity,
          subscriptionId = subscriptionId,
          credentialId = testCredentialId,
          fileType = "excel",
          fileUploadStatus = "Failure",
          fileSize = uploadDetails.size.toString,
          fileValidationTime = 0L,
          fileName = fileName,
          fileReference = fileReference,
          errorType = Some("multiple"),
          volume = Some("moreThan25"),
          stcAuditType = AuditType.BulkUploadProcessed
        )

        val expectedJson = Json.obj(
          "uploadJourney" -> "SH03",
          "affinityGroup" -> organisationAffinity,
          "subscriptionId" -> subscriptionId,
          "credentialId" -> testCredentialId,
          "fileType" -> "excel",
          "fileUploadStatus" -> "Failure",
          "fileSize" -> "1234",
          "fileValidationTime" -> 0,
          "fileName" -> fileName,
          "fileReference" -> fileReference,
          "errorType" -> "multiple",
          "volume" -> "moreThan25"
        )

        model.detail mustBe expectedJson
      }
    }
  }
}