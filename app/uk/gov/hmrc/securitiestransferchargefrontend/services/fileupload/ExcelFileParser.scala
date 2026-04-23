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
import org.apache.poi.ss.usermodel.{Cell, CellType, DateUtil}
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import uk.gov.hmrc.securitiestransferchargefrontend.models.stf.fileupload.FileParseError.{InvalidXlsx, MissingWorksheet, RowLimitExceeded}
import uk.gov.hmrc.securitiestransferchargefrontend.models.stf.fileupload.{FileParseError, ParsedCell, ParsedFile, ParsedRow, UploadedFile}

import java.time.ZoneId
import java.time.format.DateTimeFormatter
import javax.inject.{Inject, Singleton}
import scala.jdk.CollectionConverters.*
import scala.util.Try

@Singleton
class ExcelFileParser @Inject()(config: FileUploadConfig) extends FileParser {

  private val dateFormatter = DateTimeFormatter.ISO_LOCAL_DATE

  override def parse(file: UploadedFile): Either[FileParseError, ParsedFile] =
    Try {
      val workbook = new XSSFWorkbook(file.inputStream)

      try {
        val sheet =
          Option(workbook.getSheet(config.expectedWorksheetName))
            .orElse(Option(workbook.getSheetAt(0)))
            .toRight(MissingWorksheet(config.expectedWorksheetName))

        sheet.flatMap { worksheet =>
          val rows = worksheet.iterator().asScala.toSeq

          if (rows.size > config.maxRows) {
            Left(RowLimitExceeded(rows.size, config.maxRows))
          } else {
            val parsedRows = rows.zipWithIndex.map { case (row, index) =>
              ParsedRow(
                rowNumber = index + 1,
                cells = (0 until config.maxColumns).map { cellIndex =>
                  val rawValue = Option(row.getCell(cellIndex))
                    .map(extractCellValue)
                    .getOrElse("")

                  ParsedCell(cellIndex, rawValue.trim)
                }
              )
            }

            Right(
              ParsedFile(
                fileName = file.fileName,
                mimeType = file.mimeType,
                rows = parsedRows
              )
            )
          }
        }
      } finally {
        workbook.close()
        file.inputStream.close()
      }
    }.getOrElse {
      Left(InvalidXlsx(s"Unable to parse file ${file.fileName}"))
    }

  private def extractCellValue(cell: Cell): String =
    cell.getCellType match {
      case CellType.STRING =>
        cell.getStringCellValue

      case CellType.NUMERIC =>
        if (DateUtil.isCellDateFormatted(cell)) {
          cell.getDateCellValue.toInstant
            .atZone(ZoneId.systemDefault())
            .toLocalDate
            .format(dateFormatter)
        } else {
          BigDecimal(cell.getNumericCellValue).bigDecimal.stripTrailingZeros.toPlainString
        }

      case CellType.BOOLEAN =>
        cell.getBooleanCellValue.toString

      case CellType.FORMULA =>
        cell.getCachedFormulaResultType match {
          case CellType.STRING =>
            cell.getStringCellValue

          case CellType.NUMERIC =>
            if (DateUtil.isCellDateFormatted(cell)) {
              cell.getDateCellValue.toInstant
                .atZone(ZoneId.systemDefault())
                .toLocalDate
                .format(dateFormatter)
            } else {
              BigDecimal(cell.getNumericCellValue).bigDecimal.stripTrailingZeros.toPlainString
            }

          case CellType.BOOLEAN =>
            cell.getBooleanCellValue.toString

          case _ =>
            ""
        }

      case CellType.BLANK =>
        ""

      case _ =>
        ""
    }
}