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

import uk.gov.hmrc.securitiestransferchargefrontend.config.FileUploadConfig
import uk.gov.hmrc.securitiestransferchargefrontend.models.fileupload.FileParseError._
import uk.gov.hmrc.securitiestransferchargefrontend.models.fileupload.{FileParseError, ParsedCell, ParsedFile, ParsedRow, UploadedFile}

import java.io.InputStreamReader
import java.nio.charset.StandardCharsets
import javax.inject.{Inject, Singleton}
import org.apache.commons.csv.{CSVFormat, CSVParser}

import scala.jdk.CollectionConverters._
import scala.util.Try

@Singleton
class CsvFileParser @Inject()(config: FileUploadConfig) extends FileParser {

  override def parse(file: UploadedFile): Either[FileParseError, ParsedFile] =
    Try {
      val reader = new InputStreamReader(file.inputStream, StandardCharsets.UTF_8)
      val parser = CSVParser.parse(
        reader,
        CSVFormat.DEFAULT
      )

      try {
        val records = parser.getRecords.asScala.toSeq

        if (records.size > config.maxRows) {
          Left(RowLimitExceeded(records.size, config.maxRows))
        } else {
          val rows = records.zipWithIndex.map { case (record, index) =>
            ParsedRow(
              rowNumber = index + 1,
              cells = record.iterator().asScala.toSeq.zipWithIndex.map { case (value, colIndex) =>
                ParsedCell(colIndex, Option(value).getOrElse("").trim)
              }
            )
          }

          Right(
            ParsedFile(
              fileName = file.fileName,
              mimeType = file.mimeType,
              rows = rows
            )
          )
        }
      } finally {
        parser.close()
        reader.close()
      }
    }.getOrElse {
      Left(InvalidCsv(s"Unable to parse file ${file.fileName}"))
    }
}