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

package uk.gov.hmrc.securitiestransferchargefrontend.config

import play.api.Configuration

import javax.inject.{Inject, Singleton}

case class TemplateDefinition(expectedColumns: Int, signature: String)

@Singleton
class FileUploadConfig @Inject()(configuration: Configuration) {

  val maxRows: Int =
    configuration.get[Int]("file-upload.max-rows")

  val maxColumns: Int =
    configuration.getOptional[Int]("file-upload.max-columns").getOrElse(28)

  val expectedWorksheetName: String =
    configuration.getOptional[String]("file-upload.xlsx.expected-worksheet")
      .getOrElse("Sheet1")

  val firstDataRow: Int =
    configuration.getOptional[Int]("file-upload.first-data-row").getOrElse(4)

  val maxErrorsAllowed: Int =
    configuration.getOptional[Int]("file-upload.max-errors-allowed").getOrElse(25)

  def template(affinityKey: String, templateType: String): Option[TemplateDefinition] = {
    for {
      cols <- configuration.getOptional[Int](s"file-upload.templates.$affinityKey.$templateType.expected-columns")
      sig  <- configuration.getOptional[String](s"file-upload.templates.$affinityKey.$templateType.signature")
    } yield TemplateDefinition(cols, sig)
  }
}