/*
 * Copyright 2024 HM Revenue & Customs
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

package uk.gov.hmrc.securitiestransferchargefrontend.models

import play.api.i18n.Messages
import uk.gov.hmrc.securitiestransferchargefrontend.domain.SubmissionId
import uk.gov.hmrc.securitiestransferchargefrontend.utils.DateTimeFormats

import java.math.RoundingMode
import java.text.{DecimalFormat, DecimalFormatSymbols}
import java.time.LocalDate
import java.util.Locale

case class ConfirmationViewModel(
                                  submissionId: String,
                                  paymentDueBy: String,
                                  reference: Option[String],
                                  taxDue: String,
                                  isAgent: Boolean
                                )

object ConfirmationViewModel {

  private val symbols = new DecimalFormatSymbols(Locale.UK)
  symbols.setGroupingSeparator(',')
  symbols.setDecimalSeparator('.')

  private val formatter = new DecimalFormat("#,##0.##", symbols)
  formatter.setRoundingMode(RoundingMode.DOWN)

  private val formatterWithTwoDecimals = new DecimalFormat("#,##0.00", symbols)
  formatterWithTwoDecimals.setRoundingMode(RoundingMode.DOWN)

  def apply(
             submissionId: SubmissionId,
             paymentDueBy: LocalDate,
             reference: Option[String],
             taxDue: BigDecimal,
             isAgent: Boolean
           )(implicit messages: Messages): ConfirmationViewModel = {

    val truncatedTaxDue = taxDue.setScale(2, BigDecimal.RoundingMode.DOWN)

    val formattedTaxDue =
      if (truncatedTaxDue.remainder(BigDecimal(1)) == BigDecimal(0)) {
        formatter.format(truncatedTaxDue)
      } else {
        formatterWithTwoDecimals.format(truncatedTaxDue)
      }

    new ConfirmationViewModel(
      submissionId = submissionId.value,
      paymentDueBy = DateTimeFormats.formatDate(paymentDueBy)(messages.lang),
      reference = reference,
      taxDue = s"£$formattedTaxDue",
      isAgent = isAgent
    )
  }
}
