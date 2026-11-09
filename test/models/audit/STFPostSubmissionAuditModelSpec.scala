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

import base.SpecBase
import play.api.libs.json.{JsNull, Json}
import uk.gov.hmrc.securitiestransferchargefrontend.models.audit._

class STFPostSubmissionAuditModelSpec extends SpecBase {

  "STFPostSubmissionAuditModel" - {

    val testMainBuyersDetails = MainBuyersDetails(
      firstName = "John",
      lastName = "Doe",
      nino = "AB123456C",
      buyerCountry = "GB",
      buyerAddressLine1 = "1 High Street",
      buyerAddressLine2 = "District",
      buyerAddressLine3 = "Town",
      buyerAddressCity = "City",
      buyerAddressPostcode = "AB1 2CD"
    )

    val testMainSellerDetails = MainSellerDetails(
      sellerName = "Jane Smith",
      sellerInUk = true,
      sellerCountry = "GB",
      sellerAddressLine1 = "10 Main Road",
      sellerAddressLine2 = "Area",
      sellerAddressLine3 = "Region",
      sellerAddressCity = "London",
      sellerAddressPostcode = "SW1A 1AA"
    )

    val testTransferDetails = TransferDetails(
      connectedPersons = false,
      taxRelief = true,
      typeOfTaxRelief = "Group relief",
      businessName = "Test Company Ltd",
      companyRegistrationNumber = "12345678",
      chargingPointDate = "2024-01-01",
      taxRate = BigDecimal(0.5),
      buyerPurchasingShares = true,
      typeOfSecurity = "",
      numberOfShares = BigDecimal(1000),
      typeOfShares = "Ordinary",
      amountPaid = BigDecimal(7500),
      marketValue = BigDecimal(5000)
    )

    val testTransferCharges = TransferCharges(
      transferNumber = 1,
      uniqueTransferReferenceNumber = "UTRN123",
      chargeAmount = BigDecimal(1000.50),
      chargeType = "STC",
      chargeTypeDescription = "Securities transfer tax",
      chargeReference = "CHG123",
      chargeDueDate = "2024-12-31"
    )

    val testTransfer = Transfer(
      transferNumber = 1,
      mainBuyersDetails = testMainBuyersDetails,
      mainSellerDetails = testMainSellerDetails,
      transferDetails = testTransferDetails,
      transferCharges = Seq(testTransferCharges)
    )

    val testAuditDetail = STFPostSubmissionAuditDetail(
      declarantName = "Jane Smith",
      declarantAddressLine1 = "10 Main Road",
      declarantAddressLine2 = "Area",
      declarantAddressLine3 = "Region",
      declarantAddressCity = "London",
      declarantAddressPostcode = "SW1A 1AA",
      declarantCountry = "GB",
      transferMethod = "OneAtATime",
      subscriptionId = "SUB123",
      submissionId = "STC-123456789",
      credentialId = "CRED123",
      affinityGroup = "Individual",
      totalBatches = 1,
      totalTransfers = 1,
      currentBatch = 1,
      versionNumber = 1,
      declarantRole1 = "Buyer",
      declarantRole2 = "Director",
      reference = "REF123",
      transfers = Seq(testTransfer)
    )

    "must serialize MainBuyersDetails to JSON correctly" in {
      val json = Json.toJson(testMainBuyersDetails)
      
      (json \ "firstName").as[String] mustBe "John"
      (json \ "lastName").as[String] mustBe "Doe"
      (json \ "nino").as[String] mustBe "AB123456C"
      (json \ "buyerCountry").as[String] mustBe "GB"
      (json \ "buyerAddressLine1").as[String] mustBe "1 High Street"
      (json \ "buyerAddressPostcode").as[String] mustBe "AB1 2CD"
    }

    "must serialize MainSellerDetails to JSON correctly" in {
      val json = Json.toJson(testMainSellerDetails)
      
      (json \ "sellerName").as[String] mustBe "Jane Smith"
      (json \ "sellerInUk").as[Boolean] mustBe true
      (json \ "sellerCountry").as[String] mustBe "GB"
      (json \ "sellerAddressLine1").as[String] mustBe "10 Main Road"
      (json \ "sellerAddressPostcode").as[String] mustBe "SW1A 1AA"
    }

    "must serialize TransferDetails to JSON correctly" in {
      val json = Json.toJson(testTransferDetails)
      
      (json \ "connectedPersons").as[Boolean] mustBe false
      (json \ "taxRelief").as[Boolean] mustBe true
      (json \ "typeOfTaxRelief").as[String] mustBe "Group relief"
      (json \ "businessName").as[String] mustBe "Test Company Ltd"
      (json \ "companyRegistrationNumber").as[String] mustBe "12345678"
      (json \ "chargingPointDate").as[String] mustBe "2024-01-01"
      (json \ "taxRate").as[BigDecimal] mustBe BigDecimal(0.5)
      (json \ "buyerPurchasingShares").as[Boolean] mustBe true
      (json \ "numberOfShares").as[BigDecimal] mustBe BigDecimal(1000)
      (json \ "amountPaid").as[BigDecimal] mustBe BigDecimal(7500)
    }

    "must serialize TransferCharges to JSON correctly" in {
      val json = Json.toJson(testTransferCharges)
      
      (json \ "transferNumber").as[Int] mustBe 1
      (json \ "uniqueTransferReferenceNumber").as[String] mustBe "UTRN123"
      (json \ "chargeAmount").as[BigDecimal] mustBe BigDecimal(1000.50)
      (json \ "chargeType").as[String] mustBe "STC"
      (json \ "chargeTypeDescription").as[String] mustBe "Securities transfer tax"
      (json \ "chargeReference").as[String] mustBe "CHG123"
      (json \ "chargeDueDate").as[String] mustBe "2024-12-31"
    }

    "must serialize Transfer to JSON correctly" in {
      val json = Json.toJson(testTransfer)
      
      (json \ "transferNumber").as[Int] mustBe 1
      (json \ "mainBuyersDetails").isDefined mustBe true
      (json \ "mainSellerDetails").isDefined mustBe true
      (json \ "transferDetails").isDefined mustBe true
      (json \ "transferCharges").as[Seq[TransferCharges]].length mustBe 1
    }

    "must serialize STFPostSubmissionAuditDetail to JSON correctly" in {
      val json = Json.toJson(testAuditDetail)
      
      (json \ "declarantName").as[String] mustBe "Jane Smith"
      (json \ "declarantAddressLine1").as[String] mustBe "10 Main Road"
      (json \ "transferMethod").as[String] mustBe "OneAtATime"
      (json \ "subscriptionId").as[String] mustBe "SUB123"
      (json \ "submissionId").as[String] mustBe "STC-123456789"
      (json \ "credentialId").as[String] mustBe "CRED123"
      (json \ "affinityGroup").as[String] mustBe "Individual"
      (json \ "totalBatches").as[Int] mustBe 1
      (json \ "totalTransfers").as[Int] mustBe 1
      (json \ "currentBatch").as[Int] mustBe 1
      (json \ "versionNumber").as[Int] mustBe 1
      (json \ "transfers").as[Seq[Transfer]].length mustBe 1
    }

    "must serialize STFPostSubmissionAuditModel to JSON correctly" in {
      val model = STFPostSubmissionAuditModel(auditDetail = testAuditDetail)
      val json = Json.toJson(model)
      
      (json \ "auditDetail").isDefined mustBe true
      (json \ "auditDetail" \ "submissionId").as[String] mustBe "STC-123456789"
    }

    "must have correct auditType" in {
      val model = STFPostSubmissionAuditModel(auditDetail = testAuditDetail)
      model.auditType mustBe "StockTransferFormSubmission"
    }

    "must convert auditDetail to JsObject correctly" in {
      val model = STFPostSubmissionAuditModel(auditDetail = testAuditDetail)
      val detail = model.detail
      
      detail.keys must contain allOf("declarantName", "submissionId", "transfers")
    }

    "must handle multiple transfers" in {
      val multipleTransfers = Seq(testTransfer, testTransfer.copy(transferNumber = 2))
      val auditDetail = testAuditDetail.copy(
        transfers = multipleTransfers,
        totalTransfers = 2,
        transferMethod = "MoreThanOneAtATime"
      )
      val model = STFPostSubmissionAuditModel(auditDetail = auditDetail)
      
      model.detail.value("transfers").as[Seq[Transfer]].length mustBe 2
      (model.detail \ "transferMethod").as[String] mustBe "MoreThanOneAtATime"
      (model.detail \ "totalTransfers").as[Int] mustBe 2
    }

    "must handle seller not in UK" in {
      val nonUkSeller = testMainSellerDetails.copy(
        sellerInUk = false,
        sellerCountry = "FR"
      )
      val transfer = testTransfer.copy(mainSellerDetails = nonUkSeller)
      val auditDetail = testAuditDetail.copy(transfers = Seq(transfer))
      val model = STFPostSubmissionAuditModel(auditDetail = auditDetail)
      
      val json = Json.toJson(model)
      val sellerDetails = (json \ "auditDetail" \ "transfers" \ 0 \ "mainSellerDetails")
      (sellerDetails \ "sellerInUk").as[Boolean] mustBe false
      (sellerDetails \ "sellerCountry").as[String] mustBe "FR"
    }

    "must handle empty optional fields" in {
      val minimalTransferDetails = testTransferDetails.copy(
        typeOfTaxRelief = "",
        companyRegistrationNumber = "",
        typeOfSecurity = "",
        marketValue = BigDecimal(0)
      )
      val minimalTransfer = testTransfer.copy(transferDetails = minimalTransferDetails)
      val minimalAuditDetail = testAuditDetail.copy(
        declarantAddressLine2 = "",
        declarantAddressLine3 = "",
        declarantRole2 = "",
        reference = "",
        transfers = Seq(minimalTransfer)
      )
      val model = STFPostSubmissionAuditModel(auditDetail = minimalAuditDetail)
      
      val json = Json.toJson(model)
      json must not be JsNull
    }

    "must handle purchasing shares vs other securities" in {
      val securitiesTransferDetails = testTransferDetails.copy(
        buyerPurchasingShares = false,
        typeOfSecurity = "Bonds",
        typeOfShares = ""
      )
      val transfer = testTransfer.copy(transferDetails = securitiesTransferDetails)
      val auditDetail = testAuditDetail.copy(transfers = Seq(transfer))
      val model = STFPostSubmissionAuditModel(auditDetail = auditDetail)
      
      val json = Json.toJson(model)
      val transferDetails = (json \ "auditDetail" \ "transfers" \ 0 \ "transferDetails")
      (transferDetails \ "buyerPurchasingShares").as[Boolean] mustBe false
      (transferDetails \ "typeOfSecurity").as[String] mustBe "Bonds"
      (transferDetails \ "typeOfShares").as[String] mustBe ""
    }
  }
}