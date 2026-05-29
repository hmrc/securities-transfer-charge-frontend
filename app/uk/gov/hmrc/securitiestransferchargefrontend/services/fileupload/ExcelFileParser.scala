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

import com.github.pjfanning.xlsx.StreamingReader
import uk.gov.hmrc.securitiestransferchargefrontend.config.FileUploadConfig
import org.apache.poi.ss.usermodel.{Cell, CellType, DateUtil, Row, Workbook}
import uk.gov.hmrc.securitiestransferchargefrontend.models.stf.fileupload.FileParseError.{InvalidXlsx, MissingWorksheet}
import uk.gov.hmrc.securitiestransferchargefrontend.models.stf.fileupload.{FileParseError, ParsedCell, ParsedRow, UploadedFile}

import java.time.ZoneId
import java.time.format.DateTimeFormatter
import javax.inject.{Inject, Singleton}
import scala.jdk.CollectionConverters.*

@Singleton
class ExcelFileParser @Inject()(config: FileUploadConfig) extends FileParser {

  private val dateFormatter = DateTimeFormatter.ISO_LOCAL_DATE

  private def isEmptyRow(row: Row): Boolean =
    row.cellIterator().asScala.forall { cell =>
      extractCellValue(cell).isEmpty
    }

  override def withParsedStream[A](file: UploadedFile)(block: (Seq[String], Iterator[ParsedRow]) => Either[FileParseError, A]): Either[FileParseError, A] = {
    try {
      val workbook: Workbook = StreamingReader.builder()
        .rowCacheSize(100)
        .bufferSize(4096)
        .open(file.inputStream)

      try {
        val sheet = Option(workbook.getSheet(config.expectedWorksheetName))
          .orElse(Option(workbook.getSheetAt(0)))
          .toRight(MissingWorksheet(config.expectedWorksheetName))

        sheet match {
          case Left(error) => Left(error)
          case Right(worksheet) =>
            val rowIterator = worksheet.iterator().asScala.filterNot(isEmptyRow)

            if (!rowIterator.hasNext) {
              Left(InvalidXlsx("The file is empty or only contains empty rows"))
            } else {
              val headerRow = rowIterator.next()
              val headers = (0 until config.maxColumns).map { i =>
                Option(headerRow.getCell(i)).map(extractCellValue).getOrElse("").trim
              }

              val lazyParsedRows = rowIterator.map { row =>
                ParsedRow(
                  rowNumber = row.getRowNum + 1,
                  cells = (0 until config.maxColumns).map { i =>
                    val value = Option(row.getCell(i)).map(extractCellValue).getOrElse("").trim
                    ParsedCell(i, value)
                  }
                )
              }

              block(headers, lazyParsedRows)
            }
        }
      } finally {
        workbook.close()
        file.inputStream.close()
      }
    } catch {
      case _: Exception => Left(InvalidXlsx(s"Unable to parse file ${file.fileName}"))
    }
  }

  private def extractCellValue(cell: Cell): String =
    cell.getCellType match {
      case CellType.STRING => cell.getStringCellValue
      case CellType.NUMERIC =>
        if (DateUtil.isCellDateFormatted(cell)) {
          cell.getDateCellValue.toInstant
            .atZone(ZoneId.systemDefault())
            .toLocalDate
            .format(dateFormatter)
        } else {
          BigDecimal(cell.getNumericCellValue).bigDecimal.stripTrailingZeros.toPlainString
        }
      case CellType.BOOLEAN => cell.getBooleanCellValue.toString
      case CellType.FORMULA =>
        cell.getCachedFormulaResultType match {
          case CellType.STRING  => cell.getStringCellValue
          case CellType.NUMERIC =>
            if (DateUtil.isCellDateFormatted(cell)) {
              cell.getDateCellValue.toInstant
                .atZone(ZoneId.systemDefault())
                .toLocalDate
                .format(dateFormatter)
            } else {
              BigDecimal(cell.getNumericCellValue).bigDecimal.stripTrailingZeros.toPlainString
            }
          case CellType.BOOLEAN => cell.getBooleanCellValue.toString
          case _                => ""
        }
      case CellType.BLANK => ""
      case _              => ""
    }
}