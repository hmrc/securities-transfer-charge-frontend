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
import uk.gov.hmrc.securitiestransferchargefrontend.models.stf.*
import uk.gov.hmrc.securitiestransferchargefrontend.pages.QuestionPage

import java.time.LocalDate

case class StfTransaction(
                           howToNotifyAboutSecuritiesTransfer: HowToNotifyAboutSecuritiesTransfer,
                           agentReference: Option[AgentReference],
                           confirmedAddress: Option[ConfirmableAddress],
                           nameofBuyer: Option[String],
                           buyerAddress: Option[AlfConfirmedAddress],
                           nameOfSeller: String,
                           sellerAddress: AlfConfirmedAddress,
                           connectedPersons: Boolean,
                           applyingForRelief: Boolean,
                           whatReliefAreYouApplyingFor: Option[String],
                           securitiesTarget: SecuritiesTarget,
                           chargingPoint: LocalDate,
                           taxRate: TaxRate,
                           purchasingShares: Boolean,
                           detailsOfThisTransfer: Option[DetailsOfThisTransfer],
                           otherSecuritiesType: Option[String],
                           amountPaidForSecurities: Option[BigDecimal],
                           totalMarketValue: Option[BigDecimal]
                         )

object StfTransaction extends QuestionPage[StfTransaction] {

  implicit val format: OFormat[StfTransaction] = Json.format[StfTransaction]

  override def path: JsPath = JsPath

}