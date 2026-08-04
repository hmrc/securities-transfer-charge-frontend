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

package generators

import org.scalacheck.{Arbitrary, Gen}
import uk.gov.hmrc.securitiestransferchargefrontend.models.sh03.shared.ReasonForPurchase
import uk.gov.hmrc.securitiestransferchargefrontend.models.stf.{DetailsOfThisTransfer, HowToNotifyAboutSecuritiesTransfer}

trait ModelGenerators {
  
  implicit lazy val arbitraryReasonForPurchase: Arbitrary[ReasonForPurchase] =
    Arbitrary {
      Gen.oneOf(ReasonForPurchase.values)
    }

  implicit lazy val arbitraryHowToNotifyAboutSecuritiesTransfer: Arbitrary[HowToNotifyAboutSecuritiesTransfer] =
    Arbitrary {
      Gen.oneOf(HowToNotifyAboutSecuritiesTransfer.values.toSeq)
    }

  implicit lazy val arbitraryDetailsOfThisTransfer: Arbitrary[DetailsOfThisTransfer] =
    Arbitrary {
      for {
        numberOfShares <- Gen.choose(1, 999999999)
        typeOfShares <- Gen.alphaStr.suchThat(_.nonEmpty)
        amountPaid <- Gen.choose(BigDecimal(0), BigDecimal(1000000))
        marketValue <- Gen.choose(BigDecimal(0), BigDecimal(1000000))
      } yield DetailsOfThisTransfer(numberOfShares, typeOfShares, amountPaid, Some(marketValue))
    }
}
