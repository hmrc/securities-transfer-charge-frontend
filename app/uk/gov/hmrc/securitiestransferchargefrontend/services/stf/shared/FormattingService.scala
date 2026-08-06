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

package uk.gov.hmrc.securitiestransferchargefrontend.services.stf.shared

import play.api.i18n.Lang
import uk.gov.hmrc.securitiestransferchargefrontend.utils.DateTimeFormats

import java.time.LocalDate
import javax.inject.{Inject, Singleton}

@Singleton
class FormattingService @Inject() {

  def formatTaxDue(taxDue: BigDecimal): String = {
    f"£$taxDue%.2f"
  }

  def formatPaymentDueDate(paymentDueDate: LocalDate)(implicit lang: Lang): String = {
    paymentDueDate.format(DateTimeFormats.dateTimeFormat())
  }
}
