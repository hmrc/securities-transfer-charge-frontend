package forms

import uk.gov.hmrc.securitiestransferchargefrontend.forms.behaviours.OptionFieldBehaviours
import uk.gov.hmrc.securitiestransferchargefrontend.models.HowToNotifyAboutSecuritiesTransfer
import play.api.data.FormError

class HowToNotifyAboutSecuritiesTransferFormProviderSpec extends OptionFieldBehaviours {

  val form = new HowToNotifyAboutSecuritiesTransferFormProvider()()

  ".value" - {

    val fieldName = "value"
    val requiredKey = "howToNotifyAboutSecuritiesTransfer.error.required"

    behave like optionsField[HowToNotifyAboutSecuritiesTransfer](
      form,
      fieldName,
      validValues  = HowToNotifyAboutSecuritiesTransfer.values,
      invalidError = FormError(fieldName, "error.invalid")
    )

    behave like mandatoryField(
      form,
      fieldName,
      requiredError = FormError(fieldName, requiredKey)
    )
  }
}
