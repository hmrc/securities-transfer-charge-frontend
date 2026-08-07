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

package models.submission

import base.Fixtures.*
import base.SpecBase
import uk.gov.hmrc.securitiestransferchargefrontend.domain.TransferType
import uk.gov.hmrc.securitiestransferchargefrontend.domain.TransferType.{SH03, STF}
import uk.gov.hmrc.securitiestransferchargefrontend.models.sh03.HowToNotifyAboutShareBuyback
import uk.gov.hmrc.securitiestransferchargefrontend.models.sh03.shared.ReasonForPurchase.{ForCancellation, ToPlaceIntoTreasury}
import uk.gov.hmrc.securitiestransferchargefrontend.models.sh03.shared.RoleAtPurchasingCompany
import uk.gov.hmrc.securitiestransferchargefrontend.models.stf.{ConfirmableAddress, HowToNotifyAboutSecuritiesTransfer}
import uk.gov.hmrc.securitiestransferchargefrontend.models.stf.TaxRate.HalfPercent
import uk.gov.hmrc.securitiestransferchargefrontend.models.submission.{ReasonForPurchase as etmpReasonForPurchase, *}

import java.time.LocalDate

class UserAnswersTransformsSpec extends SpecBase {

  private val baseStfTransaction = StfTransaction(howToNotifyAboutSecuritiesTransfer = HowToNotifyAboutSecuritiesTransfer.OneAtATime,
    agentReference = Some(agentReference),
    confirmedAddress = Some(confirmableAddress),
    nameofBuyer = Some("buyer 1"),
    buyerAddress = Some(fakeAlfConfirmedAddress),
    nameOfSeller = "seller 1",
    sellerAddress = fakeAlfConfirmedAddress,
    connectedPersons = true,
    applyingForRelief = true,
    whatReliefAreYouApplyingFor = Some("relief"),
    securitiesTarget = securitiesTarget,
    chargingPoint = LocalDate.of(2026,1,25),
    taxRate = HalfPercent,
    purchasingShares = true,
    detailsOfThisTransfer = Some(sftDetailsOfThisTransfer),
    otherSecuritiesType = Some("Loan note"),
    amountPaidForSecurities = Some(BigDecimal(50)),
    totalMarketValue =  Some(BigDecimal(10)))

  private val baseSh03Transaction = Sh03Transaction(howToNotifyAboutShareBuyback = Some(HowToNotifyAboutShareBuyback.OneAtATime),
    agentReference = Some(agentReference),
    companyDetails = sh03CompanyDetails,
    reasonForPurchase = ForCancellation,
    treasuryShares = Some(true),
    connectedPersons = true,
    applyingForRelief = true,
    whatReliefAreYouApplyingFor = Some("relief"),
    detailsOfThisSharePurchase = sh03DetailsOfThisSharePurchase,
    maximumAmountPaid = Some(BigDecimal(500)),
    minimumAmountPaid = Some(BigDecimal(10)),
    chargingPoint = LocalDate.of(2026,5,15),
    roleAtPurchasingCompany = RoleAtPurchasingCompany(role = "Director",uksOrgan = None))

  "toStfRequest" - {

    "populate a request for an Individual" in {
      val transaction = baseStfTransaction
      val affinityData = individualAffinityData

      val result = UserAnswersTransforms.toStfRequest(transaction, affinityData)

      result.recordId mustBe 1
      result.transactionDetails.transactionType mustBe STF
      result.transactionDetails.reasonForPurchase mustBe None

      result.mainBuyerDetails.buyerName mustBe affinityData.name
      result.mainBuyerDetails.uniqueId mustBe Some(affinityData.nino)
      result.mainBuyerDetails.taxRate mustBe BuyerTaxRate.HalfPercent

      result.mainSellerDetails mustBe defined
      result.agentDetails mustBe None
    }

    "populate a request for an Organisation" in {
      val result =
        UserAnswersTransforms.toStfRequest(baseStfTransaction, organisationAffinityData)

      result.mainBuyerDetails.uniqueId mustBe Some(organisationAffinityData.utr)
      result.agentDetails mustBe None
    }


     "populate a request for an Agent" ignore { //Todo update this test once we have a way to capture the buyer's email for agents
      val result =
        UserAnswersTransforms.toStfRequest(baseStfTransaction, agentAffinityData)

      result.mainBuyerDetails.buyerName mustBe baseStfTransaction.nameofBuyer.get
      result.agentDetails mustBe defined
    }

    "use the confirmed buyer address when present" in {

      val confirmedAddress: ConfirmableAddress = confirmableAddress.copy(lines = List("1 Confirmed Address","Town"))
      val transaction = baseStfTransaction.copy(confirmedAddress = Some(confirmedAddress))

      val result = UserAnswersTransforms.toStfRequest(transaction, individualAffinityData)

      result.mainBuyerDetails.addr1 mustBe confirmedAddress.lines.head
    }

    "use the manually entered buyer address when no confirmed address exists" in {
      val buyerAddress = fakeAlfConfirmedAddress
      val transaction =
        baseStfTransaction.copy(
          confirmedAddress = None,
          buyerAddress = Some(buyerAddress)
        )

      val result =
        UserAnswersTransforms.toStfRequest(transaction, individualAffinityData)

      result.mainBuyerDetails.addr1 mustBe buyerAddress.address.lines.head
    }

    "derive the descriptionOfSecurity from detailsOfThisTransfer" in {
      val result =
        UserAnswersTransforms.toStfRequest(baseStfTransaction, individualAffinityData)

      result.transactionDetails.descriptionOfSecurity mustBe
        baseStfTransaction.detailsOfThisTransfer.get.typeOfShares
    }

    "default the number of shares to one when detailsOfThisTransfer is absent(Other securities type flow)" in {
      val transaction =
        baseStfTransaction.copy(detailsOfThisTransfer = None)

      val result = UserAnswersTransforms.toStfRequest(transaction, individualAffinityData)

      result.transactionDetails.descriptionOfSecurity mustBe baseStfTransaction.otherSecuritiesType.get
      result.transactionDetails.numberOfShares mustBe 1
    }

    "throw when no buyer address can be determined" in {
      val transaction =
        baseStfTransaction.copy(
          confirmedAddress = None,
          buyerAddress = None
        )

      intercept[IllegalArgumentException] {
        UserAnswersTransforms.toStfRequest(transaction, individualAffinityData)
      }
    }

    "throw when buyer name is missing for an Agent" in {
      val transaction =
        baseStfTransaction.copy(nameofBuyer = None)

      intercept[IllegalArgumentException] {
        UserAnswersTransforms.toStfRequest(transaction, agentAffinityData)
      }
    }

    "throw when description of shares cannot be determined" in {
      val transaction =
        baseStfTransaction.copy(
          detailsOfThisTransfer = None,
          otherSecuritiesType = None
        )

      intercept[IllegalArgumentException] {
        UserAnswersTransforms.toStfRequest(transaction, individualAffinityData)
      }
    }

    "throw when consideration cannot be determined" in {
      val transaction =
        baseStfTransaction.copy(
          detailsOfThisTransfer = None,
          amountPaidForSecurities = None
        )

      intercept[IllegalArgumentException] {
        UserAnswersTransforms.toStfRequest(transaction, individualAffinityData)
      }
    }
  }

  "toSH03Request" - {

    "create an SH03 request" in {
      val result =
        UserAnswersTransforms.toSh03Request(baseSh03Transaction, organisationAffinityData)

      result.recordId mustBe 1
      result.transactionDetails.transactionType mustBe SH03
      result.mainSellerDetails mustBe None
      result.agentDetails mustBe None
    }

    "populate agent details for an Agent" ignore { //TODO update test once we have all the missing data for agent sh03
      val result =
        UserAnswersTransforms.toSh03Request(baseSh03Transaction, agentAffinityData)

      result.agentDetails mustBe defined
    }

    "set reasonForPurchase to Both" in {
      val transaction =
        baseSh03Transaction.copy(
          reasonForPurchase = ForCancellation,
          treasuryShares = Some(true)
        )

      val result =
        UserAnswersTransforms.toSh03Request(transaction, organisationAffinityData)

      result.transactionDetails.reasonForPurchase mustBe Some(etmpReasonForPurchase.Both)
    }

    "set reasonForPurchase to PurchasedForCancellation when treasuryShares is false" in {
      val transaction =
        baseSh03Transaction.copy(
          reasonForPurchase = ForCancellation,
          treasuryShares = Some(false)
        )

      val result =
        UserAnswersTransforms.toSh03Request(transaction, organisationAffinityData)

      result.transactionDetails.reasonForPurchase mustBe Some(etmpReasonForPurchase.PurchasedForCancellation)
    }

    "set reasonForPurchase to PurchasedForCancellation when treasuryShares is absent" in {
      val transaction =
        baseSh03Transaction.copy(
          reasonForPurchase = ForCancellation,
          treasuryShares = None
        )

      val result =
        UserAnswersTransforms.toSh03Request(transaction, organisationAffinityData)

      result.transactionDetails.reasonForPurchase mustBe Some(etmpReasonForPurchase.PurchasedForCancellation)
    }

    "set reasonForPurchase to PurchasedToPlaceIntoTreasury" in {
      val transaction =
        baseSh03Transaction.copy(
          reasonForPurchase = ToPlaceIntoTreasury
        )

      val result =
        UserAnswersTransforms.toSh03Request(transaction, organisationAffinityData)

      result.transactionDetails.reasonForPurchase mustBe Some(etmpReasonForPurchase.PurchasedToPlaceIntoTreasury)
    }

    "set relief percentage when applying for relief" in {
      val transaction =
        baseSh03Transaction.copy(applyingForRelief = true)

      val result =
        UserAnswersTransforms.toSh03Request(transaction, organisationAffinityData)

      result.transactionDetails.reliefPercentage mustBe Some(100)
    }

    "leave relief percentage empty when not applying for relief" in {
      val transaction =
        baseSh03Transaction.copy(applyingForRelief = false)

      val result =
        UserAnswersTransforms.toSh03Request(transaction, organisationAffinityData)

      result.transactionDetails.reliefPercentage mustBe None
    }
  }
}
