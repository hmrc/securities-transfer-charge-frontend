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
import uk.gov.hmrc.securitiestransferchargefrontend.models.fileupload.{ParsedCell, ParsedRow, ParsedValue}
import uk.gov.hmrc.securitiestransferchargefrontend.services.fileupload.{StcRowMapper, StcUploadColumn}

import java.time.LocalDate

class StcRowMapperSpec extends AnyWordSpec with Matchers {

  private val mapper = new StcRowMapper

  "StcRowMapper.map" must {

    "map a spreadsheet row into ParsedStcRow" in {
      val row = ParsedRow(
        rowNumber = 3,
        cells = Seq(
          ParsedCell(StcUploadColumn.addressLine1, "1 Buyer Street"),
          ParsedCell(StcUploadColumn.sellerName, "Seller Ltd"),
          ParsedCell(StcUploadColumn.sellerAddressInUK, "yes"),
          ParsedCell(StcUploadColumn.sellerAddressLine1, "1 Seller Street"),
          ParsedCell(StcUploadColumn.sellerPostcode, "AA1 1AA"),
          ParsedCell(StcUploadColumn.connectedPersons, "no"),
          ParsedCell(StcUploadColumn.applyingForRelief, "yes"),
          ParsedCell(StcUploadColumn.whatReliefAreYouApplyingFor, "Charities Relief"),
          ParsedCell(StcUploadColumn.securitiesTarget, "Target Ltd"),
          ParsedCell(StcUploadColumn.whatIsCRN, "12345678"),
          ParsedCell(StcUploadColumn.chargingPoint, "2025-11-20"),
          ParsedCell(StcUploadColumn.taxRate, "0.5%"),
          ParsedCell(StcUploadColumn.whatTypeOfSecurities, "shares"),
          ParsedCell(StcUploadColumn.typeOfShares, "Ordinary Shares"),
          ParsedCell(StcUploadColumn.securitiesQuantity, "100"),
          ParsedCell(StcUploadColumn.amountPaidForSecurities, "1234.56"),
          ParsedCell(StcUploadColumn.totalMarketValue, "2000")
        )
      )

      val result = mapper.map(row)

      result.rowNumber mustBe 3
      result.sellerName mustBe ParsedValue.Valid("Seller Ltd")
      result.sellerAddressInUk mustBe ParsedValue.Valid(true)
      result.sellerAddressLine1 mustBe ParsedValue.Valid("1 Seller Street")
      result.sellerPostcode mustBe ParsedValue.Valid("AA1 1AA")
      result.connectedPersons mustBe ParsedValue.Valid(false)
      result.applyingForRelief mustBe ParsedValue.Valid(true)
      result.whatReliefAreYouApplyingFor mustBe ParsedValue.Valid("Charities Relief")
      result.securitiesTarget mustBe ParsedValue.Valid("Target Ltd")
      result.companyRegistrationNumber mustBe ParsedValue.Valid("12345678")
      result.chargingPoint mustBe ParsedValue.Valid(LocalDate.of(2025, 11, 20))
      result.taxRate mustBe ParsedValue.Valid(BigDecimal("0.5"))
      result.whatTypeOfSecurities mustBe ParsedValue.Valid("shares")
      result.typeOfShares mustBe ParsedValue.Valid("Ordinary Shares")
      result.securitiesQuantity mustBe ParsedValue.Valid(BigDecimal(100))
      result.amountPaidForSecurities mustBe ParsedValue.Valid(BigDecimal("1234.56"))
      result.totalMarketValue mustBe ParsedValue.Valid(BigDecimal("2000"))
    }

    "map empty cells to Missing" in {
      val row = ParsedRow(
        rowNumber = 3,
        cells = Seq(
          ParsedCell(StcUploadColumn.sellerName, ""),
          ParsedCell(StcUploadColumn.typeOfShares, " ")
        )
      )

      val result = mapper.map(row)

      result.sellerName mustBe ParsedValue.Missing
      result.typeOfShares mustBe ParsedValue.Missing
    }
  }
}