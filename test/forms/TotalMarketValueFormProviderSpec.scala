package forms

import uk.gov.hmrc.securitiestransferchargefrontend.config.CurrencyFormatter.currencyFormat
import uk.gov.hmrc.securitiestransferchargefrontend.forms.behaviours.CurrencyFieldBehaviours
import org.scalacheck.Gen
import play.api.data.FormError

import scala.math.BigDecimal.RoundingMode

class TotalMarketValueFormProviderSpec extends CurrencyFieldBehaviours {

  val form = new TotalMarketValueFormProvider()()

  ".value" - {

    val fieldName = "value"

    val minimum = 0
    val maximum = Int.MaxValue

    val validDataGenerator =
      Gen.choose[BigDecimal](minimum, maximum)
        .map(_.setScale(2, RoundingMode.HALF_UP))
        .map(_.toString)

    behave like fieldThatBindsValidData(
      form,
      fieldName,
      validDataGenerator
    )

    behave like currencyField(
      form,
      fieldName,
      nonNumericError     = FormError(fieldName, "totalMarketValue.error.nonNumeric"),
      invalidNumericError = FormError(fieldName, "totalMarketValue.error.invalidNumeric")
    )

    behave like currencyFieldWithMaximum(
      form,
      fieldName,
      maximum,
      FormError(fieldName, "totalMarketValue.error.aboveMaximum", Seq(currencyFormat(maximum)))
    )

    behave like mandatoryField(
      form,
      fieldName,
      requiredError = FormError(fieldName, "totalMarketValue.error.required")
    )
  }
}
