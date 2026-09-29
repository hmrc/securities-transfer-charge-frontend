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

import play.api.i18n.Messages
import uk.gov.hmrc.govukfrontend.views.viewmodels.content.HtmlContent
import uk.gov.hmrc.govukfrontend.views.viewmodels.tabs.{TabItem, TabPanel, Tabs}

case class SubmissionsViewModel(tabs: Tabs)

object SubmissionsViewModel {

  private def buildPanelContent(heading: String, count: Int, emptyText: String, table: String): TabPanel = {
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

  private def buildTabs()(implicit messages: Messages): Tabs = {

    val recentSubmissionsTab = TabItem(
      id = Some(messages("submissions.recentSubmissions.tabId")),
      label = messages("submissions.recentSubmissions.panel.heading"),
      panel = buildPanelContent(
        heading = messages("submissions.recentSubmissions.panel.heading"),
        count = 0,
        emptyText = messages("submissions.noRecentSubmissions"),
        table = recentSubmissionsTable))

    val draftSubmissionsTab = TabItem(
      id = Some(messages("submissions.drafts.tabId")),
      label = labelWithTag(messages("submissions.drafts.panel.heading"), 0, "govuk-tag--grey"),
      panel = buildPanelContent(
        heading = messages("submissions.drafts.panel.heading"), 
        count = 0, 
        emptyText = messages("submissions.noDraftSubmissions"), 
        table = draftsTable))

    val readyToPaySubmissionsTab = TabItem(
      id = Some(messages("submissions.readyToPay.tabId")),
      label = labelWithTag(messages("submissions.readyToPay.panel.heading"), 0, "govuk-tag--blue"),
      panel = buildPanelContent(
        heading = messages("submissions.readyToPay.panel.heading"), 
        count = 0, 
        emptyText = messages("submissions.noReadyToPaySubmissions"), 
        table = readyToPayTable))

    val overdueSubmissionsTab = TabItem(
      id = Some(messages("submissions.overdue.tabId")),
      label = labelWithTag(messages("submissions.overdue.panel.heading"), 0, "govuk-tag--red"),
      panel = buildPanelContent(
        heading = messages("submissions.overdue.panel.heading"), 
        count = 0, 
        emptyText = messages("submissions.noOverdueSubmissions"), 
        table = overdueTable))

    Tabs(items = Seq(recentSubmissionsTab, draftSubmissionsTab, readyToPaySubmissionsTab, overdueSubmissionsTab))
  }
  
  // TODO: replace with a factory that take submissions retrieved from ETMP and save and return  
  def empty()(implicit messages: Messages): SubmissionsViewModel = {
    SubmissionsViewModel(tabs = buildTabs())
  }
}