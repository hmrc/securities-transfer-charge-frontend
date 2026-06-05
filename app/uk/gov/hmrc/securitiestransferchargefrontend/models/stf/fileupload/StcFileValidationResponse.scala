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

import play.api.libs.json.{Json, OFormat}

final case class StcFileValidationResponse(
                                            rows: Seq[ValidatedStcRow],
                                            maxErrorsAllowed: Int
                                          ) {

  val hasBlockingErrors: Boolean =
    rows.exists(_.hasBlockingErrors)

  val hasErrors: Boolean =
    rows.exists(_.hasErrors)

  val validRows: Seq[ParsedStcRow] =
    rows.filterNot(_.hasBlockingErrors).map(_.parsedRow)

  private def allBlockingErrors: Iterator[StcRowValidationError] =
    rows.iterator.flatMap(_.validationErrors.filter(_.blocking))

  val tooManyBlockingErrors: Boolean =
    allBlockingErrors.take(maxErrorsAllowed + 1).size > maxErrorsAllowed

  lazy val blockingErrors: Seq[StcRowValidationError] =
    if (tooManyBlockingErrors) Seq.empty else allBlockingErrors.toList

  lazy val nonBlockingErrors: Seq[StcRowValidationError] =
    rows.flatMap(_.validationErrors.filterNot(_.blocking))
}

object StcFileValidationResponse {
  implicit val format: OFormat[StcFileValidationResponse] = Json.format[StcFileValidationResponse]
}