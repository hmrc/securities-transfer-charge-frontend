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

package uk.gov.hmrc.securitiestransferchargefrontend.viewmodels.stf.individuals

import uk.gov.hmrc.govukfrontend.views.viewmodels.content.HtmlContent
import uk.gov.hmrc.govukfrontend.views.viewmodels.tabs.{TabItem, TabPanel, Tabs}
import uk.gov.hmrc.securitiestransferchargefrontend.services.DashboardCounts

case class SubmissionsViewModel(tabs: Tabs, overdueCount: Int, readyToPayCount: Int, draftCount: Int)

object SubmissionsViewModel {

  private def buildPanelContent(heading: String, count: Int = 0, emptyText: String, table: String): TabPanel = {
    val body = if (count == 0) s"""<p class="govuk-body">$emptyText</p>""" else table

    TabPanel(content = HtmlContent(s"""<h2 class="govuk-heading-m">$heading</h2>$body"""))
  }

  // TODO: build the tables for each view
  private def recentSubmissionsTable: String = ""
  private def draftsTable: String = ""
  private def readyToPayTable: String = ""
  private def overdueTable: String = ""


  private def labelWithTag(text: String, count: Int, tagClass: String): String =
    s"""$text <strong class="govuk-tag govuk-!-margin-left-1 $tagClass">$count</strong>"""

  private def buildTabs(counts: DashboardCounts): Tabs = {

    val recentSubmissionsTab = TabItem(
      id = Some("recent-submissions"),
      label = "Recent submissions",
      panel = buildPanelContent(heading = "Recent submissions", emptyText = "You have no submissions from the last 18 months.", table = recentSubmissionsTable))

    val draftSubmissionsTab = TabItem(
      id = Some("drafts"),
      label = labelWithTag("Drafts", counts.drafts, "govuk-tag--grey"),
      panel = buildPanelContent(heading = "Drafts", count = counts.drafts, emptyText = "You have no draft submissions.", table = draftsTable))

    val readyToPaySubmissionsTab = TabItem(
      id = Some("ready-to-pay"),
      label = labelWithTag("Ready to pay", counts.readyToPay, "govuk-tag--blue"),
      panel = buildPanelContent(heading = "Ready to pay", count = counts.readyToPay, emptyText = "You have no ready to pay submissions.", table = readyToPayTable))

    val overdueSubmissionsTab = TabItem(
      id = Some("overdue"),
      label = labelWithTag("Overdue", counts.overdue, "govuk-tag--red"),
      panel = buildPanelContent(heading = "Overdue", count = counts.overdue, emptyText = "You have no overdue submissions.", table = overdueTable))

    Tabs(items = Seq(recentSubmissionsTab, draftSubmissionsTab, readyToPaySubmissionsTab, overdueSubmissionsTab))
  }

  def fromCounts(counts: DashboardCounts): SubmissionsViewModel =
    SubmissionsViewModel(
      tabs = buildTabs(counts),
      overdueCount = counts.overdue,
      readyToPayCount = counts.readyToPay,
      draftCount = counts.drafts
    )
}