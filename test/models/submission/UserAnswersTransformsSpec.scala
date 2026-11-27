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
import uk.gov.hmrc.securitiestransferchargefrontend.models.Relief
import uk.gov.hmrc.securitiestransferchargefrontend.models.sh03.shared.ReasonForPurchase.{ForCancellation, ToPlaceIntoTreasury}
import uk.gov.hmrc.securitiestransferchargefrontend.models.sh03.shared.RoleAtPurchasingCompany
import uk.gov.hmrc.securitiestransferchargefrontend.models.stf.ConfirmableAddress
import uk.gov.hmrc.securitiestransferchargefrontend.models.stf.TaxRate.HalfPercent
import uk.gov.hmrc.securitiestransferchargefrontend.models.submission.{ReasonForPurchase as etmpReasonForPurchase, *}

import java.time.LocalDate

class UserAnswersTransformsSpec extends SpecBase {

  // Percentages differ from 100 so the tests show the submitted value comes from the list.
  private val reliefs = Seq(Relief("relief", 40), Relief("Another Relief", 75))

  private val baseStfTransaction = StfTransaction(
    agentReference = Some(agentReference),
    confirmedAddress = Some(confirmableAddress),
    nameOfBuyer = Some("buyer 1"),
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

  private val baseSh03Transaction = Sh03Transaction(
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

      val result = UserAnswersTransforms.toStfRequest(transaction, affinityData, reliefs)

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
        UserAnswersTransforms.toStfRequest(baseStfTransaction, organisationAffinityData, reliefs)

      result.mainBuyerDetails.uniqueId mustBe organisationAffinityData.utr
      result.agentDetails mustBe None
    }


     "populate a request for an Agent" ignore { //Todo update this test once we have a way to capture the buyer's email for agents
      val result =
        UserAnswersTransforms.toStfRequest(baseStfTransaction, agentAffinityData, reliefs)

      result.mainBuyerDetails.buyerName mustBe baseStfTransaction.nameOfBuyer.get
      result.agentDetails mustBe defined
    }

    "use the confirmed buyer address when present" in {

      val confirmedAddress: ConfirmableAddress = confirmableAddress.copy(lines = List("1 Confirmed Address","Town"))
      val transaction = baseStfTransaction.copy(confirmedAddress = Some(confirmedAddress))

      val result = UserAnswersTransforms.toStfRequest(transaction, individualAffinityData, reliefs)

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
        UserAnswersTransforms.toStfRequest(transaction, individualAffinityData, reliefs)

      result.mainBuyerDetails.addr1 mustBe buyerAddress.address.lines.head
    }

    "truncate postcodes to 10 characters if they exceed the limit" in {
      val longPostcodeConfirmed = confirmableAddress.copy(postcode = "1234567890123")
      val longPostcodeSeller = fakeAlfConfirmedAddress.copy(
        address = fakeAlfConfirmedAddress.address.copy(postcode = "1234567890123")
      )

      val transaction = baseStfTransaction.copy(
        confirmedAddress = Some(longPostcodeConfirmed),
        sellerAddress = longPostcodeSeller
      )

      val result = UserAnswersTransforms.toStfRequest(transaction, individualAffinityData, reliefs)

      result.mainBuyerDetails.postcode mustBe "1234567890"
      result.mainBuyerDetails.postcode.length mustBe 10
      result.mainSellerDetails.get.postcode mustBe "1234567890"
      result.mainSellerDetails.get.postcode.length mustBe 10
    }

    "leave postcodes unchanged if they are 10 characters or fewer" in {
      val normalPostcodeConfirmed = confirmableAddress.copy(postcode = "SW1A 1AA")
      val normalPostcodeSeller = fakeAlfConfirmedAddress.copy(
        address = fakeAlfConfirmedAddress.address.copy(postcode = "SW1A 1AA")
      )

      val transaction = baseStfTransaction.copy(
        confirmedAddress = Some(normalPostcodeConfirmed),
        sellerAddress = normalPostcodeSeller
      )

      val result = UserAnswersTransforms.toStfRequest(transaction, individualAffinityData, reliefs)

      result.mainBuyerDetails.postcode mustBe "SW1A 1AA"
      result.mainSellerDetails.get.postcode mustBe "SW1A 1AA"
    }

    "derive the descriptionOfSecurity from detailsOfThisTransfer" in {
      val result =
        UserAnswersTransforms.toStfRequest(baseStfTransaction, individualAffinityData, reliefs)

      result.transactionDetails.typeOfSecurity mustBe
        baseStfTransaction.detailsOfThisTransfer.get.typeOfShares
    }

    "default the number of shares to one when detailsOfThisTransfer is absent(Other securities type flow)" in {
      val transaction =
        baseStfTransaction.copy(detailsOfThisTransfer = None)

      val result = UserAnswersTransforms.toStfRequest(transaction, individualAffinityData, reliefs)

      result.transactionDetails.typeOfSecurity mustBe baseStfTransaction.otherSecuritiesType.get
      result.transactionDetails.numberOfShares mustBe 1
    }

    "throw when no buyer address can be determined" in {
      val transaction =
        baseStfTransaction.copy(
          confirmedAddress = None,
          buyerAddress = None
        )

      intercept[IllegalArgumentException] {
        UserAnswersTransforms.toStfRequest(transaction, individualAffinityData, reliefs)
      }
    }

    "throw when buyer name is missing for an Agent" in {
      val transaction =
        baseStfTransaction.copy(nameOfBuyer = None)

      intercept[IllegalArgumentException] {
        UserAnswersTransforms.toStfRequest(transaction, agentAffinityData, reliefs)
      }
    }

    "throw when description of shares cannot be determined" in {
      val transaction =
        baseStfTransaction.copy(
          detailsOfThisTransfer = None,
          otherSecuritiesType = None
        )

      intercept[IllegalArgumentException] {
        UserAnswersTransforms.toStfRequest(transaction, individualAffinityData, reliefs)
      }
    }

    "throw when consideration cannot be determined" in {
      val transaction =
        baseStfTransaction.copy(
          detailsOfThisTransfer = None,
          amountPaidForSecurities = None
        )

      intercept[IllegalArgumentException] {
        UserAnswersTransforms.toStfRequest(transaction, individualAffinityData, reliefs)
      }
    }
  }

  "toSH03Request" - {

    "create an SH03 request" in {
      val result =
        UserAnswersTransforms.toSh03Request(baseSh03Transaction, organisationAffinityData, reliefs)

      result.recordId mustBe 1
      result.transactionDetails.transactionType mustBe SH03
      result.mainSellerDetails mustBe None
      result.agentDetails mustBe None
    }

    "populate agent details for an Agent" ignore { //TODO update test once we have all the missing data for agent sh03
      val result =
        UserAnswersTransforms.toSh03Request(baseSh03Transaction, agentAffinityData, reliefs)

      result.agentDetails mustBe defined
    }

    "set reasonForPurchase to Both" in {
      val transaction =
        baseSh03Transaction.copy(
          reasonForPurchase = ForCancellation,
          treasuryShares = Some(true)
        )

      val result =
        UserAnswersTransforms.toSh03Request(transaction, organisationAffinityData, reliefs)

      result.transactionDetails.reasonForPurchase mustBe Some(etmpReasonForPurchase.Both)
    }

    "set reasonForPurchase to PurchasedForCancellation when treasuryShares is false" in {
      val transaction =
        baseSh03Transaction.copy(
          reasonForPurchase = ForCancellation,
          treasuryShares = Some(false)
        )

      val result =
        UserAnswersTransforms.toSh03Request(transaction, organisationAffinityData, reliefs)

      result.transactionDetails.reasonForPurchase mustBe Some(etmpReasonForPurchase.PurchasedForCancellation)
    }

    "set reasonForPurchase to PurchasedForCancellation when treasuryShares is absent" in {
      val transaction =
        baseSh03Transaction.copy(
          reasonForPurchase = ForCancellation,
          treasuryShares = None
        )

      val result =
        UserAnswersTransforms.toSh03Request(transaction, organisationAffinityData, reliefs)

      result.transactionDetails.reasonForPurchase mustBe Some(etmpReasonForPurchase.PurchasedForCancellation)
    }

    "set reasonForPurchase to PurchasedToPlaceIntoTreasury" in {
      val transaction =
        baseSh03Transaction.copy(
          reasonForPurchase = ToPlaceIntoTreasury
        )

      val result =
        UserAnswersTransforms.toSh03Request(transaction, organisationAffinityData, reliefs)

      result.transactionDetails.reasonForPurchase mustBe Some(etmpReasonForPurchase.PurchasedToPlaceIntoTreasury)
    }

    "set relief percentage from the list of reliefs when applying for relief" in {
      val transaction =
        baseSh03Transaction.copy(applyingForRelief = true, whatReliefAreYouApplyingFor = Some("Another Relief"))

      val result =
        UserAnswersTransforms.toSh03Request(transaction, organisationAffinityData, reliefs)

      result.transactionDetails.reliefPercentage mustBe Some(75)
    }

    "leave relief percentage empty when not applying for relief" in {
      val transaction =
        baseSh03Transaction.copy(applyingForRelief = false)

      val result =
        UserAnswersTransforms.toSh03Request(transaction, organisationAffinityData, reliefs)

      result.transactionDetails.reliefPercentage mustBe None
    }

    "fail when the claimed relief is not in the list of reliefs" in {
      val transaction =
        baseSh03Transaction.copy(applyingForRelief = true, whatReliefAreYouApplyingFor = Some("Unknown Relief"))

      an[UnknownReliefException] must be thrownBy
        UserAnswersTransforms.toSh03Request(transaction, organisationAffinityData, reliefs)
    }

    "fail when applying for relief but no relief name was given" in {
      val transaction =
        baseSh03Transaction.copy(applyingForRelief = true, whatReliefAreYouApplyingFor = None)

      an[UnknownReliefException] must be thrownBy
        UserAnswersTransforms.toSh03Request(transaction, organisationAffinityData, reliefs)
    }
  }

  "toStfRequest relief percentage" - {

    "be taken from the list of reliefs when applying for relief" in {
      val transaction =
        baseStfTransaction.copy(applyingForRelief = true, whatReliefAreYouApplyingFor = Some("relief"))

      UserAnswersTransforms.toStfRequest(transaction, individualAffinityData, reliefs)
        .transactionDetails.reliefPercentage mustBe Some(40)
    }

    "be empty when not applying for relief" in {
      val transaction = baseStfTransaction.copy(applyingForRelief = false)

      UserAnswersTransforms.toStfRequest(transaction, individualAffinityData, reliefs)
        .transactionDetails.reliefPercentage mustBe None
    }

    "fail when the claimed relief is not in the list of reliefs" in {
      val transaction =
        baseStfTransaction.copy(applyingForRelief = true, whatReliefAreYouApplyingFor = Some("Unknown Relief"))

      an[UnknownReliefException] must be thrownBy
        UserAnswersTransforms.toStfRequest(transaction, individualAffinityData, reliefs)
    }

    "not match a relief name that differs only by case, as the tax calculators do not" in {
      val transaction =
        baseStfTransaction.copy(applyingForRelief = true, whatReliefAreYouApplyingFor = Some("RELIEF"))

      an[UnknownReliefException] must be thrownBy
        UserAnswersTransforms.toStfRequest(transaction, individualAffinityData, reliefs)
    }
  }
}
