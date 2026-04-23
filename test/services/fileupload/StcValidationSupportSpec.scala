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
import uk.gov.hmrc.securitiestransferchargefrontend.models.fileupload.{ParsedCell, ParsedRow}
import uk.gov.hmrc.securitiestransferchargefrontend.services.fileupload.StcValidationSupport

class StcValidationSupportSpec extends AnyWordSpec with Matchers {

  private val support = new StcValidationSupport

  "StcValidationSupport.validateRequiredText" must {

    "return the required message when the value is blank" in {
      val result = support.validateRequiredText(
        rawRow(columnIndex = 9, value = ""),
        columnIndex = 9,
        fieldName = "sellerAddressLine1",
        requiredMessage = "Enter the first line of your address"
      )

      result.map(_.message) mustBe Seq("Enter the first line of your address")
    }

    "return the provided length message when the value is too long" in {
      val result = support.validateRequiredText(
        rawRow(columnIndex = 9, value = "a" * 51),
        columnIndex = 9,
        fieldName = "sellerAddressLine1",
        requiredMessage = "Enter the first line of your address",
        maxLength = Some(50),
        lengthMessage = Some("Address line 1 must be 50 characters or fewer")
      )

      result.map(_.message) mustBe Seq("Address line 1 must be 50 characters or fewer")
    }

    "return the provided invalid character message when the value does not match the regex" in {
      val result = support.validateRequiredText(
        rawRow(columnIndex = 9, value = "Address @@@"),
        columnIndex = 9,
        fieldName = "sellerAddressLine1",
        requiredMessage = "Enter the first line of your address",
        pattern = Some(support.addressPattern),
        invalidMessage = Some("Address line 1 can only include letters, numbers and the following characters: , . - '")
      )

      result.map(_.message) mustBe Seq("Address line 1 can only include letters, numbers and the following characters: , . - '")
    }

    "return both length and invalid character errors when both rules are broken and both messages are supplied" in {
      val result = support.validateRequiredText(
        rawRow(columnIndex = 9, value = ("@" * 51)),
        columnIndex = 9,
        fieldName = "sellerAddressLine1",
        requiredMessage = "Enter the first line of your address",
        maxLength = Some(50),
        lengthMessage = Some("Address line 1 must be 50 characters or fewer"),
        pattern = Some(support.addressPattern),
        invalidMessage = Some("Address line 1 can only include letters, numbers and the following characters: , . - '")
      )

      result.map(_.message) mustBe Seq(
        "Address line 1 must be 50 characters or fewer",
        "Address line 1 can only include letters, numbers and the following characters: , . - '"
      )
    }
  }

  "StcValidationSupport.validateOptionalText" must {

    "return no errors for blank optional values" in {
      val result = support.validateOptionalText(
        rawRow(columnIndex = 10, value = ""),
        columnIndex = 10,
        fieldName = "sellerAddressLine2",
        maxLength = Some(50),
        lengthMessage = Some("Address line 2 must be fewer than 50 characters long")
      )

      result mustBe Seq.empty
    }

    "validate non-blank optional values" in {
      val result = support.validateOptionalText(
        rawRow(columnIndex = 10, value = "a" * 51),
        columnIndex = 10,
        fieldName = "sellerAddressLine2",
        maxLength = Some(50),
        lengthMessage = Some("Address line 2 must be fewer than 50 characters long")
      )

      result.map(_.message) mustBe Seq("Address line 2 must be fewer than 50 characters long")
    }

    "validate invalid characters for non-blank optional values when an invalid message is supplied" in {
      val result = support.validateOptionalText(
        rawRow(columnIndex = 10, value = "Address @@@"),
        columnIndex = 10,
        fieldName = "sellerAddressLine2",
        pattern = Some(support.addressPattern),
        invalidMessage = Some("Address line 2 can only include letters, numbers and the following characters: , . - '")
      )

      result.map(_.message) mustBe Seq("Address line 2 can only include letters, numbers and the following characters: , . - '")
    }
    
  }

  "StcValidationSupport.looksLikeUkPostcode" must {

    "return true for valid postcodes" in {
      support.looksLikeUkPostcode("AA1 1AA") mustBe true
      support.looksLikeUkPostcode("SW1A 1AA") mustBe true
      support.looksLikeUkPostcode("EC1A 1BB") mustBe true
    }

    "return false for invalid postcodes" in {
      support.looksLikeUkPostcode("not a postcode") mustBe false
      support.looksLikeUkPostcode("12345") mustBe false
      support.looksLikeUkPostcode("") mustBe false
    }
  }

  "StcValidationSupport.columnIndex" must {

    "return the configured spreadsheet column index for known fields" in {
      support.columnIndex("sellerName") mustBe 7
      support.columnIndex("sellerAddressInUk") mustBe 8
      support.columnIndex("sellerPostcode") mustBe 13
    }

    "return -1 for unknown fields" in {
      support.columnIndex("doesNotExist") mustBe -1
    }
  }

  private def rawRow(columnIndex: Int, value: String): ParsedRow =
    ParsedRow(
      rowNumber = 3,
      cells = Seq(ParsedCell(columnIndex, value))
    )
}