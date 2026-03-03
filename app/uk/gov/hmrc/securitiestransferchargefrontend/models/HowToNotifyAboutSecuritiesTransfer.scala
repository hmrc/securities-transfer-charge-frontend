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

package uk.gov.hmrc.securitiestransferchargefrontend.models

import play.api.i18n.Messages
import uk.gov.hmrc.govukfrontend.views.Aliases.{Hint, Text}
import uk.gov.hmrc.govukfrontend.views.viewmodels.radios.RadioItem

sealed trait HowToNotifyAboutSecuritiesTransfer

object HowToNotifyAboutSecuritiesTransfer extends Enumerable.Implicits {

  case object OneAtATime extends WithName("oneAtATime") with HowToNotifyAboutSecuritiesTransfer
  case object MoreThanOneAtATime extends WithName("moreThanOneAtATime") with HowToNotifyAboutSecuritiesTransfer

  val values: Seq[HowToNotifyAboutSecuritiesTransfer] = Seq(
    OneAtATime, MoreThanOneAtATime
  )

  def options(affinityGroupKey: String)(implicit messages: Messages): Seq[RadioItem] = {

    values.zipWithIndex.map { case (value, index) =>
      RadioItem(
        content = Text(messages(s"$affinityGroupKey.howToNotifyAboutSecuritiesTransfer.${value.toString}")),
        value = Some(value.toString),
        id = Some(s"value_$index"),
        hint = Some(
          Hint(content = Text(messages(s"$affinityGroupKey.howToNotifyAboutSecuritiesTransfer.${value.toString}.hint")))
        )
      )
    }
  }

  implicit val enumerable: Enumerable[HowToNotifyAboutSecuritiesTransfer] =
    Enumerable(values.map(v => v.toString -> v): _*)
}