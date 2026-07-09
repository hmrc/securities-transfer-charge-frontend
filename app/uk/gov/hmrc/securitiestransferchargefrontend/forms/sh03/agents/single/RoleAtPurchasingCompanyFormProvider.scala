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

package uk.gov.hmrc.securitiestransferchargefrontend.forms.sh03.agents.single

import play.api.data.Forms.*
import play.api.data.format.Formatter
import play.api.data.{Form, FormError}
import uk.gov.hmrc.securitiestransferchargefrontend.models.sh03.shared.RoleAtPurchasingCompany

import javax.inject.Inject

class RoleAtPurchasingCompanyFormProvider @Inject() {

  private val uksOrganFormatter: Formatter[Option[String]] = new Formatter[Option[String]] {

    override def bind(key: String, data: Map[String, String]): Either[Seq[FormError], Option[String]] = {
      val role = data.getOrElse("role", "")
      val uksOrgan = data.get(key).filter(_.trim.nonEmpty)

      if (role == "ukSocietas") {
        uksOrgan match {
          case None => Left(Seq(FormError(key, "agent.sh03.roleAtPurchasingCompany.uksOrgan.error.required")))
          case Some(str) if str.length > 100 => Left(Seq(FormError(key, "agent.sh03.roleAtPurchasingCompany.uksOrgan.error.length")))
          case Some(str) => Right(Some(str))
        }
      } else {
        Right(uksOrgan)
      }
    }

    override def unbind(key: String, value: Option[String]): Map[String, String] =
      value.map(v => Map(key -> v)).getOrElse(Map.empty)
  }

  def apply(): Form[RoleAtPurchasingCompany] = Form(
    mapping(
      "role" -> default(text, "").verifying("agent.sh03.roleAtPurchasingCompany.error.required", _.trim.nonEmpty),
      "uksOrgan" -> of(uksOrganFormatter)
    )(RoleAtPurchasingCompany.apply)(o => Some((o.role, o.uksOrgan)))
  )
}