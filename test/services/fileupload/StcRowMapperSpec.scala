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

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import uk.gov.hmrc.securitiestransferchargefrontend.models.fileupload.{ParsedCell, ParsedRow, ParsedStcRow}
import uk.gov.hmrc.securitiestransferchargefrontend.services.fileupload.StcRowMapper

import java.time.LocalDate

class StcRowMapperSpec extends AnyWordSpec with Matchers {

  private val mapper = new StcRowMapper()

  "map" should {

    "map a parsed row into a ParsedStcRow" in {
      val row = ParsedRow(
        rowNumber = 3,
        cells = Seq(
          ParsedCell(1, "10 Downing Street"),
          ParsedCell(2, "Westminster"),
          ParsedCell(3, "London"),
          ParsedCell(4, ""),
          ParsedCell(5, "SW1A 2AA"),
          ParsedCell(6, "United Kingdom"),
          ParsedCell(7, "Bob Seller"),
          ParsedCell(8, "yes"),
          ParsedCell(9, "1 Seller Street"),
          ParsedCell(10, "Seller District"),
          ParsedCell(11, "Seller City"),
          ParsedCell(12, ""),
          ParsedCell(13, "LS1 1AA"),
          ParsedCell(14, "United Kingdom"),
          ParsedCell(15, "no"),
          ParsedCell(16, "yes"),
          ParsedCell(17, "Group relief"),
          ParsedCell(18, "Example Holdings Ltd"),
          ParsedCell(19, "12345678"),
          ParsedCell(20, "2026-03-23"),
          ParsedCell(21, "0.5%"),
          ParsedCell(22, "Shares"),
          ParsedCell(23, "Ordinary"),
          ParsedCell(24, "1000"),
          ParsedCell(25, "£5000.25"),
          ParsedCell(26, "6000")
        )
      )

      mapper.map(row) shouldBe ParsedStcRow(
        rowNumber = 3,
        addressLine1 = Some("10 Downing Street"),
        addressLine2 = Some("Westminster"),
        addressLine3 = Some("London"),
        addressLine4 = None,
        postcode = Some("SW1A 2AA"),
        country = Some("United Kingdom"),
        sellerName = Some("Bob Seller"),
        sellerAddressInUk = Some(true),
        sellerAddressLine1 = Some("1 Seller Street"),
        sellerAddressLine2 = Some("Seller District"),
        sellerAddressLine3 = Some("Seller City"),
        sellerAddressLine4 = None,
        sellerPostcode = Some("LS1 1AA"),
        sellerCountry = Some("United Kingdom"),
        connectedPersons = Some(false),
        applyingForRelief = Some(true),
        whatReliefAreYouApplyingFor = Some("Group relief"),
        securitiesTarget = Some("Example Holdings Ltd"),
        companyRegistrationNumber = Some("12345678"),
        chargingPoint = Some(LocalDate.of(2026, 3, 23)),
        taxRate = Some(BigDecimal("0.5")),
        whatTypeOfSecurities = Some("Shares"),
        otherSecuritiesType = Some("Ordinary"),
        securitiesQuantity = Some(BigDecimal("1000")),
        amountPaidForSecurities = Some(BigDecimal("5000.25")),
        totalMarketValue = Some(BigDecimal("6000"))
      )
    }

    "map blank values to None where appropriate" in {
      val row = ParsedRow(
        rowNumber = 4,
        cells = Seq(
          ParsedCell(1, " "),
          ParsedCell(2, ""),
          ParsedCell(3, ""),
          ParsedCell(4, ""),
          ParsedCell(5, ""),
          ParsedCell(6, ""),
          ParsedCell(7, ""),
          ParsedCell(8, ""),
          ParsedCell(9, ""),
          ParsedCell(10, ""),
          ParsedCell(11, ""),
          ParsedCell(12, ""),
          ParsedCell(13, ""),
          ParsedCell(14, ""),
          ParsedCell(15, ""),
          ParsedCell(16, ""),
          ParsedCell(17, ""),
          ParsedCell(18, ""),
          ParsedCell(19, ""),
          ParsedCell(20, ""),
          ParsedCell(21, ""),
          ParsedCell(22, ""),
          ParsedCell(23, ""),
          ParsedCell(24, ""),
          ParsedCell(25, ""),
          ParsedCell(26, "")
        )
      )

      mapper.map(row) shouldBe ParsedStcRow(
        rowNumber = 4,
        addressLine1 = None,
        addressLine2 = None,
        addressLine3 = None,
        addressLine4 = None,
        postcode = None,
        country = None,
        sellerName = None,
        sellerAddressInUk = None,
        sellerAddressLine1 = None,
        sellerAddressLine2 = None,
        sellerAddressLine3 = None,
        sellerAddressLine4 = None,
        sellerPostcode = None,
        sellerCountry = None,
        connectedPersons = None,
        applyingForRelief = None,
        whatReliefAreYouApplyingFor = None,
        securitiesTarget = None,
        companyRegistrationNumber = None,
        chargingPoint = None,
        taxRate = None,
        whatTypeOfSecurities = None,
        otherSecuritiesType = None,
        securitiesQuantity = None,
        amountPaidForSecurities = None,
        totalMarketValue = None
      )
    }
  }
}