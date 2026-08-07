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

package uk.gov.hmrc.securitiestransferchargefrontend.models.submission

import play.api.libs.json.{JsPath, Json, OFormat}
import uk.gov.hmrc.securitiestransferchargefrontend.models.shared.AgentReference
import uk.gov.hmrc.securitiestransferchargefrontend.models.sh03.*
import uk.gov.hmrc.securitiestransferchargefrontend.pages.QuestionPage

import uk.gov.hmrc.securitiestransferchargefrontend.models.sh03.shared.{ReasonForPurchase, DetailsOfThisSharePurchase, RoleAtPurchasingCompany,CompanyDetails}

import java.time.LocalDate

case class Sh03Transaction(
                            howToNotifyAboutShareBuyback: Option[HowToNotifyAboutShareBuyback],
                            agentReference: Option[AgentReference],
                            companyDetails: CompanyDetails,
                            reasonForPurchase: ReasonForPurchase,
                            treasuryShares: Option[Boolean],
                            connectedPersons: Boolean,
                            applyingForRelief: Boolean,
                            whatReliefAreYouApplyingFor: Option[String],
                            detailsOfThisSharePurchase: DetailsOfThisSharePurchase,
                            maximumAmountPaid: Option[BigDecimal],
                            minimumAmountPaid: Option[BigDecimal],
                            chargingPoint: LocalDate,
                            roleAtPurchasingCompany: RoleAtPurchasingCompany
                          )

object Sh03Transaction extends QuestionPage[Sh03Transaction] {

  implicit val format: OFormat[Sh03Transaction] = Json.format[Sh03Transaction]

  override def path: JsPath = JsPath

}