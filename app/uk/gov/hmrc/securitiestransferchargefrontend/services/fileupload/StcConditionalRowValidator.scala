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

import play.api.i18n.{Lang, Messages, MessagesApi}
import uk.gov.hmrc.securitiestransferchargefrontend.forms.stf.individuals.{OtherSecuritiesTypeFormProvider, TotalMarketValueFormProvider}
import uk.gov.hmrc.securitiestransferchargefrontend.models.fileupload.{ParsedRow, ParsedStcRow, ParsedValue, StcRowValidationError}

import javax.inject.{Inject, Singleton}

@Singleton
class StcConditionalRowValidator @Inject()(
                                            support: StcValidationSupport,
                                            messagesApi: MessagesApi,
                                            otherSecuritiesTypeFormProvider: OtherSecuritiesTypeFormProvider,
                                            totalMarketValueFormProvider: TotalMarketValueFormProvider
                                          ) {

  private implicit val messages: Messages =
    messagesApi.preferred(Seq(Lang("en")))

  def validate(rawRow: ParsedRow, parsedRow: ParsedStcRow): Seq[StcRowValidationError] =
    validateReliefType(rawRow, parsedRow) ++
      validateOtherSecuritiesType(rawRow, parsedRow) ++
      validateSellerAddress(rawRow, parsedRow) ++
      validateTotalMarketValue(rawRow, parsedRow)

  private def validateReliefType(
                                  rawRow: ParsedRow,
                                  parsedRow: ParsedStcRow
                                ): Seq[StcRowValidationError] =
    parsedRow.applyingForRelief match {
      case ParsedValue.Valid(true) =>
        rawRow.valueAt(StcUploadColumn.whatReliefAreYouApplyingFor).map(_.trim) match {
          case None | Some("") =>
            Seq(
              support.error(
                rawRow.rowNumber,
                "whatReliefAreYouApplyingFor",
                "Enter the name of the relief you are applying for"
              )
            )

          case Some(value) if !StcReliefOptions.isAllowed(value) =>
            Seq(
              support.error(
                rawRow.rowNumber,
                "whatReliefAreYouApplyingFor",
                "Enter a valid relief type"
              )
            )

          case _ =>
            Seq.empty
        }

      case _ =>
        Seq.empty
    }

  private def validateOtherSecuritiesType(
                                           rawRow: ParsedRow,
                                           parsedRow: ParsedStcRow
                                         ): Seq[StcRowValidationError] =
    parsedRow.whatTypeOfSecurities match {
      case ParsedValue.Valid(value) if value.equalsIgnoreCase("other") =>
        val errors = support.bindSingleValue(
          otherSecuritiesTypeFormProvider(),
          rawRow.valueAt(StcUploadColumn.otherSecuritiesType).getOrElse("")
        )

        errors.map { formError =>
          val message =
            formError.message match {
              case "otherSecuritiesType.error.required" =>
                messages("otherSecuritiesType.error.required")
              case _ =>
                "Enter a valid type of securities"
            }

          support.error(rawRow.rowNumber, "otherSecuritiesType", message)
        }

      case _ =>
        Seq.empty
    }

  private def validateSellerAddress(
                                     rawRow: ParsedRow,
                                     parsedRow: ParsedStcRow
                                   ): Seq[StcRowValidationError] =
    parsedRow.sellerAddressInUk match {
      case ParsedValue.Valid(true) =>
        support.validateRequiredText(
          rawRow,
          StcUploadColumn.sellerAddressLine1,
          "sellerAddressLine1",
          requiredMessage = "Enter the first line of your address",
          maxLength = Some(support.addressLineMaxLength),
          pattern = Some(support.addressPattern),
          invalidMessage = "Address line 1 can only include letters, numbers and the following characters: , . - '"
        ) ++
          support.validateOptionalText(
            rawRow,
            StcUploadColumn.sellerAddressLine2,
            "sellerAddressLine2",
            maxLength = Some(support.addressLineMaxLength),
            pattern = Some(support.addressPattern),
            invalidMessage = "Address line 2 can only include letters, numbers and the following characters: , . - '"
          ) ++
          // UCD says no errors for lines 3 and 4
          validateSellerPostcode(rawRow)

      case ParsedValue.Valid(false) =>
        support.validateOptionalText(
          rawRow,
          StcUploadColumn.sellerCountry,
          "sellerCountry",
          maxLength = Some(50),
          pattern = Some(support.countryPattern),
          invalidMessage = "Country can only include letters, numbers and the following characters: , . - '"
        )

      case _ =>
        Seq.empty
    }

  private def validateSellerPostcode(rawRow: ParsedRow): Seq[StcRowValidationError] =
    rawRow.valueAt(StcUploadColumn.sellerPostcode).map(_.trim) match {
      case None | Some("") =>
        Seq(support.error(rawRow.rowNumber, "sellerPostcode", "Enter a postcode"))

      case Some(value) if value.length > support.postcodeMaxLength =>
        Seq(support.error(rawRow.rowNumber, "sellerPostcode", "Enter a real postcode, like AA1 1AA"))

      case Some(value) if !support.looksLikeUkPostcode(value) =>
        Seq(support.error(rawRow.rowNumber, "sellerPostcode", "Enter a real postcode, like AA1 1AA"))

      case _ =>
        Seq.empty
    }

  private def validateTotalMarketValue(
                                        rawRow: ParsedRow,
                                        parsedRow: ParsedStcRow
                                      ): Seq[StcRowValidationError] =
    parsedRow.connectedPersons match {
      case ParsedValue.Valid(true) =>
        val errors = support.bindSingleValue(
          totalMarketValueFormProvider(),
          rawRow.valueAt(StcUploadColumn.totalMarketValue).getOrElse("")
        )

        errors.map { formError =>
          val message =
            formError.message match {
              case "totalMarketValue.error.required" =>
                messages("totalMarketValue.error.required")

              case "totalMarketValue.error.invalidNumeric" =>
                messages("totalMarketValue.error.nonNumeric")

              case "totalMarketValue.error.nonNumeric" =>
                messages("totalMarketValue.error.nonNumeric")

              case "totalMarketValue.error.aboveMaximum" =>
                "The market value of the securities must be £999,999,999 or below"

              case _ =>
                messages("totalMarketValue.error.required")
            }

          support.error(rawRow.rowNumber, "totalMarketValue", message)
        }

      case ParsedValue.Valid(false) =>
        support.mustBeEmpty(
          rawRow,
          StcUploadColumn.totalMarketValue,
          "totalMarketValue",
          "Total market value must be empty when connected persons is no"
        )

      case _ =>
        Seq.empty
    }
}