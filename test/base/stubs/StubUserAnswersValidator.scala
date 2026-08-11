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

import com.google.common.collect.{BiMap, HashBiMap}
import play.api.mvc.Call
import uk.gov.hmrc.securitiestransferchargefrontend.models.{Mode, UserAnswers}
import uk.gov.hmrc.securitiestransferchargefrontend.navigation.{Navigator, UserAnswersValidator}
import uk.gov.hmrc.securitiestransferchargefrontend.pages.JourneyRecoveryPage
import uk.gov.hmrc.securitiestransferchargefrontend.pages.stf.shared.CheckYourAnswersPage
import uk.gov.hmrc.securitiestransferchargefrontend.pages.stf.single.*

import scala.concurrent.ExecutionContext

class StubUserAnswersValidator(navigator: Navigator)(implicit ec: ExecutionContext) extends UserAnswersValidator(navigator) {

  override protected val startPage: GettablePage[?] = JourneyRecoveryPage

  override protected def pageCallMap(mode: Mode): BiMap[GettablePage[?], Call] = {
    val map = HashBiMap.create[GettablePage[?], Call]()
    map.put(JourneyRecoveryPage, Call("GET", "/journey-recovery"))
    map
  }
}

class CyaSuccessValidator(navigator: Navigator)(implicit ec: ExecutionContext) extends UserAnswersValidator(navigator) {
  
  override protected val startPage: GettablePage[?] = CheckYourAnswersPage

  override protected def pageCallMap(mode: Mode): BiMap[GettablePage[?], Call] = {
    val map = HashBiMap.create[GettablePage[?], Call]()
    
    map.put(ConfirmAddressPage, Call("GET", "/securities-transfer-charge/stf/confirm-address"))
    map.put(StfBuyersAddressPage, Call("GET", "/securities-transfer-charge/stf/address"))
    map.put(NameOfSellerPage, Call("GET", "/securities-transfer-charge/stf/change/seller-name"))
    map.put(StfSellerAddressPage, Call("GET", "/securities-transfer-charge/stf/seller-address"))
    map.put(ConnectedPersonsPage, Call("GET", "/securities-transfer-charge/stf/change/connected-persons"))
    map.put(ApplyingForReliefPage, Call("GET", "/securities-transfer-charge/stf/change/applying-for-relief"))
    map.put(WhatReliefAreYouApplyingForPage, Call("GET", "/securities-transfer-charge/stf/change/what-relief"))
    map.put(SecuritiesTargetPage, Call("GET", "/securities-transfer-charge/stf/change/securities-target"))
    map.put(ChargingPointPage, Call("GET", "/securities-transfer-charge/stf/change/charging-point"))
    map.put(TaxRatePage, Call("GET", "/securities-transfer-charge/stf/change/tax-rate"))
    map.put(PurchasingSharesPage, Call("GET", "/securities-transfer-charge/stf/change/buying-shares"))
    map.put(DetailsOfThisTransferPage, Call("GET", "/securities-transfer-charge/stf/change/share-details"))
    map.put(OtherSecuritiesTypePage, Call("GET", "/securities-transfer-charge/stf/change/other-securities-type"))
    map.put(AmountPaidForSecuritiesPage, Call("GET", "/securities-transfer-charge/stf/change/amount-paid"))
    map.put(TotalMarketValuePage, Call("GET", "/securities-transfer-charge/stf/change/total-market-value"))
    map.put(CheckYourAnswersPage, Call("GET", "/check-your-answers"))
    
    map
  }
  
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
