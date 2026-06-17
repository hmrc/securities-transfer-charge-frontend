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

package uk.gov.hmrc.securitiestransferchargefrontend.domain

import play.api.libs.json.*

enum TransferType:
  case STF, SH03, Other

object TransferType:
  given Format[TransferType] = Format(
    Reads {
      case JsNumber(n) if n.isValidInt => n.toInt match
        case 1 => JsSuccess(TransferType.STF)
        case 2 => JsSuccess(TransferType.SH03)
        case 3 => JsSuccess(TransferType.Other)
        case other => JsError(s"Invalid TransferType value [$other]. Expected one of: 1 (STF), 2 (SH03), 3 (Other)")
      case JsNumber(n) =>
        JsError(s"Invalid TransferType number [$n]. Expected an integer value: 1 (STF), 2 (SH03), 3 (Other)")
      case other => JsError(s"Expected a JSON number for TransferType, got: $other")
    },
    Writes {
      case TransferType.STF   => JsNumber(1)
      case TransferType.SH03  => JsNumber(2)
      case TransferType.Other => JsNumber(3)
    }
  )
