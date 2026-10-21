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

package views.components

import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import uk.gov.hmrc.securitiestransferchargefrontend.models.pagination.PaginationModel
import uk.gov.hmrc.securitiestransferchargefrontend.viewmodels.pagination.PaginationViewModel
import uk.gov.hmrc.securitiestransferchargefrontend.views.html.components.Pagination
import views.ViewBaseSpec

import scala.jdk.CollectionConverters.*

class PaginationViewSpec extends ViewBaseSpec {

  private val view = app.injector.instanceOf[Pagination]
  private val baseUrl = "/dashboard/submissions"

  "Pagination view" - {

    "when there is only one page" - {

      val paginationModel = PaginationModel(totalRecent = 5, itemsPerPage = 10)
      val viewModel = paginationModel.getRecentPagination()

      "must not render pagination component" in {
        viewModel mustBe None
      }
    }

    "when on the first page of multiple pages" - {

      val paginationModel = PaginationModel(totalRecent = 50, itemsPerPage = 10)
      val viewModel = paginationModel.getRecentPagination().get

      val html = view(viewModel)
      val document: Document = Jsoup.parse(html.toString())

      "must render pagination component correctly" in {
        document.select(".govuk-pagination").isEmpty mustBe false
        document.select(".govuk-pagination__prev").isEmpty mustBe true
        
        val nextLink = document.select(".govuk-pagination__next a")
        nextLink.isEmpty mustBe false
        nextLink.attr("href") must include("recentPage=2")
        nextLink.attr("href") must include("#recent-submissions")
        
        val pageItems = document.select(".govuk-pagination__item")
        pageItems.size() mustBe 5
        
        val currentPageItem = document.select(".govuk-pagination__item--current")
        currentPageItem.text() mustBe "1"
      }
    }

    "when on a middle page" - {

      val paginationModel = PaginationModel(draftsPage = 3, totalDrafts = 50, itemsPerPage = 10)
      val viewModel = paginationModel.getDraftsPagination().get

      val html = view(viewModel)
      val document: Document = Jsoup.parse(html.toString())

      "must render previous, next and highlight current page" in {
        document.select(".govuk-pagination__prev a").isEmpty mustBe false
        document.select(".govuk-pagination__next a").isEmpty mustBe false
        
        val currentPageItem = document.select(".govuk-pagination__item--current")
        currentPageItem.text() mustBe "3"
      }
    }

    "when on the last page" - {

      val paginationModel = PaginationModel(readyToPayPage = 5, totalReadyToPay = 50, itemsPerPage = 10)
      val viewModel = paginationModel.getReadyToPayPagination().get

      val html = view(viewModel)
      val document: Document = Jsoup.parse(html.toString())

      "must render previous link but not next link" in {
        document.select(".govuk-pagination__prev a").isEmpty mustBe false
        document.select(".govuk-pagination__next").isEmpty mustBe true
        
        val currentPageItem = document.select(".govuk-pagination__item--current")
        currentPageItem.text() mustBe "5"
      }
    }

    "when there are many pages" - {

      val paginationModel = PaginationModel(overduePage = 15, totalOverdue = 200, itemsPerPage = 10)
      val viewModel = paginationModel.getOverduePagination().get

      val html = view(viewModel)
      val document: Document = Jsoup.parse(html.toString())

      "must render with ellipsis and not show all pages" in {
        document.select(".govuk-pagination__prev a").isEmpty mustBe false
        document.select(".govuk-pagination__next a").isEmpty mustBe false
        
        val currentPageItem = document.select(".govuk-pagination__item--current")
        currentPageItem.text() mustBe "15"
        
        val pageItems = document.select(".govuk-pagination__item")
        pageItems.size() must be < 20
      }
    }

    "accessibility" - {

      val paginationModel = PaginationModel(recentPage = 2, totalRecent = 50, itemsPerPage = 10)
      val viewModel = paginationModel.getRecentPagination().get

      val html = view(viewModel)
      val document: Document = Jsoup.parse(html.toString())

      "must have navigation landmark" in {
        val nav = document.select("nav.govuk-pagination")
        nav.isEmpty mustBe false
      }

      "must have aria-label on navigation" in {
        val nav = document.select("nav.govuk-pagination")
        val ariaLabel = nav.attr("aria-label")
        ariaLabel must not be empty
      }

      "must have aria-label for page links" in {
        val pageLinks = document.select(".govuk-pagination__item a")
        pageLinks.asScala.foreach { link =>
          link.attr("aria-label") must not be empty
        }
      }
    }
  }
}