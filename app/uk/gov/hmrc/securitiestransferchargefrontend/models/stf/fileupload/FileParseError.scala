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

package uk.gov.hmrc.securitiestransferchargefrontend.models.stf.fileupload

sealed trait FileParseError {
  def message: String
}

object FileParseError {

  final case class UnsupportedMimeType(mimeType: String) extends FileParseError {
    override val message: String = s"Unsupported file type: $mimeType"
  }

  final case class InvalidCsv(reason: String) extends FileParseError {
    override val message: String = s"Invalid CSV file: $reason"
  }

  final case class InvalidXlsx(reason: String) extends FileParseError {
    override val message: String = s"Invalid XLSX file: $reason"
  }

  final case class MissingWorksheet(sheetName: String) extends FileParseError {
    override val message: String = s"Missing expected worksheet: $sheetName"
  }

  final case class RowLimitExceeded(actual: Int, max: Int) extends FileParseError {
    override val message: String = s"File contains $actual rows, maximum allowed is $max"
  }

  final case class Unexpected(reason: String) extends FileParseError {
    override val message: String = s"Unexpected parsing error: $reason"
  }

  case object EmptyFile extends FileParseError {
    override val message: String = "The uploaded file is empty"
  }

  case object InvalidTemplate extends FileParseError {
    override val message: String = "The template file has been altered"
  }
}