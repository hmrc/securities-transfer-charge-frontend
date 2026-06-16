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

package services.fileupload

import org.scalatest.EitherValues
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.mockito.MockitoSugar
import uk.gov.hmrc.securitiestransferchargefrontend.config.FrontendAppConfig
import uk.gov.hmrc.securitiestransferchargefrontend.models.stf.fileupload.FileParseError.UnsupportedMimeType
import uk.gov.hmrc.securitiestransferchargefrontend.services.fileupload.{CsvFileParser, ExcelFileParser, FileParserSelector}

class FileParserSelectorSpec extends AnyWordSpec with Matchers with EitherValues with MockitoSugar {

  private val mockAppConfig = mock[FrontendAppConfig]

  private val csvParser   = new CsvFileParser
  private val excelParser = new ExcelFileParser(TestFileUploadConfig.config(), mockAppConfig)
  private val selector    = new FileParserSelector(csvParser, excelParser)

  "select" should {

    "return the CSV parser for text/csv" in {
      selector.select("text/csv").value shouldBe csvParser
    }

    "return the Excel parser for XLSX mime type" in {
      selector
        .select("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
        .value shouldBe excelParser
    }

    "return an UnsupportedMimeType error for an unsupported mime type" in {
      selector.select("application/pdf").left.value shouldBe UnsupportedMimeType("application/pdf")
    }
  }
}