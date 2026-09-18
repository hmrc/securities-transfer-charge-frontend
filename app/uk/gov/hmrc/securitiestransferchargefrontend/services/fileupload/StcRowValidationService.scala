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

import uk.gov.hmrc.securitiestransferchargefrontend.models.JourneyType
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
                      journeyType: JourneyType,
                      maxErrorsAllowed: Int,
                      maxRows: Int
                    ): Either[(FileParseError, Long), (Seq[ValidatedStcRow], Long)] = {

    implicit val columnIndex: ColumnIndexBuilder = new ColumnIndexBuilder(headers)
    val mapper = new StcRowMapper(columnIndex)

    val startedProcessor = Processor().start()

    val resolvedTemplate = (journeyType, affinityKey.toLowerCase) match {
      case (JourneyType.STF, "agent") => Right(StcTemplate.STFAgent)
      case (JourneyType.STF, _) => Right(StcTemplate.STF)
      case (JourneyType.SH03, _) => Right(StcTemplate.SH03)
      case _ => Left(FileParseError.InvalidTemplate, startedProcessor.stop().processingTime)
    }
    

    resolvedTemplate.flatMap { template =>
      @tailrec
      def processRows(
                       accumulated: List[ValidatedStcRow],
                       blockingErrorCount: Int,
                       processedRowCount: Int
                     ): Either[(FileParseError, Long), (Seq[ValidatedStcRow], Long)] = {

        if (processedRowCount > maxRows) {
          val elapsedTime = startedProcessor.stop().processingTime
          Left(FileParseError.RowLimitExceeded(processedRowCount, maxRows), elapsedTime)
        } else if (blockingErrorCount > maxErrorsAllowed || !rowStream.hasNext) {
          val elapsedTime = startedProcessor.stop().processingTime
          Right(accumulated.reverse, elapsedTime)
        } else {
          val parsedRow = mapper.map(rowStream.next())

          val errors =
            (stcBasicRowValidator.validate(parsedRow, template, affinityKey, journeyType) ++
              stcConditionalRowValidator.validate(parsedRow, template, affinityKey, journeyType))
                .sortBy(_.columnIndex)

          val updatedErrorCount = blockingErrorCount + errors.count(_.blocking)

          processRows(ValidatedStcRow(parsedRow, errors) :: accumulated, updatedErrorCount, processedRowCount + 1)
        }
      }

      processRows(Nil, 0, 0)
    }
  }
}

case class Processor(startTime: Long = 0L, processingTime: Long = 0L) {
  def start(): Processor = copy(startTime = System.nanoTime())
  def stop(): Processor = copy(processingTime = System.nanoTime() - startTime)
}
