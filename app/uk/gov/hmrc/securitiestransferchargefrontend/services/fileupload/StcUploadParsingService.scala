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
import uk.gov.hmrc.securitiestransferchargefrontend.models.stf.fileupload.{FileParseError, ParsedRow, UploadedFile}

import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import javax.inject.{Inject, Singleton}

@Singleton
class StcUploadParsingService @Inject()(
                                         fileUploadConfig: FileUploadConfig,
                                         fileParsingService: FileParsingService
                                       ) {

  def withValidatedStream[A](uploadedFile: UploadedFile, affinityKey: String)(block: (Seq[String], Iterator[ParsedRow]) => Either[FileParseError, A]): Either[FileParseError, A] = {
    fileParsingService.withParsedStream(uploadedFile) { (headers, lazyRowIterator) =>

      val expectedRow1 = fileUploadConfig.expectedTemplateHash(affinityKey, "stf", 1)
      val expectedRow2 = fileUploadConfig.expectedTemplateHash(affinityKey, "stf", 2)
      val expectedRow3 = fileUploadConfig.expectedTemplateHash(affinityKey, "stf", 3)

      val row1Valid = hashRow(headers) == expectedRow1

      val templateRowsCount = fileUploadConfig.firstDataRow - 2
      val prepRows = (1 to templateRowsCount).flatMap(_ => if (lazyRowIterator.hasNext) Some(lazyRowIterator.next()) else None).toList

      val row2Valid = prepRows.find(_.rowNumber == 2).exists(r => hashRow(r.cells.sortBy(_.columnIndex).map(_.rawValue)) == expectedRow2)
      val row3Valid = prepRows.find(_.rowNumber == 3).exists(r => hashRow(r.cells.sortBy(_.columnIndex).map(_.rawValue)) == expectedRow3)

      if (!(row1Valid && row2Valid && row3Valid)) {
        Left(FileParseError.InvalidTemplate)
      } else {
        val dataStream = lazyRowIterator.filterNot(_.isCompletelyEmpty)

        if (!dataStream.hasNext) {
          Left(FileParseError.EmptyFile)
        } else {
          block(headers, dataStream)
        }
      }
    }
  }

  private def hashRow(cells: Seq[String]): String = {
    val normalisedString = cells.map(_.trim).mkString("|")
    val digest = MessageDigest.getInstance("SHA-256")
    val hashBytes = digest.digest(normalisedString.getBytes(StandardCharsets.UTF_8))
    hashBytes.map("%02x".format(_)).mkString
  }
}