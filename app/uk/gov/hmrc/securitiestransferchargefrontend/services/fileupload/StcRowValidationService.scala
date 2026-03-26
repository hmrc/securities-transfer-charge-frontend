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

package uk.gov.hmrc.securitiestransferchargefrontend.services.fileupload

import javax.inject.Singleton
import uk.gov.hmrc.securitiestransferchargefrontend.models.fileupload.{ParsedStcRow, StcRowValidationError, ValidatedStcRow}

@Singleton
class StcRowValidationService {

  def validate(row: ParsedStcRow): ValidatedStcRow =
    ValidatedStcRow(
      parsedRow = row,
      validationErrors = validateRow(row)
    )

  private def validateRow(row: ParsedStcRow): Seq[StcRowValidationError] =
    requiredFieldErrors(row) ++ conditionalErrors(row)

  private def requiredFieldErrors(row: ParsedStcRow): Seq[StcRowValidationError] =
    Seq(
      required(row.rowNumber, "addressLine1", row.addressLine1),
      required(row.rowNumber, "sellerName", row.sellerName),
      required(row.rowNumber, "sellerAddressInUk", row.sellerAddressInUk),
      required(row.rowNumber, "connectedPersons", row.connectedPersons),
      required(row.rowNumber, "applyingForRelief", row.applyingForRelief),
      required(row.rowNumber, "chargingPoint", row.chargingPoint),
      required(row.rowNumber, "taxRate", row.taxRate),
      required(row.rowNumber, "whatTypeOfSecurities", row.whatTypeOfSecurities),
      required(row.rowNumber, "securitiesQuantity", row.securitiesQuantity),
      required(row.rowNumber, "amountPaidForSecurities", row.amountPaidForSecurities),
      required(row.rowNumber, "totalMarketValue", row.totalMarketValue)
    ).flatten

  private def conditionalErrors(row: ParsedStcRow): Seq[StcRowValidationError] = {
    val reliefError =
      if (row.applyingForRelief.contains(true) && row.whatReliefAreYouApplyingFor.isEmpty) {
        Some(error(row.rowNumber, "whatReliefAreYouApplyingFor", "Relief type is required when applying for relief"))
      } else {
        None
      }

    val shareTypeError =
      if (row.whatTypeOfSecurities.exists(_.equalsIgnoreCase("Shares")) && row.otherSecuritiesType.isEmpty) {
        Some(error(row.rowNumber, "otherSecuritiesType", "Share type is required when security type is Other"))
      } else {
        None
      }

    val sellerUkAddressErrors =
      if (row.sellerAddressInUk.contains(true)) {
        Seq(
          required(row.rowNumber, "sellerAddressLine1", row.sellerAddressLine1),
          required(row.rowNumber, "sellerPostcode", row.sellerPostcode)
        ).flatten
      } else {
        Seq.empty
      }

    val sellerNonUkCountryError =
      if (row.sellerAddressInUk.contains(false) && row.sellerCountry.isEmpty) {
        Seq(error(row.rowNumber, "sellerCountry", "Seller country is required when seller address is outside the UK"))
      } else {
        Seq.empty
      }

    Seq(reliefError, shareTypeError).flatten ++ sellerUkAddressErrors ++ sellerNonUkCountryError
  }

  private def required[A](rowNumber: Int, fieldName: String, value: Option[A]): Option[StcRowValidationError] =
    if (value.isEmpty) Some(error(rowNumber, fieldName, s"$fieldName is required")) else None

  private def error(rowNumber: Int, fieldName: String, message: String): StcRowValidationError =
    StcRowValidationError(
      rowNumber = rowNumber,
      fieldName = fieldName,
      message = message,
      blocking = true
    )
}