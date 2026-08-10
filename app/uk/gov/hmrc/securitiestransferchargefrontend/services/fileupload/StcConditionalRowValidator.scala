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
import uk.gov.hmrc.securitiestransferchargefrontend.forms.shared.BulkTotalMarketValueFormProvider
import uk.gov.hmrc.securitiestransferchargefrontend.models.JourneyType
import uk.gov.hmrc.securitiestransferchargefrontend.models.stf.fileupload.{ParsedStcRow, StcRowValidationError}

import javax.inject.{Inject, Singleton}

@Singleton
class StcConditionalRowValidator @Inject()(
                                            support: StcValidationSupport,
                                            messagesApi: MessagesApi,
                                            totalMarketValueFormProvider: BulkTotalMarketValueFormProvider
                                          ) {

  private implicit val messages: Messages =
    messagesApi.preferred(Seq(Lang("en")))

  def validate(
                row: ParsedStcRow,
                template: StcTemplate,
                affinityKey: String,
                journeyType: JourneyType
              )(implicit cols: ColumnIndexBuilder): Seq[StcRowValidationError] = {

    template match {

      case StcTemplate.STF =>
        validateSTF(row, affinityKey, journeyType)

      case StcTemplate.SH03 =>
        validateSH03(row, affinityKey, journeyType)

      case StcTemplate.STFAgent =>
        validateAgentSTF(row, affinityKey, journeyType)
    }
  }

  def validateAgentSTF(
                        row: ParsedStcRow,
                        affinityKey: String,
                        journeyType: JourneyType
                      )(implicit cols: ColumnIndexBuilder): Seq[StcRowValidationError] =
    validateReliefType(row, affinityKey) ++
      validateTypeOfSecurities(row, affinityKey) ++
      validateBuyerAddress(row) ++
      validateSellerAddress(row) ++
      validateTotalMarketValue(row, affinityKey, journeyType)


  def validateSTF(
                   row: ParsedStcRow,
                   affinityKey: String,
                   journeyType: JourneyType
                 )(implicit cols: ColumnIndexBuilder): Seq[StcRowValidationError] =
    validateReliefType(row, affinityKey) ++
      validateTypeOfSecurities(row, affinityKey) ++
      validateSellerAddress(row) ++
      validateTotalMarketValue(row, affinityKey, journeyType)

  def validateSH03(
                    row: ParsedStcRow,
                    affinityKey: String,
                    journeyType: JourneyType
                  )(implicit cols: ColumnIndexBuilder): Seq[StcRowValidationError] =
    validateReliefType(row, affinityKey) ++ 
      validateTotalMarketValue(row, affinityKey, journeyType)

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

  private def validateSellerAddress(
                                     row: ParsedStcRow
                                   )(implicit cols: ColumnIndexBuilder): Seq[StcRowValidationError] = {

    sellerAddressLinesValidation(row) ++
      validateSellerPostcode(row) ++
      row.sellerAddressInUK.fold(Seq.empty[StcRowValidationError]) { inUK =>
        if (!inUK) validateSellerCountry(row) else Seq.empty
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
                                        affinityKey: String,
                                        journeyType: JourneyType
                                      )(implicit cols: ColumnIndexBuilder): Seq[StcRowValidationError] = {

    row.connectedPersons match {

      case Some(true) =>
        val form = totalMarketValueFormProvider(affinityKey, journeyType).bind(
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

    buyerAddressLinesValidation(row) ++
      validateBuyerPostcode(row) ++
      row.buyerAddressInUK.fold(Seq.empty[StcRowValidationError]) { inUK =>
        if (!inUK) validateBuyerCountry(row) else Seq.empty
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
            "buyerCountry",
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

  private def validateTypeOfSecurities(
                                        row: ParsedStcRow,
                                        affinityKey: String
                                      )(implicit cols: ColumnIndexBuilder): Seq[StcRowValidationError] = {

    Seq(
      Option.when(row.whatTypeOfSecurities.isEmpty && row.typeOfShares.exists(_.equalsIgnoreCase("shares"))) {
        support.error(
          row.rowNumber,
          "whatTypeOfSecurities",
          messages(s"$affinityKey.fileUpload.error.typeOfShares.required")
        )
      },
      Option.when(row.whatTypeOfSecurities.exists(_.length > support.typeOfShareMaxLength)) {
        support.error(
          row.rowNumber,
          "whatTypeOfSecurities",
          messages(s"$affinityKey.fileUpload.error.typeOfShares.maxLength")
        )
      }
    ).flatten
  }
}
