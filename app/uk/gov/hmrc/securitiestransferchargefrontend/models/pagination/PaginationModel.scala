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

import play.api.i18n.Messages
import play.api.libs.json.{Format, Json}
import uk.gov.hmrc.securitiestransferchargefrontend.controllers.dashboard.routes
import uk.gov.hmrc.securitiestransferchargefrontend.models.search.SubmissionSummary
import uk.gov.hmrc.securitiestransferchargefrontend.viewmodels.pagination.PaginationViewModel

import java.util.Base64
import scala.util.Try

case class PaginationModel(
                            draftsPage: Int = 1,
                            readyToPayPage: Int = 1,
                            overduePage: Int = 1,
                            recentPage: Int = 1,
                            totalDrafts: Int = 0,
                            totalReadyToPay: Int = 0,
                            totalOverdue: Int = 0,
                            totalRecent: Int = 0,
                            itemsPerPage: Int = 10
                          ) {

  def updateDraftsPage(page: Int): PaginationModel = this.copy(draftsPage = page)

  def updateReadyToPayPage(page: Int): PaginationModel = this.copy(readyToPayPage = page)

  def updateOverduePage(page: Int): PaginationModel = this.copy(overduePage = page)

  def updateRecentPage(page: Int): PaginationModel = this.copy(recentPage = page)

  def encode: String = {
    val json = Json.toJson(this).toString()
    Base64.getUrlEncoder.withoutPadding().encodeToString(json.getBytes("UTF-8"))
  }

  // Total pages calculations
  private def calculateTotalPages(total: Int): Int = {
    Math.ceil(total.toDouble / itemsPerPage).toInt
  }

  def recentTotalPages: Int = calculateTotalPages(totalRecent)

  def draftsTotalPages: Int = calculateTotalPages(totalDrafts)

  def readyToPayTotalPages: Int = calculateTotalPages(totalReadyToPay)

  def overdueTotalPages: Int = calculateTotalPages(totalOverdue)

  // Pagination logic
  def paginateSubmissions(submissions: Seq[SubmissionSummary], currentPage: Int): Seq[SubmissionSummary] = {
    val startIndex = (currentPage - 1) * itemsPerPage
    val endIndex = startIndex + itemsPerPage
    submissions.slice(startIndex, endIndex)
  }

  private def createPaginationIfNeeded(
                                        currentPage: Int,
                                        totalItems: Int,
                                        tabId: String
                                      ): Option[PaginationViewModel] = {
    if (totalItems > itemsPerPage) {
      val urlForPage: Int => String = { page =>

        val updatedModel = tabId match {
          case id if id == "recent-submissions" => this.updateRecentPage(page)
          case id if id == "drafts" => this.updateDraftsPage(page)
          case id if id == "ready-to-pay" => this.updateReadyToPayPage(page)
          case id if id == "overdue" => this.updateOverduePage(page)
          case _ => this
        }

        val draftsPageParam = if (updatedModel.draftsPage != 1) Some(updatedModel.draftsPage) else None
        val readyToPayPageParam = if (updatedModel.readyToPayPage != 1) Some(updatedModel.readyToPayPage) else None
        val overduePageParam = if (updatedModel.overduePage != 1) Some(updatedModel.overduePage) else None
        val recentPageParam = if (updatedModel.recentPage != 1) Some(updatedModel.recentPage) else None

        routes.SubmissionsController.onPageLoad(
          draftsPage = draftsPageParam,
          readyToPayPage = readyToPayPageParam,
          overduePage = overduePageParam,
          recentPage = recentPageParam
        ).url + s"#$tabId"
      }

      Some(PaginationViewModel(
        currentPage = currentPage,
        totalItems = totalItems,
        itemsPerPage = itemsPerPage,
        urlForPage = urlForPage
      ))
    } else None
  }

  def getRecentPagination()(implicit messages: Messages): Option[PaginationViewModel] =
    createPaginationIfNeeded(recentPage, totalRecent, messages("submissions.recentSubmissions.tabId"))

  def getDraftsPagination()(implicit messages: Messages): Option[PaginationViewModel] =
    createPaginationIfNeeded(draftsPage, totalDrafts, messages("submissions.drafts.tabId"))

  def getReadyToPayPagination()(implicit messages: Messages): Option[PaginationViewModel] =
    createPaginationIfNeeded(readyToPayPage, totalReadyToPay, messages("submissions.readyToPay.tabId"))

  def getOverduePagination()(implicit messages: Messages): Option[PaginationViewModel] =
    createPaginationIfNeeded(overduePage, totalOverdue, messages("submissions.overdue.tabId"))
}

object PaginationModel {
  implicit val format: Format[PaginationModel] = Json.format[PaginationModel]

  val default: PaginationModel = PaginationModel()

  def decode(encoded: String): Option[PaginationModel] = {
    Try {
      val decoded = new String(Base64.getUrlDecoder.decode(encoded), "UTF-8")
      Json.parse(decoded).as[PaginationModel]
    }.toOption
  }

  def apply(
             draftsPage: Int,
             readyToPayPage: Int,
             overduePage: Int,
             recentPage: Int,
             totalDrafts: Int,
             totalReadyToPay: Int,
             totalOverdue: Int,
             totalRecent: Int
           ): PaginationModel = {
    new PaginationModel(
      draftsPage = draftsPage,
      readyToPayPage = readyToPayPage,
      overduePage = overduePage,
      recentPage = recentPage,
      totalDrafts = totalDrafts,
      totalReadyToPay = totalReadyToPay,
      totalOverdue = totalOverdue,
      totalRecent = totalRecent
    )
  }
}