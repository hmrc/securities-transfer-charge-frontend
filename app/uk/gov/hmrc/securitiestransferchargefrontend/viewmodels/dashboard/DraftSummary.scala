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

import uk.gov.hmrc.securitiestransferchargefrontend.models.sh03.shared.{CompanyDetails, DetailsOfThisSharePurchase, ReasonForPurchase, RoleAtPurchasingCompany}
import uk.gov.hmrc.securitiestransferchargefrontend.models.shared.AgentReference
import uk.gov.hmrc.securitiestransferchargefrontend.models.{JourneyType, UserAnswers}
import uk.gov.hmrc.securitiestransferchargefrontend.pages.sh03

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
                         sh03RoleAtPurchasingCompany: Option[RoleAtPurchasingCompany] = None
                       )

object DraftSummary {
  
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

  def getSummary(userAnswers: UserAnswers): DraftSummary =
    userAnswers.journeyType match {
      case JourneyType.SH03 => sh03Summary(userAnswers)
      case JourneyType.STF => ???
    }

}