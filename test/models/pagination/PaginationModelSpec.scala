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

package models.pagination

import base.SpecBase
import play.api.libs.json.Json
import uk.gov.hmrc.securitiestransferchargefrontend.models.pagination.PaginationModel
import uk.gov.hmrc.securitiestransferchargefrontend.models.search.{SubmissionStatus, SubmissionSummary}

import java.time.LocalDate

class PaginationModelSpec extends SpecBase {

  "PaginationModel" - {

    "create default PaginationModel with correct initial values" in {
      val model = PaginationModel.default

      model.draftsPage mustBe 1
      model.readyToPayPage mustBe 1
      model.overduePage mustBe 1
      model.recentPage mustBe 1
      model.totalDrafts mustBe 0
      model.totalReadyToPay mustBe 0
      model.totalOverdue mustBe 0
      model.totalRecent mustBe 0
      model.itemsPerPage mustBe 10
    }

    "update page numbers independently for each tab" in {
      val model = PaginationModel.default
      
      model.updateDraftsPage(3).draftsPage mustBe 3
      model.updateReadyToPayPage(2).readyToPayPage mustBe 2
      model.updateOverduePage(4).overduePage mustBe 4
      model.updateRecentPage(5).recentPage mustBe 5
    }

    "calculate total pages correctly for all tabs" in {
      val model = PaginationModel(
        totalRecent = 25,
        totalDrafts = 50,
        totalReadyToPay = 15,
        totalOverdue = 7,
        itemsPerPage = 10
      )

      model.recentTotalPages mustBe 3
      model.draftsTotalPages mustBe 5
      model.readyToPayTotalPages mustBe 2
      model.overdueTotalPages mustBe 1
    }

    "calculate total pages as 0 when no items" in {
      val model = PaginationModel.default

      model.recentTotalPages mustBe 0
      model.draftsTotalPages mustBe 0
      model.readyToPayTotalPages mustBe 0
      model.overdueTotalPages mustBe 0
    }

    "encode and decode PaginationModel correctly" in {
      val model = PaginationModel(
        draftsPage = 2,
        readyToPayPage = 3,
        overduePage = 1,
        recentPage = 4,
        totalDrafts = 20,
        totalReadyToPay = 30,
        totalOverdue = 10,
        totalRecent = 40,
        itemsPerPage = 10
      )

      val encoded = model.encode
      val decoded = PaginationModel.decode(encoded)

      decoded.isDefined mustBe true
      decoded.get mustBe model
    }

    "handle invalid encoded string gracefully" in {
      val decoded = PaginationModel.decode("invalid-base64-string")

      decoded mustBe None
    }

    "decode from query parameter with valid string" in {
      val model = PaginationModel(
        draftsPage = 2,
        readyToPayPage = 3,
        recentPage = 1,
        itemsPerPage = 10
      )
      val encoded = model.encode

      val result = PaginationModel.fromQueryParam(Some(encoded))

      result mustBe model
    }

    "return default model when query parameter is None" in {
      val result = PaginationModel.fromQueryParam(None)

      result mustBe PaginationModel.default
    }

    "return default model when query parameter is invalid" in {
      val result = PaginationModel.fromQueryParam(Some("invalid"))

      result mustBe PaginationModel.default
    }

    "serialize and deserialize to JSON correctly" in {
      val model = PaginationModel(
        draftsPage = 2,
        readyToPayPage = 3,
        overduePage = 1,
        recentPage = 4,
        totalDrafts = 20,
        totalReadyToPay = 30,
        totalOverdue = 10,
        totalRecent = 40,
        itemsPerPage = 10
      )

      val json = Json.toJson(model)
      val parsed = json.as[PaginationModel]

      parsed mustBe model
    }

    "paginate submissions correctly" in {
      val submissions = (1 to 25).map(i => 
        SubmissionSummary(
          submissionId = s"STC-$i",
          maybePaymentDueBy = None,
          maybeAgentReference = None,
          status = SubmissionStatus.Draft,
          sortDate = LocalDate.now()
        )
      )

      val model = PaginationModel(itemsPerPage = 10)
      
      // First page
      val page1 = model.paginateSubmissions(submissions, 1)
      page1.size mustBe 10
      page1.head.submissionId mustBe "STC-1"
      page1.last.submissionId mustBe "STC-10"
      
      // Middle page
      val page2 = model.paginateSubmissions(submissions, 2)
      page2.size mustBe 10
      page2.head.submissionId mustBe "STC-11"
      page2.last.submissionId mustBe "STC-20"
      
      // Last page with partial results
      val page3 = model.paginateSubmissions(submissions, 3)
      page3.size mustBe 5
      page3.head.submissionId mustBe "STC-21"
      page3.last.submissionId mustBe "STC-25"
      
      // Empty submissions
      model.paginateSubmissions(Seq.empty, 1) mustBe empty
    }

    "return pagination view model when items exceed itemsPerPage" in {
      val model = PaginationModel(
        recentPage = 2,
        draftsPage = 1,
        readyToPayPage = 3,
        overduePage = 2,
        totalRecent = 25,
        totalDrafts = 20,
        totalReadyToPay = 50,
        totalOverdue = 15,
        itemsPerPage = 10
      )

      // Recent pagination
      val recentPagination = model.getRecentPagination("/dashboard")
      recentPagination.isDefined mustBe true
      recentPagination.get.currentPage mustBe 2
      recentPagination.get.totalPages mustBe 3
      recentPagination.get.tab mustBe "recent-submissions"

      // Drafts pagination
      val draftsPagination = model.getDraftsPagination("/dashboard")
      draftsPagination.isDefined mustBe true
      draftsPagination.get.totalPages mustBe 2
      draftsPagination.get.tab mustBe "drafts"

      // Ready to pay pagination
      val readyToPayPagination = model.getReadyToPayPagination("/dashboard")
      readyToPayPagination.isDefined mustBe true
      readyToPayPagination.get.currentPage mustBe 3
      readyToPayPagination.get.totalPages mustBe 5
      readyToPayPagination.get.tab mustBe "ready-to-pay"

      // Overdue pagination
      val overduePagination = model.getOverduePagination("/dashboard")
      overduePagination.isDefined mustBe true
      overduePagination.get.totalPages mustBe 2
      overduePagination.get.tab mustBe "overdue"
    }

    "return None for pagination when items do not exceed itemsPerPage" in {
      val model = PaginationModel(
        totalRecent = 5,
        totalDrafts = 8,
        totalReadyToPay = 3,
        totalOverdue = 9,
        itemsPerPage = 10
      )

      model.getRecentPagination("/dashboard") mustBe None
      model.getDraftsPagination("/dashboard") mustBe None
      model.getReadyToPayPagination("/dashboard") mustBe None
      model.getOverduePagination("/dashboard") mustBe None
    }

    "maintain independent page numbers for each tab" in {
      val model = PaginationModel(
        draftsPage = 2,
        readyToPayPage = 3,
        overduePage = 1,
        recentPage = 4,
        itemsPerPage = 10
      )

      model.draftsPage mustBe 2
      model.readyToPayPage mustBe 3
      model.overduePage mustBe 1
      model.recentPage mustBe 4
    }

    "create model using factory method with all parameters" in {
      val model = PaginationModel(
        draftsPage = 2,
        readyToPayPage = 3,
        overduePage = 4,
        recentPage = 5,
        totalDrafts = 20,
        totalReadyToPay = 30,
        totalOverdue = 40,
        totalRecent = 50
      )

      model.draftsPage mustBe 2
      model.readyToPayPage mustBe 3
      model.overduePage mustBe 4
      model.recentPage mustBe 5
      model.totalDrafts mustBe 20
      model.totalReadyToPay mustBe 30
      model.totalOverdue mustBe 40
      model.totalRecent mustBe 50
      model.itemsPerPage mustBe 10
    }
  }
}