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

  def withVerifiedTemplateStream[A](
                                     uploadedFile: UploadedFile,
                                     affinityKey: String,
                                     templateType: String
                                   )(block: (Seq[String], Iterator[ParsedRow]) => Either[FileParseError, A]): Either[FileParseError, A] = {

    fileUploadConfig.template(affinityKey, templateType) match {
      case None =>
        Left(FileParseError.InvalidTemplate)

      case Some(templateDef) =>
        fileParsingService.withParsedStream(uploadedFile, templateDef.expectedColumns) { (headers, lazyRowIterator) =>

          val additionalHeaderRowsCount = fileUploadConfig.firstDataRow - 2
          val additionalHeaderRows = (1 to additionalHeaderRowsCount).flatMap { _ =>
            if (lazyRowIterator.hasNext) Some(lazyRowIterator.next()) else None
          }.toList

          val allHeaderCells = headers ++ additionalHeaderRows.flatMap(_.cells.sortBy(_.columnIndex).map(_.rawValue))
          
          if (hashBlock(allHeaderCells) != templateDef.signature) {
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
  }

  private def hashBlock(cells: Seq[String]): String = {
    val normalisedString = cells.map(_.trim).mkString("|")
    val digest = MessageDigest.getInstance("SHA-256")
    val hashBytes = digest.digest(normalisedString.getBytes(StandardCharsets.UTF_8))
    hashBytes.map("%02x".format(_)).mkString
  }
}