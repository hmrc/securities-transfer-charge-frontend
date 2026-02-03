package uk.gov.hmrc.securitiestransferchargefrontend.pages

import play.api.libs.json.JsPath

case object InTheUkOrNotPage extends QuestionPage[Boolean] {

  override def path: JsPath = JsPath \ toString

  override def toString: String = "inTheUkOrNot"
}
