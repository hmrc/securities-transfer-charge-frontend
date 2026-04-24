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

import uk.gov.hmrc.securitiestransferchargefrontend.models.stf.fileupload.{ParsedRow, ParsedValue}

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import scala.util.Try

object ParsedRowReader {

  def readString(row: ParsedRow, columnIndex: Int): ParsedValue[String] =
    row.valueAt(columnIndex) match {
      case None => ParsedValue.Missing
      case Some(raw) if raw.trim.isEmpty => ParsedValue.Missing
      case Some(raw) => ParsedValue.Valid(raw.trim)
    }

  def readBigDecimal(row: ParsedRow, columnIndex: Int): ParsedValue[BigDecimal] =
    row.valueAt(columnIndex) match {
      case None => ParsedValue.Missing
      case Some(raw) if raw.trim.isEmpty => ParsedValue.Missing
      case Some(raw) =>
        val cleaned =
          raw.trim
            .replace(",", "")
            .replace("£", "")
            .replace("%", "")

        Try(BigDecimal(cleaned)).toOption match {
          case Some(value) => ParsedValue.Valid(value)
          case None        => ParsedValue.Invalid(raw, "not a number")
        }
    }

  def readBoolean(row: ParsedRow, columnIndex: Int): ParsedValue[Boolean] =
    row.valueAt(columnIndex) match {
      case None => ParsedValue.Missing
      case Some(raw) if raw.trim.isEmpty => ParsedValue.Missing
      case Some(raw) =>
        raw.trim.toLowerCase match {
          case "true" | "yes" | "y"  => ParsedValue.Valid(true)
          case "false" | "no" | "n"  => ParsedValue.Valid(false)
          case _                     => ParsedValue.Invalid(raw, "not a recognised boolean")
        }
    }

  def readDate(row: ParsedRow, columnIndex: Int): ParsedValue[LocalDate] = {
    val formatters = Seq(
      DateTimeFormatter.ISO_LOCAL_DATE,
      DateTimeFormatter.ofPattern("yyyy/M/d"),
      DateTimeFormatter.ofPattern("yyyy/MM/dd"),
      DateTimeFormatter.ofPattern("d/M/uuuu"),
      DateTimeFormatter.ofPattern("dd/MM/uuuu"),
      DateTimeFormatter.ofPattern("d M uuuu"),
      DateTimeFormatter.ofPattern("dd MM uuuu")
    )

    row.valueAt(columnIndex) match {
      case None => ParsedValue.Missing
      case Some(raw) if raw.trim.isEmpty => ParsedValue.Missing
      case Some(raw) =>
        formatters.view.flatMap { formatter =>
          Try(LocalDate.parse(raw.trim, formatter)).toOption
        }.headOption match {
          case Some(date) => ParsedValue.Valid(date)
          case None       => ParsedValue.Invalid(raw, "not a valid date")
        }
    }
  }
}