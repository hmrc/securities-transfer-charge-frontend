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

  def validate(
                row: ParsedStcRow,
                template: StcTemplate,
                affinityKey: String
              )(implicit cols: ColumnIndexBuilder): Seq[StcRowValidationError] = {

    template match {

      case StcTemplate.STF =>
        validateSTF(row, affinityKey)

      case StcTemplate.SH03 =>
        validateSH03(row, affinityKey)
    }
  }

  def validateSTF(
                   row: ParsedStcRow,
                   affinityKey: String
                 )(implicit cols: ColumnIndexBuilder): Seq[StcRowValidationError] =
    validateReliefType(row, affinityKey) ++
      validateTypeOfShares(row, affinityKey) ++
      validateSellerAddress(row) ++
      validateTotalMarketValue(row)

  def validateSH03(
                    row: ParsedStcRow,
                    affinityKey: String
                  )(implicit cols: ColumnIndexBuilder): Seq[StcRowValidationError] =
    validateReliefType(row, affinityKey)

  private def validateReliefType(
                                  row: ParsedStcRow,
                                  affinityKey: String
                                )(implicit cols: ColumnIndexBuilder): Seq[StcRowValidationError] = {

    row.applyingForRelief match {

      case Some(true) =>

        row.whatReliefAreYouApplyingFor match {

          case None =>
            Seq(
              support.error(
                row.rowNumber,
                "whatReliefAreYouApplyingFor",
                messages(s"$affinityKey.fileUpload.error.whatReliefAreYouApplyingFor.invalid")
              )
            )

          case Some(value) if value.trim.isEmpty =>
            Seq(
              support.error(
                row.rowNumber,
                "whatReliefAreYouApplyingFor",
                messages(s"$affinityKey.fileUpload.error.whatReliefAreYouApplyingFor.invalid")
              )
            )

          case Some(value) if !StcReliefOptions.isAllowed(value) =>
            Seq(
              support.error(
                row.rowNumber,
                "whatReliefAreYouApplyingFor",
                messages(s"$affinityKey.fileUpload.error.whatReliefAreYouApplyingFor.invalid")
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
                                    row: ParsedStcRow,
                                    affinityKey: String
                                  )(implicit cols: ColumnIndexBuilder): Seq[StcRowValidationError] = {

    row.whatTypeOfSecurities match {

      case Some(value) if StcSecurityType.isShares(value) =>

        row.typeOfShares match {

          case None =>
            Seq(
              support.error(
                row.rowNumber,
                "typeOfShares",
                messages(s"$affinityKey.fileUpload.error.typeOfShares.required")
              )
            )

          case Some(v) if v.trim.isEmpty =>
            Seq(
              support.error(
                row.rowNumber,
                "typeOfShares",
                messages(s"$affinityKey.fileUpload.error.typeOfShares.required")
              )
            )
          case Some(value) if value.length > support.typeOfShareMaxLength =>
            Seq(
              support.error(
                row.rowNumber,
                "typeOfShares",
                messages(s"$affinityKey.fileUpload.error.typeOfShares.maxLength")
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

    val addressFields = Seq(
      "sellerAddressLine2" -> row.sellerAddressLine2,
      "sellerAddressLine3" -> row.sellerAddressLine3,
      "sellerAddressLine4" -> row.sellerAddressLine4
    )

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
        ) ++ addressFields.flatMap { case (fieldName, value) =>
          support.validateOptionalText(
            value,
            row.rowNumber,
            fieldName,
            Some(support.optAddressLineMaxLength),
            Some(messages(s"fileUpload.error.$fieldName.length")),
            Some(support.addressPattern),
            Some(messages(s"fileUpload.error.$fieldName.invalidCharacters"))
          )
        } ++
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

          case Some(value) if value > support.maxCurrency =>
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