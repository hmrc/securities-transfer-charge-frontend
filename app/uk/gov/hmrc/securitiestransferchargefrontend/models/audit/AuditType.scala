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

package uk.gov.hmrc.securitiestransferchargefrontend.models.audit

sealed trait AuditType {
  def value: String
}

object AuditType {
  case object Stf extends AuditType {
    override val value: String = "StockTransferFormStatus"
  }

  case object Sh03 extends AuditType {
    override val value: String = "NotifyPurchaseOfOwnSharesStatus"
  }

  case object UpscanValidation extends AuditType {
    override val value: String = "UpscanValidation"
  }

}
