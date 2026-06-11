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

import org.apache.commons.csv.{CSVFormat, CSVParser, CSVRecord}
import uk.gov.hmrc.securitiestransferchargefrontend.config.FileUploadConfig
import uk.gov.hmrc.securitiestransferchargefrontend.models.stf.fileupload.FileParseError.InvalidCsv
import uk.gov.hmrc.securitiestransferchargefrontend.models.stf.fileupload.{FileParseError, ParsedCell, ParsedRow, UploadedFile}

import java.io.InputStreamReader
import java.nio.charset.StandardCharsets
import javax.inject.{Inject, Singleton}
import scala.jdk.CollectionConverters.*

@Singleton
class CsvFileParser @Inject()(config: FileUploadConfig) extends FileParser {

  private val byteOrderMark = "\uFEFF"

  private def isEmptyRecord(record: CSVRecord): Boolean =
    record.iterator().asScala.forall(_.trim.isEmpty)

  override def withParsedStream[A](file: UploadedFile)(block: (Seq[String], Iterator[ParsedRow]) => Either[FileParseError, A]): Either[FileParseError, A] = {
    try {
      val reader = new InputStreamReader(file.inputStream, StandardCharsets.UTF_8)
      val parser = CSVParser.parse(reader, CSVFormat.DEFAULT)

      try {
        val rowIterator = parser.iterator().asScala.filterNot(isEmptyRecord)

        if (!rowIterator.hasNext) {
          Left(InvalidCsv("The file is empty"))
        } else {
          val headerRecord = rowIterator.next()
          val headerValues = headerRecord.iterator().asScala.toSeq

          val headers: Seq[String] = (0 until config.maxColumns).map { colIndex =>
            val rawValue = headerValues.lift(colIndex).getOrElse("").trim
            if (colIndex == 0) rawValue.replace(byteOrderMark, "") else rawValue
          }

          val lazyParsedRows = rowIterator.zipWithIndex.map { case (record, index) =>
            val values = record.iterator().asScala.toSeq
            ParsedRow(
              rowNumber = index + 2,
              cells = (0 until config.maxColumns).map { colIndex =>
                ParsedCell(colIndex, values.lift(colIndex).getOrElse("").trim)
              }
            )
          }

          block(headers, lazyParsedRows)
        }
      } finally {
        parser.close()
        reader.close()
        file.inputStream.close()
      }
    } catch {
      case _: Exception => Left(InvalidCsv(s"Unable to parse file ${file.fileName}"))
    }
  }
}