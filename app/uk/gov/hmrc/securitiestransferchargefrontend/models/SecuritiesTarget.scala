package uk.gov.hmrc.securitiestransferchargefrontend.models

import play.api.libs.json._

case class SecuritiesTarget (BusinessName: String, CRN: String)

object SecuritiesTarget {

  implicit val format: OFormat[SecuritiesTarget] = Json.format
}
