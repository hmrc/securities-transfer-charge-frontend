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

      val paginationModel = PaginationModel.forSubmissions(totalRecent = 5)
      val viewModel = paginationModel.createPaginationIfNeeded("recent-submissions", (model, page) => s"$baseUrl?recent=$page")

      "must not render pagination component" in {
        viewModel mustBe None
      }
    }

    "when on the first page of multiple pages" - {

      val paginationModel = PaginationModel.forSubmissions(totalRecent = 50)
      val viewModel = paginationModel.createPaginationIfNeeded("recent-submissions", (model, page) => s"$baseUrl?recent=$page#recent-submissions").get

      val html = view(viewModel)
      val document: Document = Jsoup.parse(html.body)

      "must render pagination component correctly" in {
        document.select(".govuk-pagination").isEmpty mustBe false
        document.select(".govuk-pagination__prev").isEmpty mustBe true
        
        val nextLink = document.select(".govuk-pagination__next a")
        nextLink.isEmpty mustBe false
        nextLink.attr("href") must include("recent=2")
        nextLink.attr("href") must include("#recent-submissions")
        
        val pageItems = document.select(".govuk-pagination__item")
        pageItems.size() mustBe 5
        
        val currentPageItem = document.select(".govuk-pagination__item--current")
        currentPageItem.text() mustBe "1"
      }
    }

    "when on a middle page" - {

      val paginationModel = PaginationModel.forSubmissions(draftsPage = 3, totalDrafts = 50)
      val viewModel = paginationModel.createPaginationIfNeeded("drafts", (model, page) => s"$baseUrl?drafts=$page#drafts").get

      val html = view(viewModel)
      val document: Document = Jsoup.parse(html.body)

      "must render previous, next and highlight current page" in {
        document.select(".govuk-pagination__prev a").isEmpty mustBe false
        document.select(".govuk-pagination__next a").isEmpty mustBe false
        
        val currentPageItem = document.select(".govuk-pagination__item--current")
        currentPageItem.text() mustBe "3"
      }
    }

    "when on the last page" - {

      val paginationModel = PaginationModel.forSubmissions(readyToPayPage = 5, totalReadyToPay = 50)
      val viewModel = paginationModel.createPaginationIfNeeded("ready-to-pay", (model, page) => s"$baseUrl?readyToPay=$page#ready-to-pay").get

      val html = view(viewModel)
      val document: Document = Jsoup.parse(html.body)

      "must render previous link but not next link" in {
        document.select(".govuk-pagination__prev a").isEmpty mustBe false
        document.select(".govuk-pagination__next").isEmpty mustBe true
        
        val currentPageItem = document.select(".govuk-pagination__item--current")
        currentPageItem.text() mustBe "5"
      }
    }

    "when there are many pages" - {

      val paginationModel = PaginationModel.forSubmissions(overduePage = 15, totalOverdue = 200)
      val viewModel = paginationModel.createPaginationIfNeeded("overdue", (model, page) => s"$baseUrl?overdue=$page#overdue").get

      val html = view(viewModel)
      val document: Document = Jsoup.parse(html.body)

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

      val paginationModel = PaginationModel.forSubmissions(recentPage = 2, totalRecent = 50)
      val viewModel = paginationModel.createPaginationIfNeeded("recent-submissions", (model, page) => s"$baseUrl?recent=$page#recent-submissions").get

      val html = view(viewModel)
      val document: Document = Jsoup.parse(html.body)

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