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

import uk.gov.hmrc.securitiestransferchargefrontend.models.Relief

/** Helpers for checking a relief name from an uploaded file against the current list of reliefs. */
object ReliefNames {

  def from(reliefs: Seq[Relief]): Set[String] = reliefs.map(r => normalise(r.name)).toSet

  /** \`allowed\` is the result of [[from]]. Matching ignores case and surrounding spaces. */
  def isAllowed(value: String, allowed: Set[String]): Boolean = allowed.contains(normalise(value))

  private def normalise(value: String): String = value.trim.toLowerCase
}
