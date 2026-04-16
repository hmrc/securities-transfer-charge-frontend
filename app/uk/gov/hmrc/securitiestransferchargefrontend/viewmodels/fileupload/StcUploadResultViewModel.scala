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

package uk.gov.hmrc.securitiestransferchargefrontend.viewmodels.fileupload

import uk.gov.hmrc.securitiestransferchargefrontend.models.fileupload.{StcFileValidationResponse, StcRowValidationError, ValidatedStcRow}

final case class StcUploadResultViewModel(
                                           totalRows: Int,
                                           hasErrors: Boolean,
                                           hasBlockingErrors: Boolean,
                                           blockingErrorDisplays: Seq[UploadErrorDisplay],
                                           nonBlockingErrorDisplays: Seq[UploadErrorDisplay],
                                           validatedRows: Seq[ValidatedStcRow]
                                         )

object StcUploadResultViewModel {

  def from(response: StcFileValidationResponse): StcUploadResultViewModel =
    StcUploadResultViewModel(
      totalRows = response.rows.size,
      hasErrors = response.hasErrors,
      hasBlockingErrors = response.hasBlockingErrors,
      blockingErrorDisplays = response.blockingErrors.map(toDisplayError),
      nonBlockingErrorDisplays = response.nonBlockingErrors.map(toDisplayError),
      validatedRows = response.rows
    )

  private def toDisplayError(error: StcRowValidationError): UploadErrorDisplay = {
    val fieldMetadata = StcUploadFieldMetadata.byFieldName.get(error.fieldName)

    UploadErrorDisplay(
      cellReference =
        if (error.columnIndex >= 0) {
          s"${SpreadsheetColumnLetters.fromZeroBasedIndex(error.columnIndex)}${error.rowNumber}"
        } else {
          s"Row ${error.rowNumber}"
        },
      questionLabel = fieldMetadata.map(_.questionLabel).getOrElse(error.fieldName),
      message = error.message
    )
  }
}