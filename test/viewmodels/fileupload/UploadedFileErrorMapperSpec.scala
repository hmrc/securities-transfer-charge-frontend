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

package viewmodels.fileupload

import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec
import uk.gov.hmrc.securitiestransferchargefrontend.models.stf.UploadedFileError
import uk.gov.hmrc.securitiestransferchargefrontend.models.stf.fileupload.StcRowValidationError
import uk.gov.hmrc.securitiestransferchargefrontend.services.fileupload.StcUploadColumn
import uk.gov.hmrc.securitiestransferchargefrontend.viewmodels.stf.fileupload.UploadedFileErrorMapper

class UploadedFileErrorMapperSpec extends AnyWordSpec with Matchers {

  "UploadedFileErrorMapper.from" must {

    "map validation errors to uploaded file errors with spreadsheet cell references" in {
      val errors = Seq(
        StcRowValidationError(
          rowNumber = 6,
          fieldName = "sellerName",
          columnIndex = StcUploadColumn.sellerName,
          message = "Enter the seller's full name",
          blocking = true
        ),
        StcRowValidationError(
          rowNumber = 36,
          fieldName = "sellerAddressLine1",
          columnIndex = StcUploadColumn.sellerAddressLine1,
          message = "Enter the first line of your address",
          blocking = true
        )
      )

      UploadedFileErrorMapper.from(errors) mustBe Seq(
        UploadedFileError(
          cell = "H6",
          error = "Enter the seller's full name"
        ),
        UploadedFileError(
          cell = "J36",
          error = "Enter the first line of your address"
        )
      )
    }

    "fall back to row references when the column index is unknown" in {
      val errors = Seq(
        StcRowValidationError(
          rowNumber = 6,
          fieldName = "unknownField",
          columnIndex = -1,
          message = "Something went wrong",
          blocking = true
        )
      )

      UploadedFileErrorMapper.from(errors) mustBe Seq(
        UploadedFileError(
          cell = "Row 6",
          error = "Something went wrong"
        )
      )
    }
  }
}