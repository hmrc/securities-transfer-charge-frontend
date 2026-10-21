/*
 * Copyright 2024 HM Revenue & Customs
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

package uk.gov.hmrc.securitiestransferchargefrontend.models.pagination

import play.api.libs.json.{Format, Json}
import uk.gov.hmrc.securitiestransferchargefrontend.models.search.SubmissionSummary
import uk.gov.hmrc.securitiestransferchargefrontend.viewmodels.pagination.PaginationViewModel

import java.util.Base64

case class TabPagination(page: Int, total: Int)

object TabPagination {
  implicit val format: Format[TabPagination] = Json.format[TabPagination]
}

case class PaginationModel(
                            tabs: Map[String, TabPagination] = Map.empty,
                            itemsPerPage: Int = 10
                          ) {

  def updateTabPage(tabId: String, page: Int): PaginationModel = {
    tabs.get(tabId) match {
      case Some(tabPagination) =>
        this.copy(tabs = tabs + (tabId -> tabPagination.copy(page = page)))
      case None =>
        this
    }
  }

  def getTabPage(tabId: String): Int = {
    tabs.get(tabId).map(_.page).getOrElse(1)
  }

  def getTabTotal(tabId: String): Int = {
    tabs.get(tabId).map(_.total).getOrElse(0)
  }

  def encode: String = {
    val json = Json.toJson(this).toString()
    Base64.getUrlEncoder.withoutPadding().encodeToString(json.getBytes("UTF-8"))
  }

  // Total pages calculations
  private def calculateTotalPages(total: Int): Int = {
    Math.ceil(total.toDouble / itemsPerPage).toInt
  }

  def getTabTotalPages(tabId: String): Int = {
    calculateTotalPages(getTabTotal(tabId))
  }

  // Pagination logic
  def paginateSubmissions(submissions: Seq[SubmissionSummary], currentPage: Int): Seq[SubmissionSummary] = {
    val startIndex = (currentPage - 1) * itemsPerPage
    val endIndex = startIndex + itemsPerPage
    submissions.slice(startIndex, endIndex)
  }

  def createPaginationIfNeeded(
                                tabId: String,
                                urlBuilder: (PaginationModel, Int) => String
                              ): Option[PaginationViewModel] = {
    val currentPage = getTabPage(tabId)
    val totalItems = getTabTotal(tabId)

    if (totalItems > itemsPerPage) {
      val urlForPage: Int => String = { page =>
        val updatedModel = this.updateTabPage(tabId, page)
        urlBuilder(updatedModel, page)
      }

      Some(PaginationViewModel(
        currentPage = currentPage,
        totalItems = totalItems,
        itemsPerPage = itemsPerPage,
        urlForPage = urlForPage
      ))
    } else None
  }
}

object PaginationModel {
  implicit val format: Format[PaginationModel] = Json.format[PaginationModel]

  val default: PaginationModel = PaginationModel()

  def forSubmissions(
                      draftsPage: Int = 1,
                      readyToPayPage: Int = 1,
                      overduePage: Int = 1,
                      recentPage: Int = 1,
                      totalDrafts: Int = 0,
                      totalReadyToPay: Int = 0,
                      totalOverdue: Int = 0,
                      totalRecent: Int = 0
                    ): PaginationModel = {
    PaginationModel(
      tabs = Map(
        "drafts" -> TabPagination(draftsPage, totalDrafts),
        "ready-to-pay" -> TabPagination(readyToPayPage, totalReadyToPay),
        "overdue" -> TabPagination(overduePage, totalOverdue),
        "recent-submissions" -> TabPagination(recentPage, totalRecent)
      )
    )
  }
}