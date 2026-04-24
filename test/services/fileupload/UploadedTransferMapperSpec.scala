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
import uk.gov.hmrc.securitiestransferchargefrontend.services.fileupload.UploadedTransferMapper
import uk.gov.hmrc.securitiestransferchargefrontend.services.fileupload.UploadedTransferMappingError

import java.time.LocalDate

class UploadedTransferMapperSpec extends AnyWordSpec with Matchers {

  "UploadedTransferMapper.fromValidatedRow" must {

    "map shares using the typeOfShares column" in {
      val row = validParsedRow(
        whatTypeOfSecurities = ParsedValue.Valid("shares"),
        typeOfShares = ParsedValue.Valid("Ordinary Shares")
      )

      val result = UploadedTransferMapper.fromValidatedRow(row).toOption.get

      result.securityDetails mustBe UploadedSecurityDetails.Shares("Ordinary Shares")
    }

    "map shares using whatever share type the user entered" in {
      val row = validParsedRow(
        whatTypeOfSecurities = ParsedValue.Valid("shares"),
        typeOfShares = ParsedValue.Valid("Preference Shares")
      )

      val result = UploadedTransferMapper.fromValidatedRow(row).toOption.get

      result.securityDetails mustBe UploadedSecurityDetails.Shares("Preference Shares")
    }

    "map non-shares using whatTypeOfSecurities as the description" in {
      val row = validParsedRow(
        whatTypeOfSecurities = ParsedValue.Valid("Loan notes or other debt securities"),
        typeOfShares = ParsedValue.Missing
      )

      val result = UploadedTransferMapper.fromValidatedRow(row).toOption.get

      result.securityDetails mustBe UploadedSecurityDetails.Other("Loan notes or other debt securities")
    }

    "map interests in underlying securities as Other" in {
      val row = validParsedRow(
        whatTypeOfSecurities = ParsedValue.Valid("Interests in underlying securities"),
        typeOfShares = ParsedValue.Missing
      )

      val result = UploadedTransferMapper.fromValidatedRow(row).toOption.get

      result.securityDetails mustBe UploadedSecurityDetails.Other("Interests in underlying securities")
    }

    "return an error when shares are selected but typeOfShares is missing" in {
      val row = validParsedRow(
        whatTypeOfSecurities = ParsedValue.Valid("shares"),
        typeOfShares = ParsedValue.Missing
      )

      UploadedTransferMapper.fromValidatedRow(row) mustBe Left(
        UploadedTransferMappingError(
          rowNumber = 3,
          fieldName = "typeOfShares",
          reason = "Required value was missing after validation"
        )
      )
    }

    "return an error when whatTypeOfSecurities is missing" in {
      val row = validParsedRow(
        whatTypeOfSecurities = ParsedValue.Missing
      )

      UploadedTransferMapper.fromValidatedRow(row) mustBe Left(
        UploadedTransferMappingError(
          rowNumber = 3,
          fieldName = "whatTypeOfSecurities",
          reason = "Required value was missing after validation"
        )
      )
    }

    "map non-UK seller addresses" in {
      val row = validParsedRow(
        sellerAddressInUk = ParsedValue.Valid(false),
        sellerAddressLine1 = ParsedValue.Missing,
        sellerAddressLine2 = ParsedValue.Missing,
        sellerPostcode = ParsedValue.Missing,
        sellerCountry = ParsedValue.Valid("FR")
      )

      val result = UploadedTransferMapper.fromValidatedRow(row).toOption.get

      result.sellerAddress mustBe UploadedSellerAddress.NonUkAddress("FR")
    }

    "map relief as None when applyingForRelief is false" in {
      val row = validParsedRow(
        applyingForRelief = ParsedValue.Valid(false),
        whatReliefAreYouApplyingFor = ParsedValue.Missing
      )

      val result = UploadedTransferMapper.fromValidatedRow(row).toOption.get

      result.relief mustBe None
    }

    "map totalMarketValue as None when missing" in {
      val row = validParsedRow(
        totalMarketValue = ParsedValue.Missing
      )

      val result = UploadedTransferMapper.fromValidatedRow(row).toOption.get

      result.totalMarketValue mustBe None
    }
  }

  "UploadedTransferMapper.fromValidatedRows" must {

    "map all rows when all are valid" in {
      val rows = Seq(
        validParsedRow(rowNumber = 3),
        validParsedRow(
          rowNumber = 4,
          whatTypeOfSecurities = ParsedValue.Valid("Loan notes or other debt securities"),
          typeOfShares = ParsedValue.Missing
        )
      )

      UploadedTransferMapper.fromValidatedRows(rows).toOption.get.map(_.rowNumber) mustBe Seq(3, 4)
    }

    "return mapping errors when any row cannot be mapped" in {
      val rows = Seq(
        validParsedRow(rowNumber = 3),
        validParsedRow(rowNumber = 4, whatTypeOfSecurities = ParsedValue.Missing)
      )

      UploadedTransferMapper.fromValidatedRows(rows) mustBe Left(
        Seq(
          UploadedTransferMappingError(
            rowNumber = 4,
            fieldName = "whatTypeOfSecurities",
            reason = "Required value was missing after validation"
          )
        )
      )
    }
  }

  private def validParsedRow(
                              rowNumber: Int = 3,
                              sellerName: ParsedValue[String] = ParsedValue.Valid("Seller Ltd"),
                              sellerAddressInUk: ParsedValue[Boolean] = ParsedValue.Valid(true),
                              sellerAddressLine1: ParsedValue[String] = ParsedValue.Valid("1 Seller Street"),
                              sellerAddressLine2: ParsedValue[String] = ParsedValue.Valid("Seller Town"),
                              sellerAddressLine3: ParsedValue[String] = ParsedValue.Missing,
                              sellerAddressLine4: ParsedValue[String] = ParsedValue.Missing,
                              sellerPostcode: ParsedValue[String] = ParsedValue.Valid("AA1 1AA"),
                              sellerCountry: ParsedValue[String] = ParsedValue.Missing,
                              connectedPersons: ParsedValue[Boolean] = ParsedValue.Valid(true),
                              applyingForRelief: ParsedValue[Boolean] = ParsedValue.Valid(true),
                              whatReliefAreYouApplyingFor: ParsedValue[String] = ParsedValue.Valid("Charities Relief"),
                              securitiesTarget: ParsedValue[String] = ParsedValue.Valid("Target Ltd"),
                              companyRegistrationNumber: ParsedValue[String] = ParsedValue.Valid("12345678"),
                              chargingPoint: ParsedValue[java.time.LocalDate] = ParsedValue.Valid(LocalDate.of(2025, 11, 20)),
                              taxRate: ParsedValue[BigDecimal] = ParsedValue.Valid(BigDecimal("0.5")),
                              whatTypeOfSecurities: ParsedValue[String] = ParsedValue.Valid("shares"),
                              typeOfShares: ParsedValue[String] = ParsedValue.Valid("Ordinary Shares"),
                              securitiesQuantity: ParsedValue[BigDecimal] = ParsedValue.Valid(BigDecimal(100)),
                              amountPaidForSecurities: ParsedValue[BigDecimal] = ParsedValue.Valid(BigDecimal("1234.56")),
                              totalMarketValue: ParsedValue[BigDecimal] = ParsedValue.Valid(BigDecimal("2000"))
                            ): ParsedStcRow =
    ParsedStcRow(
      rowNumber = rowNumber,
      sellerName = sellerName,
      sellerAddressInUk = sellerAddressInUk,
      sellerAddressLine1 = sellerAddressLine1,
      sellerAddressLine2 = sellerAddressLine2,
      sellerAddressLine3 = sellerAddressLine3,
      sellerAddressLine4 = sellerAddressLine4,
      sellerPostcode = sellerPostcode,
      sellerCountry = sellerCountry,
      connectedPersons = connectedPersons,
      applyingForRelief = applyingForRelief,
      whatReliefAreYouApplyingFor = whatReliefAreYouApplyingFor,
      securitiesTarget = securitiesTarget,
      companyRegistrationNumber = companyRegistrationNumber,
      chargingPoint = chargingPoint,
      taxRate = taxRate,
      whatTypeOfSecurities = whatTypeOfSecurities,
      typeOfShares = typeOfShares,
      securitiesQuantity = securitiesQuantity,
      amountPaidForSecurities = amountPaidForSecurities,
      totalMarketValue = totalMarketValue
    )
}