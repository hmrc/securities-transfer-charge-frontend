package uk.gov.hmrc.securitiestransferchargefrontend.pages

import uk.gov.hmrc.securitiestransferchargefrontend.models.$className$
import play.api.libs.json.JsPath

case object $className$Page extends QuestionPage[Set[$className$]] {
  
  override def path: JsPath = JsPath \ toString
  
  override def toString: String = "$className;format="decap"$"
}
