/*
 * Copyright 2024 HM Revenue & Customs
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

import base.SpecBase
import play.api.libs.json.{JsNull, Json}
import uk.gov.hmrc.securitiestransferchargefrontend.models.audit._

class SH03PostSubmissionAuditModelSpec extends SpecBase {

  "SH03PostSubmissionAuditModel" - {

    val testTransferCharge = SH03TransferCharge(
      transferNumber = 1,
      uniqueTransferReferenceNumber = "UTRN123",
      chargeAmount = BigDecimal(1000.50),
      chargeTypeDescription = "STT",
      chargeReference = "CHG123",
      chargeDueDate = "2024-12-31"
    )

    val testMainBuyerDetails = SH03MainBuyerDetails(
      mainBuyerName = "John Doe",
      mainBuyerAddressLine1 = "1 High Street",
      mainBuyerAddressLine2 = "District",
      mainBuyerAddressLine3 = "Town",
      mainBuyerAddressCity = "City",
      postcode = "AB1 2CD",
      country = "GB",
      email = "john@example.com",
      uniqueId = "AB123456C",
      taxRate = 1,
      isPublicLimitedCompany = true
    )

    val testTransactionDetails = SH03TransactionDetails(
      transactionType = 1,
      reasonForPurchase = "Purchased for cancellation",
      typeOfSecurity = "Shares",
      numberOfShares = BigDecimal(1000),
      nominalValue = "",
      marketValue = BigDecimal(5000),
      qualifyAsTreasuryShares = true,
      maxPricePaid = BigDecimal(10),
      minPricePaid = BigDecimal(5),
      originalChargingPoint = "2024-01-01",
      considerationActual = BigDecimal(7500),
      isConnectedPartiesTransactions = false,
      companyName = "Test Company Ltd",
      companyRegistrationNumber = "12345678",
      reliefClaimedName = "Group relief",
      reliefPercentage = 50
    )

    val testTransfer = SH03Transfer(
      transferNumber = 1,
      transactionDetails = testTransactionDetails,
      mainBuyerDetails = testMainBuyerDetails,
      transferCharges = Seq(testTransferCharge)
    )

    val testAuditDetail = SH03PostSubmissionAuditDetail(
      declarantRole1 = "Buyer",
      declarantRole2 = "Director",
      reference = "REF123",
      declarantName = "Jane Smith",
      declarantAddressLine1 = "10 Main Road",
      declarantAddressLine2 = "Area",
      declarantAddressLine3 = "Region",
      declarantAddressCity = "London",
      declarantPostcode = "SW1A 1AA",
      declarantCountry = "GB",
      affinityGroup = "Organisation",
      selfDeclarationAgent = false,
      isCorrectInfo = true,
      transferMethod = "oneAtATime",
      subscriptionId = "SUB123",
      submissionId = "STC-123456789",
      credentialId = "CRED123",
      totalBatches = 1,
      totalTransfers = 1,
      currentBatch = 1,
      versionNumber = 1,
      transfers = Seq(testTransfer)
    )

    "must serialize SH03TransferCharge to JSON correctly" in {
      val json = Json.toJson(testTransferCharge)
      
      (json \ "transferNumber").as[Int] mustBe 1
      (json \ "uniqueTransferReferenceNumber").as[String] mustBe "UTRN123"
      (json \ "chargeAmount").as[BigDecimal] mustBe BigDecimal(1000.50)
      (json \ "chargeTypeDescription").as[String] mustBe "STT"
      (json \ "chargeReference").as[String] mustBe "CHG123"
      (json \ "chargeDueDate").as[String] mustBe "2024-12-31"
    }

    "must serialize SH03MainBuyerDetails to JSON correctly" in {
      val json = Json.toJson(testMainBuyerDetails)
      
      (json \ "mainBuyerName").as[String] mustBe "John Doe"
      (json \ "mainBuyerAddressLine1").as[String] mustBe "1 High Street"
      (json \ "postcode").as[String] mustBe "AB1 2CD"
      (json \ "country").as[String] mustBe "GB"
      (json \ "email").as[String] mustBe "john@example.com"
      (json \ "uniqueId").as[String] mustBe "AB123456C"
      (json \ "taxRate").as[Int] mustBe 1
      (json \ "isPublicLimitedCompany").as[Boolean] mustBe true
    }

    "must serialize SH03TransactionDetails to JSON correctly" in {
      val json = Json.toJson(testTransactionDetails)
      
      (json \ "transactionType").as[Int] mustBe 1
      (json \ "reasonForPurchase").as[String] mustBe "Purchased for cancellation"
      (json \ "typeOfSecurity").as[String] mustBe "Shares"
      (json \ "numberOfShares").as[BigDecimal] mustBe BigDecimal(1000)
      (json \ "marketValue").as[BigDecimal] mustBe BigDecimal(5000)
      (json \ "qualifyAsTreasuryShares").as[Boolean] mustBe true
      (json \ "considerationActual").as[BigDecimal] mustBe BigDecimal(7500)
      (json \ "isConnectedPartiesTransactions").as[Boolean] mustBe false
      (json \ "companyName").as[String] mustBe "Test Company Ltd"
      (json \ "reliefPercentage").as[Int] mustBe 50
    }

    "must serialize SH03Transfer to JSON correctly" in {
      val json = Json.toJson(testTransfer)
      
      (json \ "transferNumber").as[Int] mustBe 1
      (json \ "transactionDetails").isDefined mustBe true
      (json \ "mainBuyerDetails").isDefined mustBe true
      (json \ "transferCharges").as[Seq[SH03TransferCharge]].length mustBe 1
    }

    "must serialize SH03PostSubmissionAuditDetail to JSON correctly" in {
      val json = Json.toJson(testAuditDetail)
      
      (json \ "declarantRole1").as[String] mustBe "Buyer"
      (json \ "declarantRole2").as[String] mustBe "Director"
      (json \ "reference").as[String] mustBe "REF123"
      (json \ "declarantName").as[String] mustBe "Jane Smith"
      (json \ "affinityGroup").as[String] mustBe "Organisation"
      (json \ "selfDeclarationAgent").as[Boolean] mustBe false
      (json \ "isCorrectInfo").as[Boolean] mustBe true
      (json \ "transferMethod").as[String] mustBe "oneAtATime"
      (json \ "subscriptionId").as[String] mustBe "SUB123"
      (json \ "submissionId").as[String] mustBe "STC-123456789"
      (json \ "totalBatches").as[Int] mustBe 1
      (json \ "totalTransfers").as[Int] mustBe 1
      (json \ "currentBatch").as[Int] mustBe 1
      (json \ "versionNumber").as[Int] mustBe 1
      (json \ "transfers").as[Seq[SH03Transfer]].length mustBe 1
    }

    "must serialize SH03PostSubmissionAuditModel to JSON correctly" in {
      val model = SH03PostSubmissionAuditModel(auditDetail = testAuditDetail)
      val json = Json.toJson(model)
      
      (json \ "auditDetail").isDefined mustBe true
      (json \ "auditDetail" \ "submissionId").as[String] mustBe "STC-123456789"
    }

    "must have correct auditType" in {
      val model = SH03PostSubmissionAuditModel(auditDetail = testAuditDetail)
      model.auditType mustBe "PurchaseOfOwnSharesSubmission"
    }

    "must convert auditDetail to JsObject correctly" in {
      val model = SH03PostSubmissionAuditModel(auditDetail = testAuditDetail)
      val detail = model.detail
      
      detail.keys must contain allOf("declarantName", "submissionId", "transfers")
    }

    "must handle multiple transfers" in {
      val multipleTransfers = Seq(testTransfer, testTransfer.copy(transferNumber = 2))
      val auditDetail = testAuditDetail.copy(
        transfers = multipleTransfers,
        totalTransfers = 2,
        transferMethod = "moreThanOneAtATime"
      )
      val model = SH03PostSubmissionAuditModel(auditDetail = auditDetail)
      
      model.detail.value("transfers").as[Seq[SH03Transfer]].length mustBe 2
      (model.detail \ "transferMethod").as[String] mustBe "moreThanOneAtATime"
      (model.detail \ "totalTransfers").as[Int] mustBe 2
    }

    "must handle empty optional fields" in {
      val minimalTransactionDetails = testTransactionDetails.copy(
        companyRegistrationNumber = "",
        reliefClaimedName = "",
        reliefPercentage = 0
      )
      val minimalTransfer = testTransfer.copy(
        transactionDetails = minimalTransactionDetails
      )
      val minimalAuditDetail = testAuditDetail.copy(
        declarantRole2 = "",
        reference = "",
        declarantAddressLine2 = "",
        declarantAddressLine3 = "",
        selfDeclarationAgent = false,
        transfers = Seq(minimalTransfer)
      )
      val model = SH03PostSubmissionAuditModel(auditDetail = minimalAuditDetail)
      
      val json = Json.toJson(model)
      json must not be JsNull
    }
  }
}