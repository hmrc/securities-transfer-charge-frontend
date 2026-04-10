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

import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{verify, when}
import org.scalatest.EitherValues
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.mockito.MockitoSugar
import uk.gov.hmrc.securitiestransferchargefrontend.models.fileupload.FileParseError.UnsupportedMimeType
import uk.gov.hmrc.securitiestransferchargefrontend.models.fileupload.{ParsedFile, UploadedFile}
import uk.gov.hmrc.securitiestransferchargefrontend.services.fileupload.FileParserSelector
import uk.gov.hmrc.securitiestransferchargefrontend.services.fileupload.FileParser
import uk.gov.hmrc.securitiestransferchargefrontend.services.fileupload.FileParsingService

import java.io.ByteArrayInputStream
import java.nio.charset.StandardCharsets

class FileParsingServiceSpec extends AnyWordSpec with Matchers with EitherValues with MockitoSugar {

  private val uploadedFile = UploadedFile(
    fileName = "test.csv",
    mimeType = "text/csv",
    inputStream = new ByteArrayInputStream("header\nvalue".getBytes(StandardCharsets.UTF_8))
  )

  private val parsedFile = ParsedFile(
    fileName = "test.csv",
    mimeType = "text/csv",
    rows = Seq()
  )

  "parse" should {

    "select the parser and delegate parsing successfully" in {
      val fileParserSelector = mock[FileParserSelector]
      val parser             = mock[FileParser]
      val service            = new FileParsingService(fileParserSelector)

      when(fileParserSelector.select("text/csv")).thenReturn(Right(parser))
      when(parser.parse(any[UploadedFile])).thenReturn(Right(parsedFile))

      service.parse(uploadedFile).value shouldBe parsedFile

      verify(fileParserSelector).select("text/csv")
      verify(parser).parse(uploadedFile)
    }

    "return the selector error when the mime type is unsupported" in {
      val fileParserSelector = mock[FileParserSelector]
      val service            = new FileParsingService(fileParserSelector)

      when(fileParserSelector.select("text/csv")).thenReturn(Left(UnsupportedMimeType("text/csv")))

      service.parse(uploadedFile).left.value shouldBe UnsupportedMimeType("text/csv")

      verify(fileParserSelector).select("text/csv")
    }
  }
}