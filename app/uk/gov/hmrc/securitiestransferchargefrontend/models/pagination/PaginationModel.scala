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
    totalPages: Int,
    totalItems: Int,
    baseUrl: String,
    tabId: String
  ): Option[PaginationViewModel] = {
    if (totalItems > itemsPerPage) {
      Some(PaginationViewModel(
        currentPage,
        totalPages,
        totalItems,
        itemsPerPage,
        baseUrl,
        tabId,
        this
      ))
    } else None
  }

  def getRecentPagination(baseUrl: String): Option[PaginationViewModel] =
    createPaginationIfNeeded(recentPage, recentTotalPages, totalRecent, baseUrl, "recent-submissions")

  def getDraftsPagination(baseUrl: String): Option[PaginationViewModel] =
    createPaginationIfNeeded(draftsPage, draftsTotalPages, totalDrafts, baseUrl, "drafts")

  def getReadyToPayPagination(baseUrl: String): Option[PaginationViewModel] =
    createPaginationIfNeeded(readyToPayPage, readyToPayTotalPages, totalReadyToPay, baseUrl, "ready-to-pay")

  def getOverduePagination(baseUrl: String): Option[PaginationViewModel] =
    createPaginationIfNeeded(overduePage, overdueTotalPages, totalOverdue, baseUrl, "overdue")
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

  def fromQueryParam(param: Option[String]): PaginationModel = {
    param.flatMap(decode).getOrElse(default)
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