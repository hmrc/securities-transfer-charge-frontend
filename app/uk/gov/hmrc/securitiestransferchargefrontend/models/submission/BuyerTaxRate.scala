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

enum BuyerTaxRate:
  case HalfPercent, OneAndHalfPercent

object BuyerTaxRate:
  given Format[BuyerTaxRate] = Format(
    Reads {
      case JsNumber(n) if n.isValidInt => n.toInt match
        case 1 => JsSuccess(BuyerTaxRate.HalfPercent)
        case 2 => JsSuccess(BuyerTaxRate.OneAndHalfPercent)
        case n => JsError(s"Invalid BuyerTaxRate value [$n]. Expected one of: 1 (HalfPercent), 2 (OneAndHalfPercent)")
      case nan => JsError(s"Expected a JSON number for BuyerTaxRate, got: $nan")
    },
    Writes {
      case BuyerTaxRate.HalfPercent       => JsNumber(1)
      case BuyerTaxRate.OneAndHalfPercent => JsNumber(2)
    }
  )
