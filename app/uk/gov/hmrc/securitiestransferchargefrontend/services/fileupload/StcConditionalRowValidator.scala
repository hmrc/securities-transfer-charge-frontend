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
import uk.gov.hmrc.securitiestransferchargefrontend.models.fileupload.{ParsedRow, ParsedStcRow, ParsedValue, StcRowValidationError}

import javax.inject.{Inject, Singleton}

@Singleton
class StcConditionalRowValidator @Inject()(
                                            support: StcValidationSupport,
                                            messagesApi: MessagesApi
                                          ) {

  private implicit val messages: Messages =
    messagesApi.preferred(Seq(Lang("en")))

  private val marketValueMaximum = BigDecimal(999999999)

  def validate(rawRow: ParsedRow, parsedRow: ParsedStcRow): Seq[StcRowValidationError] =
    validateReliefType(rawRow, parsedRow) ++
      validateTypeOfShares(rawRow, parsedRow) ++
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
                messages("fileUpload.error.whatReliefAreYouApplyingFor.invalid")
              )
            )

          case Some(value) if !StcReliefOptions.isAllowed(value) =>
            Seq(
              support.error(
                rawRow.rowNumber,
                "whatReliefAreYouApplyingFor",
                messages("fileUpload.error.whatReliefAreYouApplyingFor.invalid")
              )
            )

          case _ =>
            Seq.empty
        }

      case _ =>
        Seq.empty
    }

  private def validateTypeOfShares(
                                    rawRow: ParsedRow,
                                    parsedRow: ParsedStcRow
                                  ): Seq[StcRowValidationError] =
    parsedRow.whatTypeOfSecurities match {
      case ParsedValue.Valid(value) if value.trim.equalsIgnoreCase("shares") =>
        rawRow.valueAt(StcUploadColumn.typeOfShares).map(_.trim) match {
          case None | Some("") =>
            Seq(
              support.error(
                rawRow.rowNumber,
                "typeOfShares",
                messages("fileUpload.error.typeOfShares.required")
              )
            )

          case _ =>
            Seq.empty
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
          requiredMessage = messages("fileUpload.error.sellerAddressLine1.required"),
          maxLength = Some(support.addressLineMaxLength),
          lengthMessage = Some(messages("fileUpload.error.sellerAddressLine1.length")),
          pattern = Some(support.addressPattern),
          invalidMessage = messages("fileUpload.error.sellerAddressLine1.invalidCharacters")
        ) ++
          support.validateOptionalText(
            rawRow,
            StcUploadColumn.sellerAddressLine2,
            "sellerAddressLine2",
            maxLength = Some(support.addressLineMaxLength),
            lengthMessage = Some(messages("fileUpload.error.sellerAddressLine2.length")),
            pattern = Some(support.addressPattern),
            invalidMessage = messages("fileUpload.error.sellerAddressLine2.invalidCharacters")
          ) ++
          validateSellerPostcode(rawRow)

      case ParsedValue.Valid(false) =>
        support.validateOptionalText(
          rawRow,
          StcUploadColumn.sellerCountry,
          "sellerCountry",
          maxLength = Some(50),
          lengthMessage = Some(messages("fileUpload.error.sellerCountry.length")),
          pattern = Some(support.countryPattern),
          invalidMessage = messages("fileUpload.error.sellerCountry.invalidCharacters")
        )

      case _ =>
        Seq.empty
    }

  private def validateSellerPostcode(rawRow: ParsedRow): Seq[StcRowValidationError] =
    rawRow.valueAt(StcUploadColumn.sellerPostcode).map(_.trim) match {
      case None | Some("") =>
        Seq(
          support.error(
            rawRow.rowNumber,
            "sellerPostcode",
            messages("fileUpload.error.sellerPostcode.required")
          )
        )

      case Some(value) if !support.looksLikeUkPostcode(value) =>
        Seq(
          support.error(
            rawRow.rowNumber,
            "sellerPostcode",
            messages("fileUpload.error.sellerPostcode.invalid")
          )
        )

      case _ =>
        Seq.empty
    }

  private def validateTotalMarketValue(
                                        rawRow: ParsedRow,
                                        parsedRow: ParsedStcRow
                                      ): Seq[StcRowValidationError] =
    parsedRow.connectedPersons match {
      case ParsedValue.Valid(true) =>
        parsedRow.totalMarketValue match {
          case ParsedValue.Missing =>
            Seq(
              support.error(
                rawRow.rowNumber,
                "totalMarketValue",
                messages("totalMarketValue.error.required")
              )
            )

          case ParsedValue.Invalid(_, _) =>
            Seq(
              support.error(
                rawRow.rowNumber,
                "totalMarketValue",
                messages("fileUpload.error.totalMarketValue.nonNumeric")
              )
            )

          case ParsedValue.Valid(value) if value > marketValueMaximum =>
            Seq(
              support.error(
                rawRow.rowNumber,
                "totalMarketValue",
                messages("fileUpload.error.totalMarketValue.maximum")
              )
            )

          case _ =>
            Seq.empty
        }

      case ParsedValue.Valid(false) =>
        Seq.empty

      case _ =>
        Seq.empty
    }
}