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

package uk.gov.hmrc.securitiestransferchargefrontend.forms.stf.organisations

import play.api.data.Form
import play.api.i18n.Messages
import uk.gov.hmrc.securitiestransferchargefrontend.forms.mappings.Mappings

import java.time.LocalDate
import javax.inject.Inject

class ChargingPointFormProvider @Inject() extends Mappings {

  def apply()(implicit messages: Messages): Form[LocalDate] =
    Form(
      "value" -> localDate(
        invalidKey     = "chargingPoint.org.error.invalid",
        allRequiredKey = "chargingPoint.org.error.required.all",
        twoRequiredKey = "chargingPoint.org.error.required.two",
        requiredKey    = "chargingPoint.org.error.required",
      ).verifying(
        maxDate(LocalDate.now(), "chargingPoint.org.error.futureDate")
      )
    )
}
