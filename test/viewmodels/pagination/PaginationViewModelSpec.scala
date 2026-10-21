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

package viewmodels.pagination

import base.SpecBase
import uk.gov.hmrc.securitiestransferchargefrontend.models.pagination.PaginationModel
import uk.gov.hmrc.securitiestransferchargefrontend.viewmodels.pagination.PaginationViewModel

class PaginationViewModelSpec extends SpecBase {

  "PaginationViewModel" - {

    "display logic" - {
      "should not display pagination when total pages is 1" in {
        val paginationModel = PaginationModel(totalRecent = 5, itemsPerPage = 10)
        val viewModel = PaginationViewModel(1, 5, 10, "/test", "recent-submissions", paginationModel)

        viewModel.shouldDisplay mustBe false
        viewModel.totalPages mustBe 1
      }

      "should display pagination when total pages is greater than 1" in {
        val paginationModel = PaginationModel(totalRecent = 25, itemsPerPage = 10)
        val viewModel = PaginationViewModel(1, 25, 10, "/test", "recent-submissions", paginationModel)

        viewModel.shouldDisplay mustBe true
        viewModel.totalPages mustBe 3
      }
    }

    "page generation without ellipsis (≤12 pages)" - {
      "should show all pages for small pagination" in {
        val paginationModel = PaginationModel(totalRecent = 45, itemsPerPage = 10)
        val viewModel = PaginationViewModel(3, 45, 10, "/test", "recent-submissions", paginationModel)

        viewModel.pages must have size 5
        viewModel.pages.map(_.number) mustBe Seq(1, 2, 3, 4, 5)
        viewModel.pages.exists(_.isEllipsis) mustBe false
        viewModel.pages(2).isCurrent mustBe true
      }

      "should show all pages at showEllipsisLimit boundary" in {
        val paginationModel = PaginationModel(totalRecent = 125, itemsPerPage = 10)
        val viewModel = PaginationViewModel(6, 125, 10, "/test", "recent-submissions", paginationModel)

        viewModel.pages must have size 13
        viewModel.pages.map(_.number) mustBe (1 to 13)
        viewModel.pages.exists(_.isEllipsis) mustBe false
      }
    }

    "ellipsis logic (>12 pages)" - {
      "should show ellipsis at start of pagination" in {
        val paginationModel = PaginationModel(totalRecent = 155, itemsPerPage = 10)
        val viewModel = PaginationViewModel(1, 155, 10, "/test", "recent-submissions", paginationModel)

        viewModel.pages.head.number mustBe 1
        viewModel.pages.head.isCurrent mustBe true
        viewModel.pages.last.number mustBe 16
        viewModel.pages.count(_.isEllipsis) mustBe 1
      }

      "should show ellipsis at end of pagination" in {
        val paginationModel = PaginationModel(totalRecent = 195, itemsPerPage = 10)
        val viewModel = PaginationViewModel(20, 195, 10, "/test", "recent-submissions", paginationModel)

        viewModel.pages.head.number mustBe 1
        viewModel.pages.last.number mustBe 20
        viewModel.pages.last.isCurrent mustBe true
        viewModel.pages.count(_.isEllipsis) mustBe 1
      }

      "should show ellipsis on both sides for middle pages" in {
        val paginationModel = PaginationModel(totalRecent = 295, itemsPerPage = 10)
        val viewModel = PaginationViewModel(15, 295, 10, "/test", "recent-submissions", paginationModel)

        viewModel.pages.head.number mustBe 1
        viewModel.pages.last.number mustBe 30
        viewModel.pages.count(_.isEllipsis) mustBe 2
        
        val currentPageItem = viewModel.pages.find(_.isCurrent)
        currentPageItem.get.number mustBe 15
      }

      "ellipsis items should have number 0 and not be current" in {
        val paginationModel = PaginationModel(totalRecent = 195, itemsPerPage = 10)
        val viewModel = PaginationViewModel(10, 195, 10, "/test", "recent-submissions", paginationModel)

        val ellipsisItems = viewModel.pages.filter(_.isEllipsis)
        ellipsisItems.nonEmpty mustBe true
        ellipsisItems.foreach { item =>
          item.number mustBe 0
          item.isCurrent mustBe false
        }
      }
    }

    "navigation" - {
      "should handle previous and next page correctly" in {
        val paginationModel = PaginationModel(totalRecent = 50, itemsPerPage = 10)
        
        // First page
        val firstPage = PaginationViewModel(1, 50, 10, "/test", "recent-submissions", paginationModel)
        firstPage.previousPage mustBe None
        firstPage.nextPage mustBe Some(2)
        
        // Middle page
        val middlePage = PaginationViewModel(3, 50, 10, "/test", "recent-submissions", paginationModel)
        middlePage.previousPage mustBe Some(2)
        middlePage.nextPage mustBe Some(4)
        
        // Last page
        val lastPage = PaginationViewModel(5, 50, 10, "/test", "recent-submissions", paginationModel)
        lastPage.previousPage mustBe Some(4)
        lastPage.nextPage mustBe None
      }

      "should generate previous and next page calls correctly" in {
        val paginationModel = PaginationModel(totalRecent = 50, itemsPerPage = 10)
        val viewModel = PaginationViewModel(3, 50, 10, "/test", "recent-submissions", paginationModel)

        viewModel.previousPageCall.isDefined mustBe true
        viewModel.previousPageCall.get.url must include("recentPage=2")
        
        viewModel.nextPageCall.isDefined mustBe true
        viewModel.nextPageCall.get.url must include("recentPage=4")
      }
    }

    "URL generation" - {
      "should build URLs correctly for different tabs" in {
        val recentModel = PaginationModel(recentPage = 1, totalRecent = 50, itemsPerPage = 10)
        val recentVM = PaginationViewModel(1, 50, 10, "/dashboard", "recent-submissions", recentModel)
        recentVM.pageCall(2).url must include("recentPage=2")
        recentVM.pageCall(2).url must include("#recent-submissions")

        val draftsModel = PaginationModel(draftsPage = 1, totalDrafts = 50, itemsPerPage = 10)
        val draftsVM = PaginationViewModel(1, 50, 10, "/dashboard", "drafts", draftsModel)
        draftsVM.pageCall(3).url must include("draftsPage=3")
        draftsVM.pageCall(3).url must include("#drafts")
      }

      "should not include default page values in URL" in {
        val paginationModel = PaginationModel(totalRecent = 50, itemsPerPage = 10)
        val viewModel = PaginationViewModel(1, 50, 10, "/dashboard", "recent-submissions", paginationModel)

        val call = viewModel.pageCall(1)
        call.url mustNot include("?")
        call.url must include("#recent-submissions")
      }

      "should include multiple tab pages when navigating" in {
        val paginationModel = PaginationModel(
          draftsPage = 2,
          readyToPayPage = 3,
          recentPage = 1,
          totalRecent = 50,
          itemsPerPage = 10
        )
        val viewModel = PaginationViewModel(1, 50, 10, "/dashboard", "recent-submissions", paginationModel)

        val call = viewModel.pageCall(2)
        call.url must include("draftsPage=2")
        call.url must include("readyToPayPage=3")
        call.url must include("recentPage=2")
      }
    }

    "factory method" - {
      "should clamp current page to valid range" in {
        val paginationModel = PaginationModel(totalRecent = 50, itemsPerPage = 10)
        
        val tooLow = PaginationViewModel(0, 50, 10, "/test", "recent-submissions", paginationModel)
        tooLow.currentPage mustBe 1
        
        val tooHigh = PaginationViewModel(100, 50, 10, "/test", "recent-submissions", paginationModel)
        tooHigh.currentPage mustBe 5
      }

      "should handle zero total items" in {
        val paginationModel = PaginationModel(totalRecent = 0, itemsPerPage = 10)
        val viewModel = PaginationViewModel(1, 0, 10, "/test", "recent-submissions", paginationModel)

        viewModel.totalPages mustBe 0
        viewModel.shouldDisplay mustBe false
      }
    }

    "edge cases" - {
      "should handle boundary item counts" in {
        val paginationModel1 = PaginationModel(totalRecent = 1, itemsPerPage = 10)
        val singleItem = PaginationViewModel(1, 1, 10, "/test", "recent-submissions", paginationModel1)
        singleItem.totalPages mustBe 1
        singleItem.shouldDisplay mustBe false

        val paginationModel2 = PaginationModel(totalRecent = 10, itemsPerPage = 10)
        val exactPage = PaginationViewModel(1, 10, 10, "/test", "recent-submissions", paginationModel2)
        exactPage.totalPages mustBe 1
        exactPage.shouldDisplay mustBe false

        val paginationModel3 = PaginationModel(totalRecent = 11, itemsPerPage = 10)
        val overPage = PaginationViewModel(1, 11, 10, "/test", "recent-submissions", paginationModel3)
        overPage.totalPages mustBe 2
        overPage.shouldDisplay mustBe true
      }

      "should handle very large page counts" in {
        val paginationModel = PaginationModel(totalRecent = 1000, itemsPerPage = 10)
        val viewModel = PaginationViewModel(50, 1000, 10, "/test", "recent-submissions", paginationModel)

        viewModel.totalPages mustBe 100
        viewModel.shouldDisplay mustBe true
        viewModel.pages.exists(_.isEllipsis) mustBe true
      }
    }
  }
}