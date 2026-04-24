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

package uk.gov.hmrc.securitiestransferchargefrontend.viewmodels.stf.fileupload

import uk.gov.hmrc.securitiestransferchargefrontend.models.stf.fileupload.StcRowValidationError
import uk.gov.hmrc.securitiestransferchargefrontend.models.stf.UploadedFileError

object UploadedFileErrorMapper {

  def from(errors: Seq[StcRowValidationError]): Seq[UploadedFileError] =
    errors.map(toUploadedFileError)

  private def toUploadedFileError(error: StcRowValidationError): UploadedFileError =
    UploadedFileError(
      cell = cellReference(error),
      error = error.message
    )

  private def cellReference(error: StcRowValidationError): String =
    if (error.columnIndex >= 0) {
      s"${SpreadsheetColumnLetters.fromZeroBasedIndex(error.columnIndex)}${error.rowNumber}"
    } else {
      s"Row ${error.rowNumber}"
    }
}