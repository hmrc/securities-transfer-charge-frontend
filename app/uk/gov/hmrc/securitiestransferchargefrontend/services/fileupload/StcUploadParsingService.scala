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

import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import javax.inject.{Inject, Singleton}
import uk.gov.hmrc.auth.core.AffinityGroup
import uk.gov.hmrc.securitiestransferchargefrontend.config.FileUploadConfig
import uk.gov.hmrc.securitiestransferchargefrontend.models.stf.fileupload.{FileParseError, ParsedFile, UploadedFile}

@Singleton
class StcUploadParsingService @Inject()(
                                         fileUploadConfig: FileUploadConfig,
                                         fileParsingService: FileParsingService
                                       ) {

  def parse(uploadedFile: UploadedFile, affinityGroup: AffinityGroup): Either[FileParseError, ParsedFile] =
    fileParsingService.parse(uploadedFile).flatMap { parsedFile =>

      if (!isTemplateValid(parsedFile, affinityGroup)) {
        Left(FileParseError.InvalidTemplate)
      } else {
        val dataRows =
          parsedFile.rows
            .filter(_.rowNumber >= fileUploadConfig.firstDataRow)
            .filterNot(_.isCompletelyEmpty)

        if (dataRows.isEmpty) {
          Left(FileParseError.EmptyFile)
        } else {
          Right(parsedFile.copy(rows = dataRows))
        }
      }
    }

  private def isTemplateValid(parsedFile: ParsedFile, affinityGroup: AffinityGroup): Boolean = {
    val expectedRow1 = fileUploadConfig.expectedTemplateHash(affinityGroup, "stf", 1)
    val expectedRow2 = fileUploadConfig.expectedTemplateHash(affinityGroup, "stf", 2)
    val expectedRow3 = fileUploadConfig.expectedTemplateHash(affinityGroup, "stf", 3)

    val row1Valid = hashRow(parsedFile.headers) == expectedRow1

    def isRowValid(rowNum: Int, expectedHash: String): Boolean = {
      parsedFile.rows.find(_.rowNumber == rowNum) match {
        case Some(row) =>
          val orderedCells = row.cells.sortBy(_.columnIndex).map(_.rawValue)
          hashRow(orderedCells) == expectedHash
        case None =>
          false
      }
    }

    val row2Valid = isRowValid(2, expectedRow2)
    val row3Valid = isRowValid(3, expectedRow3)

    row1Valid && row2Valid && row3Valid
  }

  private def hashRow(cells: Seq[String]): String = {
    val normalisedString = cells.map(_.trim).mkString("|")
    val digest = MessageDigest.getInstance("SHA-256")
    val hashBytes = digest.digest(normalisedString.getBytes(StandardCharsets.UTF_8))

    hashBytes.map("%02x".format(_)).mkString
  }
}