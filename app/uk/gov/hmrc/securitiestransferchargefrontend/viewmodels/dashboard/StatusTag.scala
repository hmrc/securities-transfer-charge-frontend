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

package uk.gov.hmrc.securitiestransferchargefrontend.viewmodels.dashboard

import uk.gov.hmrc.govukfrontend.views.viewmodels.content.HtmlContent
import uk.gov.hmrc.securitiestransferchargefrontend.models.search.SubmissionStatus

object StatusTag {

  def cssClass(status: SubmissionStatus): String = status match {
    case SubmissionStatus.ReadyToPay  => "govuk-tag--blue"
    case SubmissionStatus.Draft       => "govuk-tag--grey"
    case SubmissionStatus.Overdue     => "govuk-tag--orange"
    case SubmissionStatus.Paid        => "govuk-tag--green"
    case SubmissionStatus.Processing  => "govuk-tag--yellow"
    case _                            => "govuk-tag--white"
  }

  def tagHtml(status: SubmissionStatus): HtmlContent = {
    HtmlContent(s"""<strong class="govuk-tag ${cssClass(status)}">${status.toString}</strong>""")
  }

  def labelWithTag(text: String, count: Int, status: SubmissionStatus): String = {
    s"""$text <strong class="govuk-tag govuk-!-margin-left-1 ${cssClass(status)}">$count</strong>"""
  }
}
