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


import ParsedValue._

object SafeRowReader {

  def readString(row: ParsedRow, idx: Option[Int]): Option[String] =
    idx.flatMap(i => unwrap(ParsedRowReader.readString(row, i)))
      .filter(_.nonEmpty)

  def readBoolean(row: ParsedRow, idx: Option[Int]): Option[Boolean] =
    idx.flatMap(i => unwrap(ParsedRowReader.readBoolean(row, i)))

  def readBigDecimal(row: ParsedRow, idx: Option[Int]): Option[BigDecimal] =
    idx.flatMap(i => unwrap(ParsedRowReader.readBigDecimal(row, i)))

  def readTaxRate(row: ParsedRow, idx: Option[Int]): Option[BigDecimal] =
    idx.flatMap(i => unwrap(ParsedRowReader.readTaxRate(row, i)).map(_.value))

  def readDate(row: ParsedRow, idx: Option[Int]): Option[LocalDate] =
    idx.flatMap(i => unwrap(ParsedRowReader.readDate(row, i)))

  private def unwrap[A](parsed: ParsedValue[A]): Option[A] =
    parsed match {
      case Valid(value) => Some(value)
      case _            => None
    }
}