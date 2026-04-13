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

import play.api.libs.json.*

enum ReasonForPurchase:
  case PurchasedForCancellation, PurchasedToPlaceIntoTreasury, Both

object ReasonForPurchase:
  given Format[ReasonForPurchase] = Format(
    Reads {
      case JsNumber(n) if n.isValidInt => n.toInt match
        case 1 => JsSuccess(ReasonForPurchase.PurchasedForCancellation)
        case 2 => JsSuccess(ReasonForPurchase.PurchasedToPlaceIntoTreasury)
        case 3 => JsSuccess(ReasonForPurchase.Both)
        case other => JsError(
            s"Invalid ReasonForPurchase value [$other]. Expected one of: 1 (PurchasedForCancellation), 2 (PurchasedToPlaceIntoTreasury), 3 (Both)"
          )
      case JsNumber(n) =>
        JsError(s"Invalid ReasonForPurchase number [$n]. Expected an integer value: 1, 2 or 3")
      case other => JsError(s"Expected a JSON number for ReasonForPurchase, got: $other")
    },
    Writes {
      case ReasonForPurchase.PurchasedForCancellation  => JsNumber(1)
      case ReasonForPurchase.PurchasedToPlaceIntoTreasury => JsNumber(2)
      case ReasonForPurchase.Both                      => JsNumber(3)
    }
  )
