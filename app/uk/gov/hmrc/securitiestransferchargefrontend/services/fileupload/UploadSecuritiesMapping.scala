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

import uk.gov.hmrc.securitiestransferchargefrontend.models.fileupload.{ParsedStcRow, ParsedValue}
import uk.gov.hmrc.securitiestransferchargefrontend.models.stf.{DetailsOfThisTransfer, WhatTypeOfSecurities}

final case class UploadSecuritiesMapping(
                                          frontendSecuritiesType: WhatTypeOfSecurities,
                                          otherSecuritiesType: Option[String],
                                          detailsOfThisTransfer: DetailsOfThisTransfer
                                        )

object UploadSecuritiesMapping {

  def from(row: ParsedStcRow): Option[UploadSecuritiesMapping] =
    for {
      numberOfShares <- parsed(row.securitiesQuantity).map(_.toString)
      amountPaid     <- parsed(row.amountPaidForSecurities)
      securitiesType <- parsed(row.whatTypeOfSecurities)
    } yield {
      if (isShares(securitiesType)) {
        UploadSecuritiesMapping(
          frontendSecuritiesType = WhatTypeOfSecurities.Shares,
          otherSecuritiesType = None,
          detailsOfThisTransfer = DetailsOfThisTransfer(
            numberOfShares = numberOfShares,
            typeOfShares = parsed(row.typeOfShares).getOrElse(""),
            amountPaid = amountPaid,
            marketValue = parsed(row.totalMarketValue)
          )
        )
      } else {
        UploadSecuritiesMapping(
          frontendSecuritiesType = WhatTypeOfSecurities.Other,
          otherSecuritiesType = Some(securitiesType),
          detailsOfThisTransfer = DetailsOfThisTransfer(
            numberOfShares = numberOfShares,
            typeOfShares = securitiesType,
            amountPaid = amountPaid,
            marketValue = parsed(row.totalMarketValue)
          )
        )
      }
    }

  private def isShares(value: String): Boolean =
    value.trim.equalsIgnoreCase("shares")

  private def parsed[A](value: ParsedValue[A]): Option[A] =
    value match {
      case ParsedValue.Valid(value) => Some(value)
      case _                        => None
    }
}