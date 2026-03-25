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

import uk.gov.hmrc.securitiestransferchargefrontend.models.fileupload.ParsedRow

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import scala.util.Try

object ParsedRowReader {

  def readString(row: ParsedRow, columnIndex: Int): Option[String] =
    row.valueAt(columnIndex).map(_.trim).filter(_.nonEmpty)

  def readBigDecimal(row: ParsedRow, columnIndex: Int): Option[BigDecimal] =
    readString(row, columnIndex)
      .map(
        _.replace(",", "")
          .replace("£", "")
          .replace("%", "")
          .trim
      )
      .filter(_.nonEmpty)
      .flatMap(value => Try(BigDecimal(value)).toOption)

  def readBoolean(row: ParsedRow, columnIndex: Int): Option[Boolean] =
    readString(row, columnIndex).flatMap {
      case s if Set("true", "yes", "y").contains(s.toLowerCase)  => Some(true)
      case s if Set("false", "no", "n").contains(s.toLowerCase)  => Some(false)
      case _                                                     => None
    }

  def readDate(row: ParsedRow, columnIndex: Int): Option[LocalDate] = {
    val formatters = Seq(
      DateTimeFormatter.ISO_LOCAL_DATE,
      DateTimeFormatter.ofPattern("d/M/uuuu"),
      DateTimeFormatter.ofPattern("dd/MM/uuuu"),
      DateTimeFormatter.ofPattern("d M uuuu"),
      DateTimeFormatter.ofPattern("dd MM uuuu")
    )

    readString(row, columnIndex).flatMap { value =>
      formatters.view.flatMap { formatter =>
        Try(LocalDate.parse(value.trim, formatter)).toOption
      }.headOption
    }
  }
}