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

package uk.gov.hmrc.securitiestransferchargefrontend.viewmodels.pagination

import play.api.mvc.Call

final case class PaginationViewModel private(
                                              currentPage: Int,
                                              totalPages: Int,
                                              totalItems: Int,
                                              itemsPerPage: Int,
                                              urlForPage: Int => String
                                            ) {

  private def getShowEllipsisLimit: Int = {
    val onPageAfterFirstPage = currentPage >= 2
    if (onPageAfterFirstPage) 11 else 15
  }

  private def getMiddleRange: Int = {
    val onPageAfter = currentPage >= 99
    if (onPageAfter) 3 else 4
  }

  def shouldDisplay: Boolean = totalPages > 1

  def previousPage: Option[Int] = if (currentPage > 1) Some(currentPage - 1) else None

  def nextPage: Option[Int] = if (currentPage < totalPages) Some(currentPage + 1) else None

  def pages: Seq[PageItem] = {
    val showEllipsisLimit = getShowEllipsisLimit //Show all pages if within limit (no ellipsis needed)
    val middleRange = getMiddleRange // Pages on each side of current

    if (totalPages <= showEllipsisLimit) {
      (1 to totalPages).map(page => PageItem(page, page == currentPage))
    } else {
      val items = scala.collection.mutable.ListBuffer[PageItem]()

      val pagesBeforeEllipsis = showEllipsisLimit - 1 // Pages to show before ellipsis
      val startThreshold = showEllipsisLimit - 3 // When to switch from start view
      val endThreshold = showEllipsisLimit - 3 // Pages from end to show end view

      val firstPage = 1

      if (currentPage <= startThreshold) {
        for (page <- firstPage to pagesBeforeEllipsis) {
          items += PageItem(page, page == currentPage)
        }
        items += PageItem(0, isEllipsis = true)
        items += PageItem(totalPages, currentPage == totalPages)

      } else if (currentPage >= totalPages - endThreshold) {
        items += PageItem(firstPage, currentPage == firstPage)
        items += PageItem(0, isEllipsis = true)
        val lastPagesStart = totalPages - pagesBeforeEllipsis + 1
        for (page <- lastPagesStart to totalPages) {
          items += PageItem(page, page == currentPage)
        }

      } else {
        items += PageItem(firstPage, currentPage == firstPage)
        items += PageItem(0, isEllipsis = true)

        for (page <- (currentPage - middleRange) to (currentPage + middleRange)) {
          items += PageItem(page, page == currentPage)
        }

        items += PageItem(0, isEllipsis = true)
        items += PageItem(totalPages, currentPage == totalPages)
      }

      items.toSeq
    }
  }

  def previousPageCall: Option[Call] = {
    previousPage.map(page => Call("GET", urlForPage(page)))
  }

  def nextPageCall: Option[Call] = {
    nextPage.map(page => Call("GET", urlForPage(page)))
  }

  def pageCall(page: Int): Call = {
    Call("GET", urlForPage(page))
  }
}

case class PageItem(
                     number: Int,
                     isCurrent: Boolean = false,
                     isEllipsis: Boolean = false
                   )

object PaginationViewModel {

  def apply(
             currentPage: Int,
             totalItems: Int,
             itemsPerPage: Int,
             urlForPage: Int => String
           ): PaginationViewModel = {
    val totalPages = Math.ceil(totalItems.toDouble / itemsPerPage).toInt
    PaginationViewModel(
      currentPage = Math.max(1, Math.min(currentPage, Math.max(1, totalPages))),
      totalPages = totalPages,
      totalItems = totalItems,
      itemsPerPage = itemsPerPage,
      urlForPage = urlForPage
    )
  }

}