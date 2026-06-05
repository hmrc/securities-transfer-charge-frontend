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

import org.mockito.ArgumentMatchers.{any, eq => eqTo}
import org.mockito.Mockito.{verify, when}
import org.scalatest.EitherValues
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.mockito.MockitoSugar
import uk.gov.hmrc.securitiestransferchargefrontend.models.stf.fileupload.FileParseError.UnsupportedMimeType
import uk.gov.hmrc.securitiestransferchargefrontend.models.stf.fileupload.{FileParseError, ParsedRow, UploadedFile}
import uk.gov.hmrc.securitiestransferchargefrontend.services.fileupload.{FileParser, FileParserSelector, FileParsingService}

import java.io.ByteArrayInputStream
import java.nio.charset.StandardCharsets

class FileParsingServiceSpec extends AnyWordSpec with Matchers with EitherValues with MockitoSugar {

  private val uploadedFile = UploadedFile(
    fileName = "test.csv",
    mimeType = "text/csv",
    inputStream = new ByteArrayInputStream("header\nvalue".getBytes(StandardCharsets.UTF_8))
  )

  private val dummyBlock: (Seq[String], Iterator[ParsedRow]) => Either[FileParseError, String] =
    (_, _) => Right("Stream processed successfully")

  "withParsedStream" should {

    "select the parser and delegate the streaming block successfully" in {
      val fileParserSelector = mock[FileParserSelector]
      val parser             = mock[FileParser]
      val service            = new FileParsingService(fileParserSelector)

      when(fileParserSelector.select("text/csv")).thenReturn(Right(parser))

      when(parser.withParsedStream[String](eqTo(uploadedFile))(any()))
        .thenReturn(Right("Stream Processed Successfully"))

      service.withParsedStream(uploadedFile)(dummyBlock).value shouldBe "Stream Processed Successfully"

      verify(fileParserSelector).select("text/csv")
      verify(parser).withParsedStream[String](eqTo(uploadedFile))(any())
    }

    "return the selector error when the mime type is unsupported" in {
      val fileParserSelector = mock[FileParserSelector]
      val service            = new FileParsingService(fileParserSelector)

      when(fileParserSelector.select("text/csv")).thenReturn(Left(UnsupportedMimeType("text/csv")))

      service.withParsedStream(uploadedFile)(dummyBlock).left.value shouldBe UnsupportedMimeType("text/csv")

      verify(fileParserSelector).select("text/csv")
    }
  }
}