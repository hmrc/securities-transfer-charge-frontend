package uk.gov.hmrc.securitiestransferchargefrontend.pages

import uk.gov.hmrc.securitiestransferchargefrontend.models.SecuritiesTarget
import play.api.libs.json.JsPath

case object SecuritiesTargetPage extends QuestionPage[SecuritiesTarget] {

  override def path: JsPath = JsPath \ toString

  override def toString: String = "securitiesTarget"
}
