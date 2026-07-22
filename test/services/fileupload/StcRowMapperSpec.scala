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

package services.fileupload

import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec
import uk.gov.hmrc.securitiestransferchargefrontend.models.stf.fileupload._
import uk.gov.hmrc.securitiestransferchargefrontend.services.fileupload._

import java.time.LocalDate

class StcRowMapperSpec extends AnyWordSpec with Matchers {

  private val headers: Seq[String] = Seq(
    StcColumns.buyerName,
    StcColumns.buyerAddressInUK,
    StcColumns.buyerAddressLine1,
    StcColumns.buyerAddressLine2,
    StcColumns.buyerAddressLine3,
    StcColumns.buyerAddressLine4,
    StcColumns.buyerPostcode,
    StcColumns.buyerCountry,
    StcColumns.sellerName,
    StcColumns.sellerAddressInUK,
    StcColumns.sellerAddressLine1,
    StcColumns.sellerAddressLine2,
    StcColumns.sellerAddressLine3,
    StcColumns.sellerAddressLine4,
    StcColumns.sellerPostcode,
    StcColumns.sellerCountry,
    StcColumns.connectedPersons,
    StcColumns.applyingForRelief,
    StcColumns.whatRelief,
    StcColumns.securitiesTarget,
    StcColumns.companyRegistrationNumber,
    StcColumns.chargingPoint,
    StcColumns.taxRate,
    StcColumns.whatTypeOfSecurities,
    StcColumns.typeOfShares,
    StcColumns.securitiesQuantity,
    StcColumns.amountPaidForSecurities,
    StcColumns.totalMarketValue,
    StcColumns.minSharePrice,
    StcColumns.maxSharePrice,
    StcColumns.purchaseReason,
    StcColumns.purchasedForCancellation
  )

  private implicit val cols: ColumnIndexBuilder =
    new ColumnIndexBuilder(headers)

  private val mapper = new StcRowMapper(cols)

  "StcRowMapper.map" must {

    "map a complete valid row" in {

      val row = ParsedRow(
        rowNumber = 3,
        cells = Seq(
          ParsedCell(0, "Bob Buyer"),
          ParsedCell(1, "yes"),
          ParsedCell(2, "1 Buyer Street"),
          ParsedCell(3, "Buyer District"),
          ParsedCell(4, "Buyer City"),
          ParsedCell(5, ""),
          ParsedCell(6, "AA1 1AA"),
          ParsedCell(7, "United Kingdom"),
          ParsedCell(8, "Bob Seller"),
          ParsedCell(9, "yes"),
          ParsedCell(10, "1 Seller Street"),
          ParsedCell(11, "Seller District"),
          ParsedCell(12, "Seller City"),
          ParsedCell(13, ""),
          ParsedCell(14, "LS1 1AA"),
          ParsedCell(15, "United Kingdom"),
          ParsedCell(16, "no"),
          ParsedCell(17, "yes"),
          ParsedCell(18, "Group relief"),
          ParsedCell(19, "Example Holdings Ltd"),
          ParsedCell(20, "12345678"),
          ParsedCell(21, "2026-03-23"),
          ParsedCell(22, "0.5%"),
          ParsedCell(23, "Shares"),
          ParsedCell(24, "Ordinary"),
          ParsedCell(25, "1000"),
          ParsedCell(26, "5000.25"),
          ParsedCell(27, "6000"),
          ParsedCell(28, "7000"),
          ParsedCell(29, "8000"),
          ParsedCell(30, "treasury"),
          ParsedCell(31, "false")
        )
      )

      val result = mapper.map(row)

      result.rowNumber mustBe 3
      result.buyerName mustBe Some("Bob Buyer")
      result.buyerAddressInUK mustBe Some(true)
      result.buyerAddressLine1 mustBe Some("1 Buyer Street")
      result.buyerAddressLine2 mustBe Some("Buyer District")
      result.buyerAddressLine3 mustBe Some("Buyer City")
      result.buyerPostcode mustBe Some("AA1 1AA")
      result.buyerCountry mustBe Some("United Kingdom")
      result.sellerName mustBe Some("Bob Seller")
      result.sellerAddressInUK mustBe Some(true)
      result.sellerAddressLine1 mustBe Some("1 Seller Street")
      result.sellerAddressLine2 mustBe Some("Seller District")
      result.sellerAddressLine3 mustBe Some("Seller City")
      result.sellerPostcode mustBe Some("LS1 1AA")
      result.sellerCountry mustBe Some("United Kingdom")
      result.connectedPersons mustBe Some(false)
      result.applyingForRelief mustBe Some(true)
      result.whatReliefAreYouApplyingFor mustBe Some("Group relief")
      result.securitiesTarget mustBe Some("Example Holdings Ltd")
      result.companyRegistrationNumber mustBe Some("12345678")
      result.chargingPoint mustBe ParsedValue.Valid(LocalDate.of(2026, 3, 23))
      result.taxRate mustBe Some(BigDecimal("0.5"))
      result.whatTypeOfSecurities mustBe Some("Shares")
      result.typeOfShares mustBe Some("Ordinary")
      result.securitiesQuantity mustBe Some("1000")
      result.amountPaidForSecurities mustBe Some("5000.25")
      result.totalMarketValue mustBe Some("6000")
      result.minSharePrice mustBe Some("7000")
      result.maxSharePrice mustBe Some("8000")
      result.sharePurchaseReason mustBe Some("treasury")
      result.purchaseForCancellation mustBe Some(false)
    }

    "map blank values to Missing" in {

      val row = ParsedRow(
        rowNumber = 4,
        cells = Seq.fill(24)(ParsedCell(0, "")) 
      )

      val result = mapper.map(row)

      result.sellerName mustBe None
      result.sellerPostcode mustBe None
      result.whatTypeOfSecurities mustBe None
      result.typeOfShares mustBe None
    }
  }
}