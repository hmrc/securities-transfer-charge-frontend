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
import uk.gov.hmrc.securitiestransferchargefrontend.models.fileupload.{ParsedStcRow, ParsedValue, StcRowValidationError, ValidatedStcRow}

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
      requiredString(row.rowNumber, "sellerName", row.sellerName),
      requiredBoolean(row.rowNumber, "sellerAddressInUk", row.sellerAddressInUk),
      requiredBoolean(row.rowNumber, "connectedPersons", row.connectedPersons),
      requiredBoolean(row.rowNumber, "applyingForRelief", row.applyingForRelief),
      requiredDate(row.rowNumber, "chargingPoint", row.chargingPoint),
      requiredNumber(row.rowNumber, "taxRate", row.taxRate),
      requiredString(row.rowNumber, "whatTypeOfSecurities", row.whatTypeOfSecurities),
      requiredNumber(row.rowNumber, "securitiesQuantity", row.securitiesQuantity),
      requiredNumber(row.rowNumber, "amountPaidForSecurities", row.amountPaidForSecurities)
    ).flatten

  private def conditionalErrors(row: ParsedStcRow): Seq[StcRowValidationError] = {
    val reliefError =
      row.applyingForRelief match {
        case ParsedValue.Valid(true) =>
          requiredString(
            row.rowNumber,
            "whatReliefAreYouApplyingFor",
            row.whatReliefAreYouApplyingFor,
            requiredMessage = "Relief type is required when applying for relief",
            invalidMessage = "Relief type must be a valid value"
          )
        case _ =>
          Seq.empty
      }

    val shareTypeError =
      row.whatTypeOfSecurities match {
        case ParsedValue.Valid(value) if value.equalsIgnoreCase("Other") =>
          requiredString(
            row.rowNumber,
            "otherSecuritiesType",
            row.otherSecuritiesType,
            requiredMessage = "Share type is required when security type is Other",
            invalidMessage = "Share type must be a valid value"
          )
        case _ =>
          Seq.empty
      }

    val sellerUkAddressErrors =
      row.sellerAddressInUk match {
        case ParsedValue.Valid(true) =>
          requiredString(row.rowNumber, "sellerAddressLine1", row.sellerAddressLine1) ++
            requiredString(row.rowNumber, "sellerAddressLine2", row.sellerAddressLine2) ++
            requiredString(row.rowNumber, "sellerPostcode", row.sellerPostcode)
        case _ =>
          Seq.empty
      }

    val sellerNonUkCountryError =
      row.sellerAddressInUk match {
        case ParsedValue.Valid(false) =>
          requiredString(
            row.rowNumber,
            "sellerCountry",
            row.sellerCountry,
            requiredMessage = "Seller country is required when seller address is outside the UK",
            invalidMessage = "Seller country must be a valid value"
          )
        case _ =>
          Seq.empty
      }

    val totalMarketValueErrors =
      row.connectedPersons match {
        case ParsedValue.Valid(true) =>
          requiredNumber(
            row.rowNumber,
            "totalMarketValue",
            row.totalMarketValue,
            requiredMessage = "Total market value is required when connected persons is yes",
            invalidMessage = "Total market value must be a number"
          )

        case ParsedValue.Valid(false) =>
          mustBeEmpty(
            row.rowNumber,
            "totalMarketValue",
            row.totalMarketValue,
            message = "Total market value must be empty when connected persons is no"
          )

        case _ =>
          Seq.empty
      }

    reliefError ++
      shareTypeError ++
      sellerUkAddressErrors ++
      sellerNonUkCountryError ++
      totalMarketValueErrors
  }

  private def requiredString(
                              rowNumber: Int,
                              fieldName: String,
                              value: ParsedValue[String],
                              requiredMessage: String = ""
                            ): Seq[StcRowValidationError] =
    value match {
      case ParsedValue.Missing =>
        Seq(error(rowNumber, fieldName, messageOrDefault(requiredMessage, s"$fieldName is required")))
      case ParsedValue.Invalid(_, _) =>
        Seq(error(rowNumber, fieldName, s"$fieldName must be a valid value"))
      case ParsedValue.Valid(_) =>
        Seq.empty
    }

  private def requiredString(
                              rowNumber: Int,
                              fieldName: String,
                              value: ParsedValue[String],
                              requiredMessage: String,
                              invalidMessage: String
                            ): Seq[StcRowValidationError] =
    value match {
      case ParsedValue.Missing =>
        Seq(error(rowNumber, fieldName, requiredMessage))
      case ParsedValue.Invalid(_, _) =>
        Seq(error(rowNumber, fieldName, invalidMessage))
      case ParsedValue.Valid(_) =>
        Seq.empty
    }

  private def requiredNumber(
                              rowNumber: Int,
                              fieldName: String,
                              value: ParsedValue[BigDecimal],
                              requiredMessage: String = "",
                              invalidMessage: String = ""
                            ): Seq[StcRowValidationError] =
    value match {
      case ParsedValue.Missing =>
        Seq(error(rowNumber, fieldName, messageOrDefault(requiredMessage, s"$fieldName is required")))
      case ParsedValue.Invalid(_, _) =>
        Seq(error(rowNumber, fieldName, messageOrDefault(invalidMessage, s"$fieldName must be a number")))
      case ParsedValue.Valid(_) =>
        Seq.empty
    }

  private def mustBeEmpty(
                           rowNumber: Int,
                           fieldName: String,
                           value: ParsedValue[BigDecimal],
                           message: String
                         ): Seq[StcRowValidationError] =
    value match {
      case ParsedValue.Missing =>
        Seq.empty
      case ParsedValue.Valid(_) | ParsedValue.Invalid(_, _) =>
        Seq(error(rowNumber, fieldName, message))
    }

  private def requiredBoolean(
                               rowNumber: Int,
                               fieldName: String,
                               value: ParsedValue[Boolean],
                               requiredMessage: String = "",
                               invalidMessage: String = ""
                             ): Seq[StcRowValidationError] =
    value match {
      case ParsedValue.Missing =>
        Seq(error(rowNumber, fieldName, messageOrDefault(requiredMessage, s"$fieldName is required")))
      case ParsedValue.Invalid(_, _) =>
        Seq(error(rowNumber, fieldName, messageOrDefault(invalidMessage, s"$fieldName must be yes or no")))
      case ParsedValue.Valid(_) =>
        Seq.empty
    }

  private def requiredDate(
                            rowNumber: Int,
                            fieldName: String,
                            value: ParsedValue[java.time.LocalDate],
                            requiredMessage: String = "",
                            invalidMessage: String = ""
                          ): Seq[StcRowValidationError] =
    value match {
      case ParsedValue.Missing =>
        Seq(error(rowNumber, fieldName, messageOrDefault(requiredMessage, s"$fieldName is required")))
      case ParsedValue.Invalid(_, _) =>
        Seq(error(rowNumber, fieldName, messageOrDefault(invalidMessage, s"$fieldName must be a valid date")))
      case ParsedValue.Valid(_) =>
        Seq.empty
    }

  private def messageOrDefault(message: String, default: String): String =
    if (message.nonEmpty) message else default

  private def error(rowNumber: Int, fieldName: String, message: String): StcRowValidationError =
    StcRowValidationError(
      rowNumber = rowNumber,
      fieldName = fieldName,
      message = message,
      blocking = true
    )
}