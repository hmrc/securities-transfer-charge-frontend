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
import uk.gov.hmrc.securitiestransferchargefrontend.models.stf.fileupload.{ParsedStcRow, StcRowValidationError}

import javax.inject.{Inject, Singleton}

@Singleton
class StcConditionalRowValidator @Inject()(
                                            support: StcValidationSupport,
                                            messagesApi: MessagesApi
                                          ) {

  private implicit val messages: Messages =
    messagesApi.preferred(Seq(Lang("en")))

  private val marketValueMaximum = BigDecimal(999999999)
  private val typeOfShareMaxLength = 100

  def validate(
                row: ParsedStcRow,
                template: StcTemplate
              )(implicit cols: ColumnIndexBuilder): Seq[StcRowValidationError] = {

    template match {

      case StcTemplate.STF =>
        validateSTF(row)

      case StcTemplate.SH03 =>
        validateSH03(row)
    }
  }

  def validateSTF(
                   row: ParsedStcRow
                 )(implicit cols: ColumnIndexBuilder): Seq[StcRowValidationError] =
    validateReliefType(row) ++
      validateTypeOfShares(row) ++
      validateSellerAddress(row) ++
      validateTotalMarketValue(row)

  def validateSH03(
                    row: ParsedStcRow
                  )(implicit cols: ColumnIndexBuilder): Seq[StcRowValidationError] =
    validateReliefType(row)

  private def validateReliefType(
                                  row: ParsedStcRow
                                )(implicit cols: ColumnIndexBuilder): Seq[StcRowValidationError] = {

    row.applyingForRelief match {

      case Some(true) =>

        row.whatReliefAreYouApplyingFor match {

          case None =>
            Seq(
              support.error(
                row.rowNumber,
                "whatReliefAreYouApplyingFor",
                messages("fileUpload.error.whatReliefAreYouApplyingFor.invalid")
              )
            )

          case Some(value) if value.trim.isEmpty =>
            Seq(
              support.error(
                row.rowNumber,
                "whatReliefAreYouApplyingFor",
                messages("fileUpload.error.whatReliefAreYouApplyingFor.invalid")
              )
            )

          case Some(value) if !StcReliefOptions.isAllowed(value) =>
            Seq(
              support.error(
                row.rowNumber,
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
  }

  private def validateTypeOfShares(
                                    row: ParsedStcRow
                                  )(implicit cols: ColumnIndexBuilder): Seq[StcRowValidationError] = {

    row.whatTypeOfSecurities match {

      case Some(value) if StcSecurityType.isShares(value) =>

        row.typeOfShares match {

          case None =>
            Seq(
              support.error(
                row.rowNumber,
                "typeOfShares",
                messages("fileUpload.error.typeOfShares.required")
              )
            )

          case Some(v) if v.trim.isEmpty =>
            Seq(
              support.error(
                row.rowNumber,
                "typeOfShares",
                messages("fileUpload.error.typeOfShares.required")
              )
            )
          case Some(value) if value.length > typeOfShareMaxLength =>
            Seq(
              support.error(
                row.rowNumber,
                "typeOfShares",
                messages("fileUpload.error.typeOfShares.maxLength")
              )
            )

          case _ =>
            Seq.empty
        }

      case _ =>
        Seq.empty
    }
  }

  private def validateSellerAddress(
                                     row: ParsedStcRow
                                   )(implicit cols: ColumnIndexBuilder): Seq[StcRowValidationError] = {

    row.sellerAddressInUK match {

      case Some(true) =>
        support.validateRequiredText(
          row.sellerAddressLine1,
          row.rowNumber,
          "sellerAddressLine1",
          messages("fileUpload.error.sellerAddressLine1.required"),
          Some(support.addressLineMaxLength),
          Some(messages("fileUpload.error.sellerAddressLine1.length")),
          Some(support.addressPattern),
          Some(messages("fileUpload.error.sellerAddressLine1.invalidCharacters"))
        ) ++
          support.validateOptionalText(
            row.sellerAddressLine2,
            row.rowNumber,
            "sellerAddressLine2",
            Some(support.addressLineMaxLength),
            Some(messages("fileUpload.error.sellerAddressLine2.length")),
            Some(support.addressPattern),
            Some(messages("fileUpload.error.sellerAddressLine2.invalidCharacters"))
          ) ++
          validateSellerPostcode(row)

      case Some(false) =>
        support.validateOptionalText(
          row.sellerCountry,
          row.rowNumber,
          "sellerCountry",
          Some(support.countryMaxLength),
          Some(messages("fileUpload.error.sellerCountry.length")),
          Some(support.countryPattern),
          Some(messages("fileUpload.error.sellerCountry.invalidCharacters"))
        )

      case _ =>
        Seq.empty
    }
  }

  private def validateSellerPostcode(
                                      row: ParsedStcRow
                                    )(implicit cols: ColumnIndexBuilder): Seq[StcRowValidationError] = {

    row.sellerPostcode match {

      case None =>
        Seq(
          support.error(
            row.rowNumber,
            "sellerPostcode",
            messages("fileUpload.error.sellerPostcode.required")
          )
        )

      case Some(v) if v.trim.isEmpty =>
        Seq(
          support.error(
            row.rowNumber,
            "sellerPostcode",
            messages("fileUpload.error.sellerPostcode.required")
          )
        )

      case Some(v) if !support.looksLikeUkPostcode(v) =>
        Seq(
          support.error(
            row.rowNumber,
            "sellerPostcode",
            messages("fileUpload.error.sellerPostcode.invalid")
          )
        )

      case _ =>
        Seq.empty
    }
  }

  private def validateTotalMarketValue(
                                        row: ParsedStcRow
                                      )(implicit cols: ColumnIndexBuilder): Seq[StcRowValidationError] = {

    row.connectedPersons match {

      case Some(true) =>
        row.totalMarketValue match {

          case None =>
            Seq(
              support.error(
                row.rowNumber,
                "totalMarketValue",
                messages("totalMarketValue.error.required")
              )
            )

          case Some(value) if value > marketValueMaximum =>
            Seq(
              support.error(
                row.rowNumber,
                "totalMarketValue",
                messages("fileUpload.error.totalMarketValue.maximum")
              )
            )

          case _ =>
            Seq.empty
        }

      case _ =>
        Seq.empty
    }
  }
}