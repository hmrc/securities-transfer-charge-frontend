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

package base.stubs

import play.api.mvc.Call
import uk.gov.hmrc.securitiestransferchargefrontend.models.UserAnswers
import uk.gov.hmrc.securitiestransferchargefrontend.navigation.{Navigator, PageCallBiMap, PageCallBiMapBuilder, UserAnswersValidator}
import uk.gov.hmrc.securitiestransferchargefrontend.pages.JourneyRecoveryPage
import uk.gov.hmrc.securitiestransferchargefrontend.pages.stf.shared.CheckYourAnswersPage
import uk.gov.hmrc.securitiestransferchargefrontend.pages.stf.single.*

import scala.concurrent.ExecutionContext

class StubUserAnswersValidator(navigator: Navigator)(implicit ec: ExecutionContext) extends UserAnswersValidator(navigator) {

  override protected val startPage: GettablePage[?] = JourneyRecoveryPage

  override protected val pageCallMap: PageCallBiMap =
    PageCallBiMapBuilder()
      .addMappingNoCheck(JourneyRecoveryPage, () => Call("GET", "/journey-recovery"))
      .build
}

class CyaSuccessValidator(navigator: Navigator, journeyPrefix: String = "stf", affinityPrefix: String = "")(implicit ec: ExecutionContext) extends UserAnswersValidator(navigator) {

  override protected val startPage: GettablePage[?] = CheckYourAnswersPage

  override protected val pageCallMap: PageCallBiMap =
    PageCallBiMapBuilder()
      .addMappingNoCheck(ConfirmAddressPage, () => Call("GET", s"/securities-transfer-charge/$journeyPrefix/${affinityPrefix}confirm-address"))
      .addMappingNoCheck(StfBuyersAddressPage, () => Call("GET", s"/securities-transfer-charge/$journeyPrefix/${affinityPrefix}address"))
      .addMappingNoCheck(NameOfSellerPage, () => Call("GET", s"/securities-transfer-charge/$journeyPrefix/${affinityPrefix}change/seller-name"))
      .addMappingNoCheck(StfSellerAddressPage, () => Call("GET", s"/securities-transfer-charge/$journeyPrefix/${affinityPrefix}seller-address"))
      .addMappingNoCheck(ConnectedPersonsPage, () => Call("GET", s"/securities-transfer-charge/$journeyPrefix/${affinityPrefix}change/connected-persons"))
      .addMappingNoCheck(ApplyingForReliefPage, () => Call("GET", s"/securities-transfer-charge/$journeyPrefix/${affinityPrefix}change/applying-for-relief"))
      .addMappingNoCheck(WhatReliefAreYouApplyingForPage, () => Call("GET", s"/securities-transfer-charge/$journeyPrefix/${affinityPrefix}change/what-relief"))
      .addMappingNoCheck(SecuritiesTargetPage, () => Call("GET", s"/securities-transfer-charge/$journeyPrefix/${affinityPrefix}change/securities-target"))
      .addMappingNoCheck(ChargingPointPage, () => Call("GET", s"/securities-transfer-charge/$journeyPrefix/${affinityPrefix}change/charging-point"))
      .addMappingNoCheck(TaxRatePage, () => Call("GET", s"/securities-transfer-charge/$journeyPrefix/${affinityPrefix}change/tax-rate"))
      .addMappingNoCheck(PurchasingSharesPage, () => Call("GET", s"/securities-transfer-charge/$journeyPrefix/${affinityPrefix}change/buying-shares"))
      .addMappingNoCheck(DetailsOfThisTransferPage, () => Call("GET", s"/securities-transfer-charge/$journeyPrefix/${affinityPrefix}change/share-details"))
      .addMappingNoCheck(OtherSecuritiesTypePage, () => Call("GET", s"/securities-transfer-charge/$journeyPrefix/${affinityPrefix}change/other-securities-type"))
      .addMappingNoCheck(AmountPaidForSecuritiesPage, () => Call("GET", s"/securities-transfer-charge/$journeyPrefix/${affinityPrefix}change/amount-paid"))
      .addMappingNoCheck(TotalMarketValuePage, () => Call("GET", s"/securities-transfer-charge/$journeyPrefix/${affinityPrefix}change/total-market-value"))
      .addMappingNoCheck(CheckYourAnswersPage, () => Call("GET", "/check-your-answers"))
      .build

  override protected def pageHasValidDataAtPath(userAnswers: UserAnswers, page: GettablePage[_]): Boolean = {
    page match {
      case DetailsOfThisTransferPage if userAnswers.get(ConnectedPersonsPage).contains(true) =>
        userAnswers.get(DetailsOfThisTransferPage).exists(_.marketValue.isDefined)
      case SecuritiesTargetPage => // CRN is optional
        userAnswers.get(SecuritiesTargetPage).exists(_.businessName.nonEmpty)
      case _ => super.pageHasValidDataAtPath(userAnswers, page)
    }
  }
}
