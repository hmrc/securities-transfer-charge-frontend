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

package uk.gov.hmrc.securitiestransferchargefrontend.forms.shared

import play.api.data.Form
import play.api.data.Forms.{mapping, optional}
import uk.gov.hmrc.securitiestransferchargefrontend.forms.mappings.Mappings
import uk.gov.hmrc.securitiestransferchargefrontend.models.shared.AgentReference

import javax.inject.Inject

class AgentReferenceFormProvider @Inject() extends Mappings {
  
  val maxLength = 255

  def apply(): Form[AgentReference] = Form(
    mapping(
      "value" ->
        optional(
          text()
            .verifying(maxLength(maxLength, "agentReference.error.length"))
        )
    )(AgentReference.apply)(x => Some(x.agentReference))
  )
}
