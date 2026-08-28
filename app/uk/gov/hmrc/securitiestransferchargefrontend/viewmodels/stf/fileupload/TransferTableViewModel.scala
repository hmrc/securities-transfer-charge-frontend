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

package uk.gov.hmrc.securitiestransferchargefrontend.viewmodels.stf.fileupload

import uk.gov.hmrc.govukfrontend.views.viewmodels.table.TableRow
import uk.gov.hmrc.securitiestransferchargefrontend.viewmodels.govuk.table._
import uk.gov.hmrc.securitiestransferchargefrontend.viewmodels.stf.fileupload.Transfer
import uk.gov.hmrc.govukfrontend.views.Aliases.Text
import uk.gov.hmrc.govukfrontend.views.viewmodels.content.HtmlContent



object TransferTableViewModel {

  def rows(transfers: Seq[Transfer]): Seq[Seq[TableRow]] =
    transfers.map { transfer =>
      Seq(
        TableRowViewModel(
          content = Text(transfer.seller)
        ),
        TableRowViewModel(
          content = Text(transfer.securitiesBoughtIn)
        ),
        TableRowViewModel(
          content = HtmlContent(formatCurrency(transfer.consideration))
        ).withCssClass("govuk-table__cell--numeric"),
        TableRowViewModel(
          content = HtmlContent(formatCurrency(transfer.taxDue))
        ).withCssClass("govuk-table__cell--numeric")
      )
    }

  private def formatCurrency(value: BigDecimal): String =
    f"£${value.setScale(2)}%,.2f"
}
