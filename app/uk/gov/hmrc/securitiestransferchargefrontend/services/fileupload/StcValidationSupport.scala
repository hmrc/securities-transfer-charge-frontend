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

import play.api.data.Form
import play.api.data.FormError
import play.api.i18n.{Lang, Messages, MessagesApi}
import uk.gov.hmrc.securitiestransferchargefrontend.models.fileupload.{ParsedRow, StcRowValidationError}
import uk.gov.hmrc.securitiestransferchargefrontend.viewmodels.fileupload.StcUploadFieldMetadata

import javax.inject.{Inject, Singleton}
import scala.util.matching.Regex

@Singleton
class StcValidationSupport @Inject()(messagesApi: MessagesApi) {

  implicit val messages: Messages =
    messagesApi.preferred(Seq(Lang("en")))

  val addressLineMaxLength = 50
  val postcodeMaxLength    = 10
  val countryMaxLength     = 50

  val addressPattern: Regex =
    """^[A-Za-z0-9,\.\-\' ]+$""".r

  val countryPattern: Regex =
    """^[A-Za-z0-9,\.\-\' ]+$""".r

  val securitiesQuantityMin: BigDecimal = BigDecimal(1)
  val securitiesQuantityMax: BigDecimal = BigDecimal(999999999)

  private val ukPostcodePattern: Regex =
    """(?i)^[A-Z]{1,2}\d[A-Z\d]?\s?\d[A-Z]{2}$""".r

  def questionLabel(fieldName: String): String =
    StcUploadFieldMetadata
      .byFieldName
      .get(fieldName)
      .map(_.questionLabel)
      .getOrElse(fieldName)

  def columnIndex(fieldName: String): Int =
    StcUploadFieldMetadata
      .byFieldName
      .get(fieldName)
      .map(_.columnIndex)
      .getOrElse(-1)

  def bindSingleValue[A](form: Form[A], rawValue: String): Seq[FormError] =
    form.bind(Map("value" -> rawValue)).errors

  def error(rowNumber: Int, fieldName: String, message: String): StcRowValidationError =
    StcRowValidationError(
      rowNumber = rowNumber,
      fieldName = fieldName,
      columnIndex = columnIndex(fieldName),
      message = message,
      blocking = true
    )

  def validateRequiredText(
                            rawRow: ParsedRow,
                            columnIndex: Int,
                            fieldName: String,
                            requiredMessage: String,
                            maxLength: Option[Int] = None,
                            lengthMessage: Option[String] = None,
                            pattern: Option[Regex] = None,
                            invalidMessage: String = "Enter a valid value"
                          ): Seq[StcRowValidationError] =
    rawRow.valueAt(columnIndex).map(_.trim) match {
      case None | Some("") =>
        Seq(error(rawRow.rowNumber, fieldName, requiredMessage))

      case Some(value) =>
        validateTextValue(
          rowNumber = rawRow.rowNumber,
          fieldName = fieldName,
          value = value,
          maxLength = maxLength,
          lengthMessage = lengthMessage,
          pattern = pattern,
          invalidMessage = invalidMessage
        )
    }

  def validateOptionalText(
                            rawRow: ParsedRow,
                            columnIndex: Int,
                            fieldName: String,
                            maxLength: Option[Int] = None,
                            lengthMessage: Option[String] = None,
                            pattern: Option[Regex] = None,
                            invalidMessage: String = "Enter a valid value"
                          ): Seq[StcRowValidationError] =
    rawRow.valueAt(columnIndex).map(_.trim) match {
      case None | Some("") =>
        Seq.empty

      case Some(value) =>
        validateTextValue(
          rowNumber = rawRow.rowNumber,
          fieldName = fieldName,
          value = value,
          maxLength = maxLength,
          lengthMessage = lengthMessage,
          pattern = pattern,
          invalidMessage = invalidMessage
        )
    }

  def validateBooleanField(
                            rawRow: ParsedRow,
                            columnIndex: Int,
                            fieldName: String,
                            form: Form[Boolean],
                            requiredMessage: String,
                            invalidMessage: String
                          ): Seq[StcRowValidationError] = {
    val errors = bindSingleValue(form, rawRow.valueAt(columnIndex).getOrElse(""))

    errors.map { formError =>
      val message =
        formError.message match {
          case "connectedPersons.error.required" |
               "applyingForRelief.error.required" |
               "error.required" =>
            requiredMessage

          case "error.boolean" =>
            invalidMessage

          case _ =>
            invalidMessage
        }

      error(rawRow.rowNumber, fieldName, message)
    }
  }

  def looksLikeUkPostcode(value: String): Boolean =
    ukPostcodePattern.pattern.matcher(value.trim).matches()

  private def validateTextValue(
                                 rowNumber: Int,
                                 fieldName: String,
                                 value: String,
                                 maxLength: Option[Int],
                                 lengthMessage: Option[String],
                                 pattern: Option[Regex],
                                 invalidMessage: String
                               ): Seq[StcRowValidationError] = {
    val lengthErrors =
      maxLength.toSeq.collect {
        case max if value.length > max =>
          error(
            rowNumber,
            fieldName,
            lengthMessage.getOrElse(s"${questionLabel(fieldName)} must be $max characters or fewer")
          )
      }

    val patternErrors =
      pattern.toSeq.collect {
        case regex if regex.findFirstMatchIn(value).isEmpty =>
          error(rowNumber, fieldName, invalidMessage)
      }

    lengthErrors ++ patternErrors
  }
}