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

package forms.sh03.shared

import forms.behaviours.DateBehaviours
import play.api.i18n.Messages
import play.api.test.Helpers.stubMessages
import uk.gov.hmrc.securitiestransferchargefrontend.forms.sh03.shared.ChargingPointFormProvider
import uk.gov.hmrc.securitiestransferchargefrontend.models.DateHelper.today

import java.time.{LocalDate, ZoneOffset}

class ChargingPointFormProviderSpec extends DateBehaviours {

  private implicit val messages: Messages = stubMessages()
  val formProvider = new ChargingPointFormProvider()

  ".value" - {

    val validData = datesBetween(
      min = LocalDate.of(2000, 1, 1),
      max = LocalDate.now(ZoneOffset.UTC)
    )

    Seq("agent", "org").foreach { affinityKey =>

      s"when affinity key is $affinityKey" - {

        val form = formProvider(affinityKey)
        val requiredKey = s"$affinityKey.sh03.chargingPoint.error.required.all"
        val futureDateKey = s"$affinityKey.sh03.chargingPoint.error.futureDate"

        behave like dateField(form, "value", validData)

        behave like mandatoryDateField(form, "value", requiredKey)

        "reject dates in the future" in {
          val futureDate = today.plusDays(1)

          val result = form.bind(
            Map(
              "value.day"   -> futureDate.getDayOfMonth.toString,
              "value.month" -> futureDate.getMonthValue.toString,
              "value.year"  -> futureDate.getYear.toString
            )
          )
          result.errors.map(_.message) must contain(futureDateKey)
        }
      }
    }
  }
}