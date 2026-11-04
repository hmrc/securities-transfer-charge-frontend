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
import uk.gov.hmrc.securitiestransferchargefrontend.models.JourneyType.{SH03, STF}
import uk.gov.hmrc.securitiestransferchargefrontend.services.stf.shared.FormattingService
import uk.gov.hmrc.securitiestransferchargefrontend.utils.DateTimeFormats

import java.time.LocalDate

case class ConfirmationViewModel(
                                  submissionId: String,
                                  paymentDueBy: String,
                                  reference: Option[String],
                                  taxDue: String,
                                  isAgent: Boolean,
                                  journeyType: JourneyType,
                                  isBulk: Boolean
                                ) {
  val useReferencesKey: String = journeyType match {
    case STF  => "confirmation.heading.transferReference.p2"
    case SH03 => "confirmation.heading.shareBuyBack.p2"
  }
  
}
  
object ConfirmationViewModel {
  
  val formatter = FormattingService()

  def apply(
             submissionId: SubmissionId,
             paymentDueBy: LocalDate,
             reference: Option[String],
             taxDue: BigDecimal,
             isAgent: Boolean,
             journeyType: JourneyType = STF,
             isBulk: Boolean
           )(implicit messages: Messages): ConfirmationViewModel = {
    
    
    new ConfirmationViewModel(
      submissionId = submissionId.value,
      paymentDueBy = DateTimeFormats.formatDate(paymentDueBy)(messages.lang),
      reference = reference,
      taxDue = formatter.formatCurrency(taxDue),
      isAgent = isAgent,
      journeyType = journeyType,
      isBulk = isBulk
    )
  }
}
