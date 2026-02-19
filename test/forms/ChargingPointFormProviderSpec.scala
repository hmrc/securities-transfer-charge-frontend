package forms

import forms.behaviours.DateBehaviours
import play.api.i18n.Messages
import play.api.test.Helpers.stubMessages
import uk.gov.hmrc.securitiestransferchargefrontend.forms.ChargingPointFormProvider

import java.time.{LocalDate, ZoneOffset}

class ChargingPointFormProviderSpec extends DateBehaviours {

  private implicit val messages: Messages = stubMessages()
  private val form = new ChargingPointFormProvider()()

  ".value" - {

    val today = LocalDate.now(ZoneOffset.UTC)

    val validData = datesBetween(
      min = LocalDate.of(2000, 1, 1),
      max = LocalDate.now(ZoneOffset.UTC)
    )

    behave like dateField(form, "value", validData)

    behave like mandatoryDateField(form, "value", "chargingPoint.error.required.all")


    "reject dates in the future" in {
      val futureDate = today.plusDays(1)

      val result = form.bind(
        Map(
          "value.day" -> futureDate.getDayOfMonth.toString,
          "value.month" -> futureDate.getMonthValue.toString,
          "value.year" -> futureDate.getYear.toString
        )
      )
      result.errors.map(_.message) must contain("chargingPoint.error.futureDate")
    }
  }
}
