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
import uk.gov.hmrc.securitiestransferchargefrontend.viewmodels.pagination.PaginationViewModel

class PaginationViewModelSpec extends SpecBase {

  "PaginationViewModel" - {

    "generic factory method (urlForPage)" - {

      "display logic" - {
        "should not display pagination when total pages is 1" in {
          val viewModel = PaginationViewModel(
            currentPage = 1,
            totalItems = 5,
            itemsPerPage = 10,
            urlForPage = page => s"/test?page=$page"
          )

          viewModel.shouldDisplay mustBe false
          viewModel.totalPages mustBe 1
        }

        "should display pagination when total pages is greater than 1" in {
          val viewModel = PaginationViewModel(
            currentPage = 1,
            totalItems = 25,
            itemsPerPage = 10,
            urlForPage = page => s"/test?page=$page"
          )

          viewModel.shouldDisplay mustBe true
          viewModel.totalPages mustBe 3
        }
      }

      "navigation" - {
        "should handle previous and next page correctly" in {
          // First page
          val firstPage = PaginationViewModel(
            currentPage = 1,
            totalItems = 50,
            itemsPerPage = 10,
            urlForPage = page => s"/test?page=$page"
          )
          firstPage.previousPage mustBe None
          firstPage.nextPage mustBe Some(2)
          
          // Middle page
          val middlePage = PaginationViewModel(
            currentPage = 3,
            totalItems = 50,
            itemsPerPage = 10,
            urlForPage = page => s"/test?page=$page"
          )
          middlePage.previousPage mustBe Some(2)
          middlePage.nextPage mustBe Some(4)
          
          // Last page
          val lastPage = PaginationViewModel(
            currentPage = 5,
            totalItems = 50,
            itemsPerPage = 10,
            urlForPage = page => s"/test?page=$page"
          )
          lastPage.previousPage mustBe Some(4)
          lastPage.nextPage mustBe None
        }

        "should generate previous and next page calls correctly" in {
          val viewModel = PaginationViewModel(
            currentPage = 3,
            totalItems = 50,
            itemsPerPage = 10,
            urlForPage = page => s"/test?page=$page"
          )

          viewModel.previousPageCall.isDefined mustBe true
          viewModel.previousPageCall.get.url mustBe "/test?page=2"
          
          viewModel.nextPageCall.isDefined mustBe true
          viewModel.nextPageCall.get.url mustBe "/test?page=4"
        }
      }

      "URL generation" - {
        "should use custom URL builder function" in {
          val viewModel = PaginationViewModel(
            currentPage = 1,
            totalItems = 50,
            itemsPerPage = 10,
            urlForPage = page => s"/custom/path?tab=test&page=$page#anchor"
          )

          val call = viewModel.pageCall(2)
          call.url mustBe "/custom/path?tab=test&page=2#anchor"
        }
      }

      "factory method" - {
        "should clamp current page to valid range" in {
          val tooLow = PaginationViewModel(
            currentPage = 0,
            totalItems = 50,
            itemsPerPage = 10,
            urlForPage = page => s"/test?page=$page"
          )
          tooLow.currentPage mustBe 1
          
          val tooHigh = PaginationViewModel(
            currentPage = 100,
            totalItems = 50,
            itemsPerPage = 10,
            urlForPage = page => s"/test?page=$page"
          )
          tooHigh.currentPage mustBe 5
        }

        "should handle zero total items" in {
          val viewModel = PaginationViewModel(
            currentPage = 1,
            totalItems = 0,
            itemsPerPage = 10,
            urlForPage = page => s"/test?page=$page"
          )

          viewModel.totalPages mustBe 0
          viewModel.shouldDisplay mustBe false
        }
      }
    }
  }
}