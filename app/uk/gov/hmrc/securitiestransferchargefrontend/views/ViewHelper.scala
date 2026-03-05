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

package uk.gov.hmrc.securitiestransferchargefrontend.views

import play.api.data.Form
import uk.gov.hmrc.govukfrontend.views.Aliases.SelectItem
import uk.gov.hmrc.securitiestransferchargefrontend.models.Relief

object ViewHelper {

  private def reliefToSelectItem(relief: Relief, selected: Boolean = false): SelectItem =
    SelectItem(
      value = Some(relief.name),
      text = s"${relief.name}",
      selected = selected,
      attributes = Map("id" -> relief.name)
    )

  /** Convert reliefs to SelectItems with blank option, marking the selected value from the form */
  def reliefsToSelectItems(reliefs: Seq[Relief], form: Form[_]): Seq[SelectItem] = {
    val blank = SelectItem(
      text = "",
      value = None,
      selected = form("reliefs").value.isEmpty
    )

    blank +: reliefs.map { relief =>
      val isSelected = form("reliefs").value.contains(relief.name)
      reliefToSelectItem(relief, selected = isSelected)
    }
  }

  /** Convert a plain list of reliefs to SelectItems (no selection) */
  def reliefsToSelectItems(reliefs: Seq[Relief]): Seq[SelectItem] =
    reliefs.map(relief => reliefToSelectItem(relief))
}


