package uk.gov.hmrc.securitiestransferchargefrontend.pages

import play.api.libs.json.JsPath

case object NameOfSellerPage extends QuestionPage[String] {

  override def path: JsPath = JsPath \ toString

  override def toString: String = "nameOfSeller"
}
