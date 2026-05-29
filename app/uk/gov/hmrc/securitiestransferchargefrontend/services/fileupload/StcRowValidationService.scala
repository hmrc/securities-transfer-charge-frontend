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

import uk.gov.hmrc.securitiestransferchargefrontend.models.stf.fileupload.{ParsedRow, ParsedStcRow, ValidatedStcRow}

import javax.inject.{Inject, Singleton}

@Singleton
class StcRowValidationService @Inject()(
                                         stcBasicRowValidator: StcBasicRowValidator,
                                         stcConditionalRowValidator: StcConditionalRowValidator
                                       ) {

  def validateAll(
                   rows: Seq[ParsedRow],
                   headers: Seq[String],
                   affinityKey:String
                 ): Seq[ValidatedStcRow] = {

    implicit val columnIndex: ColumnIndexBuilder = new ColumnIndexBuilder(headers)
    val mapper = new StcRowMapper(columnIndex)

    val parsedRows: Seq[ParsedStcRow] =
      rows.map(mapper.map)

    val template = detectTemplate(headers)

    parsedRows.map { parsedRow =>

      ValidatedStcRow(
        parsedRow = parsedRow,
        validationErrors =
          stcBasicRowValidator.validate(parsedRow, template,affinityKey) ++
            stcConditionalRowValidator.validate(parsedRow, template, affinityKey)
      )
    }
  }


  private def detectTemplate(headers: Seq[String]): StcTemplate = {

    val normalised = headers.map(_.trim.toLowerCase).toSet

    val templates = Seq(StcTemplate.SH03, StcTemplate.STF)

    templates.find { template =>
      template.identifyingFields
        .map(_.toLowerCase)
        .subsetOf(normalised)
    }.getOrElse {
      throw new IllegalArgumentException("Unable to determine file template")
    }
  }
}