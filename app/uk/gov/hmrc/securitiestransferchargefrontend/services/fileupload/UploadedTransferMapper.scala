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

import uk.gov.hmrc.securitiestransferchargefrontend.models.fileupload._

object UploadedTransferMapper {

  def fromValidatedRows(rows: Seq[ParsedStcRow]): Either[Seq[UploadedTransferMappingError], Seq[UploadedTransfer]] = {
    val mappedRows = rows.map(fromValidatedRow)

    val errors =
      mappedRows.collect {
        case Left(error) => error
      }

    if (errors.nonEmpty) {
      Left(errors)
    } else {
      Right(
        mappedRows.collect {
          case Right(uploadedTransfer) => uploadedTransfer
        }
      )
    }
  }

  def fromValidatedRow(row: ParsedStcRow): Either[UploadedTransferMappingError, UploadedTransfer] =
    for {
      sellerName                <- required(row, "sellerName", row.sellerName)
      sellerAddressInUk         <- required(row, "sellerAddressInUk", row.sellerAddressInUk)
      sellerAddress             <- mapSellerAddress(row, sellerAddressInUk)
      connectedPersons          <- required(row, "connectedPersons", row.connectedPersons)
      applyingForRelief         <- required(row, "applyingForRelief", row.applyingForRelief)
      relief                    <- mapRelief(row, applyingForRelief)
      securitiesTarget          <- mapSecuritiesTarget(row)
      chargingPoint             <- required(row, "chargingPoint", row.chargingPoint)
      taxRate                   <- required(row, "taxRate", row.taxRate)
      securityDetails           <- mapSecurityDetails(row)
      securitiesQuantity        <- required(row, "securitiesQuantity", row.securitiesQuantity)
      amountPaidForSecurities   <- required(row, "amountPaidForSecurities", row.amountPaidForSecurities)
      totalMarketValue           = optional(row.totalMarketValue)
    } yield UploadedTransfer(
      rowNumber = row.rowNumber,
      sellerName = sellerName,
      sellerAddress = sellerAddress,
      connectedPersons = connectedPersons,
      relief = relief,
      securitiesTarget = securitiesTarget,
      chargingPoint = chargingPoint,
      taxRate = taxRate,
      securityDetails = securityDetails,
      securitiesQuantity = securitiesQuantity,
      amountPaidForSecurities = amountPaidForSecurities,
      totalMarketValue = totalMarketValue
    )

  private def mapSellerAddress(
                                row: ParsedStcRow,
                                sellerAddressInUk: Boolean
                              ): Either[UploadedTransferMappingError, UploadedSellerAddress] =
    if (sellerAddressInUk) {
      for {
        line1    <- required(row, "sellerAddressLine1", row.sellerAddressLine1)
        postcode <- required(row, "sellerPostcode", row.sellerPostcode)
      } yield UploadedSellerAddress.UkAddress(
        line1 = line1,
        line2 = optional(row.sellerAddressLine2),
        line3 = optional(row.sellerAddressLine3),
        line4 = optional(row.sellerAddressLine4),
        postcode = postcode
      )
    } else {
      required(row, "sellerCountry", row.sellerCountry).map { country =>
        UploadedSellerAddress.NonUkAddress(country)
      }
    }

  private def mapRelief(
                         row: ParsedStcRow,
                         applyingForRelief: Boolean
                       ): Either[UploadedTransferMappingError, Option[String]] =
    if (applyingForRelief) {
      required(row, "whatReliefAreYouApplyingFor", row.whatReliefAreYouApplyingFor).map(Some(_))
    } else {
      Right(None)
    }

  private def mapSecuritiesTarget(row: ParsedStcRow): Either[UploadedTransferMappingError, UploadedSecuritiesTarget] =
    required(row, "securitiesTarget", row.securitiesTarget).map { businessName =>
      UploadedSecuritiesTarget(
        businessName = businessName,
        companyRegistrationNumber = optional(row.companyRegistrationNumber)
      )
    }

  private def mapSecurityDetails(row: ParsedStcRow): Either[UploadedTransferMappingError, UploadedSecurityDetails] =
    required(row, "whatTypeOfSecurities", row.whatTypeOfSecurities).flatMap { whatTypeOfSecurities =>
      if (StcSecurityType.isShares(whatTypeOfSecurities)) {
        required(row, "typeOfShares", row.typeOfShares).map { typeOfShares =>
          UploadedSecurityDetails.Shares(
            typeOfShares = typeOfShares
          )
        }
      } else {
        Right(
          UploadedSecurityDetails.Other(
            description = whatTypeOfSecurities
          )
        )
      }
    }

  private def required[A](
                           row: ParsedStcRow,
                           fieldName: String,
                           value: ParsedValue[A]
                         ): Either[UploadedTransferMappingError, A] =
    value match {
      case ParsedValue.Valid(value) =>
        Right(value)

      case ParsedValue.Missing =>
        Left(
          UploadedTransferMappingError(
            rowNumber = row.rowNumber,
            fieldName = fieldName,
            reason = "Required value was missing after validation"
          )
        )

      case ParsedValue.Invalid(rawValue, reason) =>
        Left(
          UploadedTransferMappingError(
            rowNumber = row.rowNumber,
            fieldName = fieldName,
            reason = s"Invalid value '$rawValue' after validation: $reason"
          )
        )
    }

  private def optional[A](value: ParsedValue[A]): Option[A] =
    value match {
      case ParsedValue.Valid(value) => Some(value)
      case _                        => None
    }
}

final case class UploadedTransferMappingError(
                                               rowNumber: Int,
                                               fieldName: String,
                                               reason: String
                                             )