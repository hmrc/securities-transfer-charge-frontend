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

package uk.gov.hmrc.securitiestransferchargefrontend.viewmodels.dashboard

import play.api.libs.json.{Json, OFormat}
import uk.gov.hmrc.securitiestransferchargefrontend.models.sh03.shared.{CompanyDetails, DetailsOfThisSharePurchase, ReasonForPurchase, RoleAtPurchasingCompany}
import uk.gov.hmrc.securitiestransferchargefrontend.models.shared.AgentReference
import uk.gov.hmrc.securitiestransferchargefrontend.models.stf.{AlfConfirmedAddress, ConfirmableAddress, DetailsOfThisTransfer, SecuritiesTarget, TaxRate}
import uk.gov.hmrc.securitiestransferchargefrontend.models.{JourneyType, UserAnswers}
import uk.gov.hmrc.securitiestransferchargefrontend.pages.sh03 as sh03
import uk.gov.hmrc.securitiestransferchargefrontend.pages.stf.shared as stfShared
import uk.gov.hmrc.securitiestransferchargefrontend.pages.stf.single as stfSingle

import java.time.LocalDate

case class DraftSummary(
                         sh03AgentReference: Option[AgentReference] = None,
                         sh03CompanyDetails: Option[CompanyDetails] = None,
                         sh03ReasonForPurchase: Option[ReasonForPurchase] = None,
                         sh03TreasuryShares: Option[Boolean] = None,
                         sh03ConnectedPersons: Option[Boolean] = None,
                         sh03ApplyingForRelief: Option[Boolean] = None,
                         sh03WhatReliefAreYouApplyingFor: Option[String] = None,
                         sh03DetailsOfThisSharePurchase: Option[DetailsOfThisSharePurchase] = None,
                         sh03MaximumAmountPaid: Option[BigDecimal] = None,
                         sh03MinimumAmountPaid: Option[BigDecimal] = None,
                         sh03ChargingPoint: Option[LocalDate] = None,
                         sh03RoleAtPurchasingCompany: Option[RoleAtPurchasingCompany] = None,
                         stfAgentReference: Option[AgentReference] = None,
                         stfConfirmedAddress: Option[ConfirmableAddress] = None,
                         stfNameOfBuyer: Option[String] = None,
                         stfBuyerAddress: Option[AlfConfirmedAddress] = None,
                         stfNameOfSeller: Option[String] = None,
                         stfSellerAddress: Option[AlfConfirmedAddress] = None,
                         stfConnectedPersons: Option[Boolean] = None,
                         stfApplyingForRelief: Option[Boolean] = None,
                         stfWhatReliefAreYouApplyingFor: Option[String] = None,
                         stfSecuritiesTarget: Option[SecuritiesTarget] = None,
                         stfChargingPoint: Option[LocalDate] = None,
                         stfTaxRate: Option[TaxRate] = None,
                         stfPurchasingShares: Option[Boolean] = None,
                         stfDetailsOfThisTransfer: Option[DetailsOfThisTransfer] = None,
                         stfOtherSecuritiesType: Option[String] = None,
                         stfAmountPaidForSecurities: Option[BigDecimal] = None,
                         stfTotalMarketValue: Option[BigDecimal] = None
                       )

object DraftSummary {

  implicit val format: OFormat[DraftSummary] = Json.format[DraftSummary]

  def sh03Summary(userAnswers: UserAnswers): DraftSummary = DraftSummary(
    sh03AgentReference = userAnswers.get(sh03.AgentReferencePage),
    sh03CompanyDetails = userAnswers.get(sh03.CompanyDetailsPage),
    sh03ReasonForPurchase = userAnswers.get(sh03.ReasonForPurchasePage),
    sh03TreasuryShares = userAnswers.get(sh03.TreasurySharesPage),
    sh03ConnectedPersons = userAnswers.get(sh03.ConnectedPersonsPage),
    sh03ApplyingForRelief = userAnswers.get(sh03.ApplyingForReliefPage),
    sh03WhatReliefAreYouApplyingFor = userAnswers.get(sh03.WhatReliefAreYouApplyingForPage),
    sh03DetailsOfThisSharePurchase = userAnswers.get(sh03.DetailsOfThisSharePurchasePage),
    sh03MaximumAmountPaid = userAnswers.get(sh03.MaximumAmountPaidPage),
    sh03MinimumAmountPaid = userAnswers.get(sh03.MinimumAmountPaidPage),
    sh03ChargingPoint = userAnswers.get(sh03.ChargingPointPage),
    sh03RoleAtPurchasingCompany = userAnswers.get(sh03.RoleAtPurchasingCompanyPage))


  def stfSummary(userAnswers: UserAnswers): DraftSummary = DraftSummary(
    stfAgentReference = userAnswers.get(stfShared.AgentReferencePage),
    stfConfirmedAddress = userAnswers.get(stfSingle.ConfirmAddressPage),
    stfNameOfBuyer = userAnswers.get(stfSingle.NameOfBuyerPage),
    stfBuyerAddress = userAnswers.get(stfSingle.StfBuyersAddressPage),
    stfNameOfSeller = userAnswers.get(stfSingle.NameOfSellerPage),
    stfSellerAddress = userAnswers.get(stfSingle.StfSellerAddressPage),
    stfConnectedPersons = userAnswers.get(stfSingle.ConnectedPersonsPage),
    stfApplyingForRelief = userAnswers.get(stfSingle.ApplyingForReliefPage),
    stfWhatReliefAreYouApplyingFor = userAnswers.get(stfSingle.WhatReliefAreYouApplyingForPage),
    stfSecuritiesTarget = userAnswers.get(stfSingle.SecuritiesTargetPage),
    stfChargingPoint = userAnswers.get(stfSingle.ChargingPointPage),
    stfTaxRate = userAnswers.get(stfSingle.TaxRatePage),
    stfPurchasingShares = userAnswers.get(stfSingle.PurchasingSharesPage),
    stfDetailsOfThisTransfer = userAnswers.get(stfSingle.DetailsOfThisTransferPage),
    stfOtherSecuritiesType = userAnswers.get(stfSingle.OtherSecuritiesTypePage),
    stfAmountPaidForSecurities = userAnswers.get(stfSingle.AmountPaidForSecuritiesPage),
    stfTotalMarketValue = userAnswers.get(stfSingle.TotalMarketValuePage))


  def getSummary(userAnswers: UserAnswers): DraftSummary =
    userAnswers.journeyType match {
      case JourneyType.SH03 => sh03Summary(userAnswers)
      case JourneyType.STF => stfSummary(userAnswers)
    }

}