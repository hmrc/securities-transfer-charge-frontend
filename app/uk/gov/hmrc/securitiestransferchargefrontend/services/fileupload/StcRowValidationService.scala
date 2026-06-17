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

package uk.gov.hmrc.securitiestransferchargefrontend.services.fileupload

import uk.gov.hmrc.securitiestransferchargefrontend.models.stf.fileupload.{FileParseError, ParsedRow, ValidatedStcRow}

import javax.inject.{Inject, Singleton}
import scala.annotation.tailrec

@Singleton
class StcRowValidationService @Inject()(
                                         stcBasicRowValidator: StcBasicRowValidator,
                                         stcConditionalRowValidator: StcConditionalRowValidator
                                       ) {

  def validateStream(
                      rowStream: Iterator[ParsedRow],
                      headers: Seq[String],
                      affinityKey: String,
                      templateType: String,
                      maxErrorsAllowed: Int,
                      maxRows: Int
                    ): Either[FileParseError, Seq[ValidatedStcRow]] = {

    implicit val columnIndex: ColumnIndexBuilder = new ColumnIndexBuilder(headers)
    val mapper = new StcRowMapper(columnIndex)

    val resolvedTemplate = templateType.toLowerCase match {
      case "stf"  => Right(StcTemplate.STF)
      case "sh03" => Right(StcTemplate.SH03)
      case _      => Left(FileParseError.InvalidTemplate)
    }

    resolvedTemplate.flatMap { template =>
      @tailrec
      def processRows(
                       accumulated: List[ValidatedStcRow],
                       blockingErrorCount: Int,
                       processedRowCount: Int
                     ): Either[FileParseError, Seq[ValidatedStcRow]] = {

        if (processedRowCount > maxRows) {
          Left(FileParseError.RowLimitExceeded(processedRowCount, maxRows))
        } else if (blockingErrorCount > maxErrorsAllowed || !rowStream.hasNext) {
          Right(accumulated.reverse)
        } else {
          val parsedRow = mapper.map(rowStream.next())

          val errors =
            (stcBasicRowValidator.validate(parsedRow, template, affinityKey) ++
              stcConditionalRowValidator.validate(parsedRow, template, affinityKey))
                .sortBy(_.columnIndex)

          val updatedErrorCount = blockingErrorCount + errors.count(_.blocking)

          processRows(ValidatedStcRow(parsedRow, errors) :: accumulated, updatedErrorCount, processedRowCount + 1)
        }
      }

      processRows(Nil, 0, 0)
    }
  }
}
