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
import uk.gov.hmrc.securitiestransferchargefrontend.models.stf.fileupload.{ParsedCell, ParsedRow, ParsedStcRow, ParsedValue}
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
        addressLine1 = ParsedValue.Valid("10 Downing Street"),
        addressLine2 = ParsedValue.Valid("Westminster"),
        addressLine3 = ParsedValue.Valid("London"),
        addressLine4 = ParsedValue.Missing,
        postcode = ParsedValue.Valid("SW1A 2AA"),
        country = ParsedValue.Valid("United Kingdom"),
        sellerName = ParsedValue.Valid("Bob Seller"),
        sellerAddressInUk = ParsedValue.Valid(true),
        sellerAddressLine1 = ParsedValue.Valid("1 Seller Street"),
        sellerAddressLine2 = ParsedValue.Valid("Seller District"),
        sellerAddressLine3 = ParsedValue.Valid("Seller City"),
        sellerAddressLine4 = ParsedValue.Missing,
        sellerPostcode = ParsedValue.Valid("LS1 1AA"),
        sellerCountry = ParsedValue.Valid("United Kingdom"),
        connectedPersons = ParsedValue.Valid(false),
        applyingForRelief = ParsedValue.Valid(true),
        whatReliefAreYouApplyingFor = ParsedValue.Valid("Group relief"),
        securitiesTarget = ParsedValue.Valid("Example Holdings Ltd"),
        companyRegistrationNumber = ParsedValue.Valid("12345678"),
        chargingPoint = ParsedValue.Valid(LocalDate.of(2026, 3, 23)),
        taxRate = ParsedValue.Valid(BigDecimal("0.5")),
        whatTypeOfSecurities = ParsedValue.Valid("Shares"),
        typeOfShares = ParsedValue.Valid("Ordinary"),
        securitiesQuantity = ParsedValue.Valid(BigDecimal("1000")),
        amountPaidForSecurities = ParsedValue.Valid(BigDecimal("5000.25")),
        totalMarketValue = ParsedValue.Valid(BigDecimal("6000"))
      )
    }

    "map blank values to Missing where appropriate" in {
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
        addressLine1 = ParsedValue.Missing,
        addressLine2 = ParsedValue.Missing,
        addressLine3 = ParsedValue.Missing,
        addressLine4 = ParsedValue.Missing,
        postcode = ParsedValue.Missing,
        country = ParsedValue.Missing,
        sellerName = ParsedValue.Missing,
        sellerAddressInUk = ParsedValue.Missing,
        sellerAddressLine1 = ParsedValue.Missing,
        sellerAddressLine2 = ParsedValue.Missing,
        sellerAddressLine3 = ParsedValue.Missing,
        sellerAddressLine4 = ParsedValue.Missing,
        sellerPostcode = ParsedValue.Missing,
        sellerCountry = ParsedValue.Missing,
        connectedPersons = ParsedValue.Missing,
        applyingForRelief = ParsedValue.Missing,
        whatReliefAreYouApplyingFor = ParsedValue.Missing,
        securitiesTarget = ParsedValue.Missing,
        companyRegistrationNumber = ParsedValue.Missing,
        chargingPoint = ParsedValue.Missing,
        taxRate = ParsedValue.Missing,
        whatTypeOfSecurities = ParsedValue.Missing,
        typeOfShares = ParsedValue.Missing,
        securitiesQuantity = ParsedValue.Missing,
        amountPaidForSecurities = ParsedValue.Missing,
        totalMarketValue = ParsedValue.Missing
      )
    }

    "preserve invalid typed values as Invalid instead of collapsing them to Missing" in {
      val row = ParsedRow(
        rowNumber = 5,
        cells = Seq(
          ParsedCell(8, "maybe"),
          ParsedCell(20, "not-a-date"),
          ParsedCell(21, "abc"),
          ParsedCell(24, "foo"),
          ParsedCell(25, "bar"),
          ParsedCell(26, "baz")
        )
      )

      mapper.map(row) shouldBe ParsedStcRow(
        rowNumber = 5,
        addressLine1 = ParsedValue.Missing,
        addressLine2 = ParsedValue.Missing,
        addressLine3 = ParsedValue.Missing,
        addressLine4 = ParsedValue.Missing,
        postcode = ParsedValue.Missing,
        country = ParsedValue.Missing,
        sellerName = ParsedValue.Missing,
        sellerAddressInUk = ParsedValue.Invalid("maybe", "not a recognised boolean"),
        sellerAddressLine1 = ParsedValue.Missing,
        sellerAddressLine2 = ParsedValue.Missing,
        sellerAddressLine3 = ParsedValue.Missing,
        sellerAddressLine4 = ParsedValue.Missing,
        sellerPostcode = ParsedValue.Missing,
        sellerCountry = ParsedValue.Missing,
        connectedPersons = ParsedValue.Missing,
        applyingForRelief = ParsedValue.Missing,
        whatReliefAreYouApplyingFor = ParsedValue.Missing,
        securitiesTarget = ParsedValue.Missing,
        companyRegistrationNumber = ParsedValue.Missing,
        chargingPoint = ParsedValue.Invalid("not-a-date", "not a valid date"),
        taxRate = ParsedValue.Invalid("abc", "not a number"),
        whatTypeOfSecurities = ParsedValue.Missing,
        typeOfShares = ParsedValue.Missing,
        securitiesQuantity = ParsedValue.Invalid("foo", "not a number"),
        amountPaidForSecurities = ParsedValue.Invalid("bar", "not a number"),
        totalMarketValue = ParsedValue.Invalid("baz", "not a number")
      )
    }
  }
}