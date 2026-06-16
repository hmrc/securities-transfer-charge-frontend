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
import uk.gov.hmrc.securitiestransferchargefrontend.forms.stf.shared.TotalMarketValueFormProvider
import uk.gov.hmrc.securitiestransferchargefrontend.models.stf.fileupload.{ParsedStcRow, StcRowValidationError}

import javax.inject.{Inject, Singleton}

@Singleton
class StcConditionalRowValidator @Inject()(
                                            support: StcValidationSupport,
                                            messagesApi: MessagesApi,
                                            totalMarketValueFormProvider: TotalMarketValueFormProvider
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

      case StcTemplate.STFAgent =>
        validateAgentSTF(row, affinityKey)
    }
  }

  def validateAgentSTF(
                        row: ParsedStcRow,
                        affinityKey: String
                      )(implicit cols: ColumnIndexBuilder): Seq[StcRowValidationError] =
    validateReliefType(row, affinityKey) ++
      validateTypeOfShares(row, affinityKey) ++
      validateBuyerAddress(row) ++
      validateSellerAddress(row) ++
      validateTotalMarketValue(row, affinityKey)


  def validateSTF(
                   row: ParsedStcRow,
                   affinityKey: String
                 )(implicit cols: ColumnIndexBuilder): Seq[StcRowValidationError] =
    validateReliefType(row, affinityKey) ++
      validateTypeOfShares(row, affinityKey) ++
      validateSellerAddress(row) ++
      validateTotalMarketValue(row, affinityKey)

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

    row.sellerAddressInUK.fold(Seq.empty[StcRowValidationError]) { inUK =>
      sellerAddressLinesValidation(row) ++
        (if (inUK) validateSellerPostcode(row)
        else validateSellerCountry(row))
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
                                        row: ParsedStcRow,
                                        affinityKey: String
                                      )(implicit cols: ColumnIndexBuilder): Seq[StcRowValidationError] = {

    row.connectedPersons match {

      case Some(true) =>
        val form = totalMarketValueFormProvider(affinityKey).bind(
          Map("value" -> row.totalMarketValue.getOrElse(""))
        )

        form.errors.map { e =>
          support.error(row.rowNumber, "totalMarketValue", messages(e.message))
        }

      case _ =>
        Seq.empty
    }
  }


  private def validateBuyerAddress(
                                    row: ParsedStcRow
                                  )(implicit cols: ColumnIndexBuilder): Seq[StcRowValidationError] = {

    row.buyerAddressInUK.fold(Seq.empty[StcRowValidationError]) { inUK =>
      buyerAddressLinesValidation(row) ++
        (if (inUK) validateBuyerPostcode(row)
        else validateBuyerCountry(row))
    }
  }

  private def validateBuyerPostcode(
                                     row: ParsedStcRow
                                   )(implicit cols: ColumnIndexBuilder): Seq[StcRowValidationError] = {

    row.buyerPostcode match {

      case None =>
        Seq(
          support.error(
            row.rowNumber,
            "buyerPostcode",
            messages("fileUpload.error.buyerPostcode.required")
          )
        )

      case Some(v) if v.trim.isEmpty =>
        Seq(
          support.error(
            row.rowNumber,
            "buyerPostcode",
            messages("fileUpload.error.buyerPostcode.required")
          )
        )

      case Some(v) if !support.looksLikeUkPostcode(v) =>
        Seq(
          support.error(
            row.rowNumber,
            "buyerPostcode",
            messages("fileUpload.error.buyerPostcode.invalid")
          )
        )

      case _ =>
        Seq.empty
    }
  }

  private def validateSellerCountry(
                                     row: ParsedStcRow
                                   )(implicit cols: ColumnIndexBuilder): Seq[StcRowValidationError] = {

    row.sellerCountry match {

      case Some(v) if v.length > support.countryMaxLength =>
        Seq(
          support.error(
            row.rowNumber,
            "sellerCountry",
            messages("fileUpload.error.sellerCountry.length")
          )
        )

      case Some(v) if !support.looksLikeCountry(v) =>
        Seq(
          support.error(
            row.rowNumber,
            "sellerCountry",
            messages("fileUpload.error.sellerCountry.invalidCharacters")
          )
        )

      case None =>
        Seq(
          support.error(
            row.rowNumber,
            "sellerCountry",
            messages("fileUpload.error.sellerCountry.required")
          )
        )
      case _ => Seq.empty
    }
  }

  private def validateBuyerCountry(
                                    row: ParsedStcRow
                                  )(implicit cols: ColumnIndexBuilder): Seq[StcRowValidationError] = {

    row.buyerCountry match {

      case Some(v) if v.length > support.countryMaxLength =>
        Seq(
          support.error(
            row.rowNumber,
            "buyerCountry",
            messages("fileUpload.error.buyerCountry.length")
          )
        )

      case Some(v) if !support.looksLikeCountry(v) =>
        Seq(
          support.error(
            row.rowNumber,
            "buyerCountry",
            messages("fileUpload.error.buyerCountry.invalidCharacters")
          )
        )

      case None =>
        Seq(
          support.error(
            row.rowNumber,
            "sellerCountry",
            messages("fileUpload.error.buyerCountry.required")
          )
        )
      case _ => Seq.empty
    }
  }

  private def sellerAddressLinesValidation(row: ParsedStcRow)(implicit cols: ColumnIndexBuilder) = {

    val addressFields = Seq(
      "sellerAddressLine2" -> row.sellerAddressLine2,
      "sellerAddressLine3" -> row.sellerAddressLine3,
      "sellerAddressLine4" -> row.sellerAddressLine4
    )

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
    }
  }

  private def buyerAddressLinesValidation(row: ParsedStcRow)(implicit cols: ColumnIndexBuilder) = {

    val addressFields = Seq(
      "buyerAddressLine2" -> row.buyerAddressLine2,
      "buyerAddressLine3" -> row.buyerAddressLine3,
      "buyerAddressLine4" -> row.buyerAddressLine4
    )

    support.validateRequiredText(
      row.buyerAddressLine1,
      row.rowNumber,
      "buyerAddressLine1",
      messages("fileUpload.error.buyerAddressLine1.required"),
      Some(support.addressLineMaxLength),
      Some(messages("fileUpload.error.buyerAddressLine1.length")),
      Some(support.addressPattern),
      Some(messages("fileUpload.error.buyerAddressLine1.invalidCharacters"))
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
    }
  }
}
