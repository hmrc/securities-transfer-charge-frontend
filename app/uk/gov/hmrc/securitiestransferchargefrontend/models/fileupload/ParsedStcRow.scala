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

package uk.gov.hmrc.securitiestransferchargefrontend.models.fileupload

import play.api.libs.json.{Json, OFormat}

import java.time.LocalDate

final case class ParsedStcRow(
                               rowNumber: Int,
                               addressLine1: ParsedValue[String],
                               addressLine2: ParsedValue[String],
                               addressLine3: ParsedValue[String],
                               addressLine4: ParsedValue[String],
                               postcode: ParsedValue[String],
                               country: ParsedValue[String],
                               sellerName: ParsedValue[String],
                               sellerAddressInUk: ParsedValue[Boolean],
                               sellerAddressLine1: ParsedValue[String],
                               sellerAddressLine2: ParsedValue[String],
                               sellerAddressLine3: ParsedValue[String],
                               sellerAddressLine4: ParsedValue[String],
                               sellerPostcode: ParsedValue[String],
                               sellerCountry: ParsedValue[String],
                               connectedPersons: ParsedValue[Boolean],
                               applyingForRelief: ParsedValue[Boolean],
                               whatReliefAreYouApplyingFor: ParsedValue[String],
                               securitiesTarget: ParsedValue[String],
                               companyRegistrationNumber: ParsedValue[String],
                               chargingPoint: ParsedValue[LocalDate],
                               taxRate: ParsedValue[BigDecimal],
                               whatTypeOfSecurities: ParsedValue[String],
                               typeOfShares: ParsedValue[String],
                               securitiesQuantity: ParsedValue[BigDecimal],
                               amountPaidForSecurities: ParsedValue[BigDecimal],
                               totalMarketValue: ParsedValue[BigDecimal]
                             )

object ParsedStcRow {
  implicit val format: OFormat[ParsedStcRow] = Json.format[ParsedStcRow]
}