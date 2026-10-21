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
import uk.gov.hmrc.securitiestransferchargefrontend.models.pagination.{PaginationModel, TabPagination}
import uk.gov.hmrc.securitiestransferchargefrontend.models.search.{SubmissionStatus, SubmissionSummary}

import java.time.LocalDate
import java.util.Base64
import scala.util.Try

class PaginationModelSpec extends SpecBase {

  private def decode(encoded: String): Option[PaginationModel] = {
    Try {
      val decoded = new String(Base64.getUrlDecoder.decode(encoded), "UTF-8")
      Json.parse(decoded).as[PaginationModel]
    }.toOption
  }

  "PaginationModel" - {

    "create default PaginationModel with correct initial values" in {
      val model = PaginationModel.default

      model.tabs mustBe empty
      model.itemsPerPage mustBe 10
    }

    "forSubmissions factory method" - {
      "create model with all tabs configured" in {
        val model = PaginationModel.forSubmissions(
          draftsPage = 2,
          readyToPayPage = 3,
          overduePage = 1,
          recentPage = 4,
          totalDrafts = 20,
          totalReadyToPay = 30,
          totalOverdue = 10,
          totalRecent = 40
        )

        model.getTabPage("drafts") mustBe 2
        model.getTabPage("ready-to-pay") mustBe 3
        model.getTabPage("overdue") mustBe 1
        model.getTabPage("recent-submissions") mustBe 4

        model.getTabTotal("drafts") mustBe 20
        model.getTabTotal("ready-to-pay") mustBe 30
        model.getTabTotal("overdue") mustBe 10
        model.getTabTotal("recent-submissions") mustBe 40
      }

      "create model with default values" in {
        val model = PaginationModel.forSubmissions()

        model.getTabPage("drafts") mustBe 1
        model.getTabPage("ready-to-pay") mustBe 1
        model.getTabPage("overdue") mustBe 1
        model.getTabPage("recent-submissions") mustBe 1

        model.getTabTotal("drafts") mustBe 0
        model.getTabTotal("ready-to-pay") mustBe 0
        model.getTabTotal("overdue") mustBe 0
        model.getTabTotal("recent-submissions") mustBe 0
      }
    }

    "updateTabPage" - {
      "update page number for existing tab" in {
        val model = PaginationModel.forSubmissions(draftsPage = 1, totalDrafts = 50)
        val updated = model.updateTabPage("drafts", 3)

        updated.getTabPage("drafts") mustBe 3
        updated.getTabTotal("drafts") mustBe 50
      }

      "return unchanged model for non-existent tab" in {
        val model = PaginationModel.forSubmissions()
        val updated = model.updateTabPage("non-existent", 5)

        updated mustBe model
      }
    }

    "getTabPage" - {
      "return page number for existing tab" in {
        val model = PaginationModel.forSubmissions(draftsPage = 3)
        model.getTabPage("drafts") mustBe 3
      }

      "return 1 for non-existent tab" in {
        val model = PaginationModel.default
        model.getTabPage("non-existent") mustBe 1
      }
    }

    "getTabTotal" - {
      "return total for existing tab" in {
        val model = PaginationModel.forSubmissions(totalDrafts = 25)
        model.getTabTotal("drafts") mustBe 25
      }

      "return 0 for non-existent tab" in {
        val model = PaginationModel.default
        model.getTabTotal("non-existent") mustBe 0
      }
    }

    "getTabTotalPages" - {
      "calculate total pages correctly" in {
        val model = PaginationModel.forSubmissions(
          totalDrafts = 25,
          totalReadyToPay = 50,
          totalOverdue = 7
        )

        model.getTabTotalPages("drafts") mustBe 3
        model.getTabTotalPages("ready-to-pay") mustBe 5
        model.getTabTotalPages("overdue") mustBe 1
      }

      "return 0 for tab with no items" in {
        val model = PaginationModel.forSubmissions()
        model.getTabTotalPages("drafts") mustBe 0
      }
    }

    "encode and decode" - {
      "encode and decode PaginationModel correctly" in {
        val model = PaginationModel.forSubmissions(
          draftsPage = 2,
          readyToPayPage = 3,
          overduePage = 1,
          recentPage = 4,
          totalDrafts = 20,
          totalReadyToPay = 30,
          totalOverdue = 10,
          totalRecent = 40
        )

        val encoded = model.encode
        val decoded = decode(encoded)

        decoded.isDefined mustBe true
        decoded.get.getTabPage("drafts") mustBe 2
        decoded.get.getTabTotal("drafts") mustBe 20
      }

      "handle invalid encoded string gracefully" in {
        val decoded = decode("invalid-base64-string")
        decoded mustBe None
      }
    }

    "serialize and deserialize to JSON correctly" in {
      val model = PaginationModel.forSubmissions(
        draftsPage = 2,
        totalDrafts = 20
      )

      val json = Json.toJson(model)
      val parsed = json.as[PaginationModel]

      parsed.getTabPage("drafts") mustBe 2
      parsed.getTabTotal("drafts") mustBe 20
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

    "createPaginationIfNeeded" - {
      "return pagination view model when items exceed itemsPerPage with custom URL builder" in {
        val model = PaginationModel.forSubmissions(
          recentPage = 2,
          totalRecent = 25
        )

        val urlBuilder: (PaginationModel, Int) => String = { (model, _) =>
          s"/test?page=${model.getTabPage("recent-submissions")}#recent-submissions"
        }

        val pagination = model.createPaginationIfNeeded("recent-submissions", urlBuilder)

        pagination.isDefined mustBe true
        pagination.get.currentPage mustBe 2
        pagination.get.totalPages mustBe 3
        pagination.get.totalItems mustBe 25
      }

      "return None when items do not exceed itemsPerPage" in {
        val model = PaginationModel.forSubmissions(totalRecent = 5)

        val urlBuilder: (PaginationModel, Int) => String = { (_, page) =>
          s"/test?page=$page"
        }

        val pagination = model.createPaginationIfNeeded("recent-submissions", urlBuilder)

        pagination mustBe None
      }

      "use URL builder function correctly" in {
        val model = PaginationModel.forSubmissions(
          draftsPage = 1,
          totalDrafts = 25
        )

        var capturedModel: Option[PaginationModel] = None
        var capturedPage: Option[Int] = None

        val urlBuilder: (PaginationModel, Int) => String = { (model, page) =>
          capturedModel = Some(model)
          capturedPage = Some(page)
          s"/custom/url?page=$page"
        }

        val pagination = model.createPaginationIfNeeded("drafts", urlBuilder)

        pagination.isDefined mustBe true

        val _ = pagination.get.pageCall(2).url

        capturedModel.isDefined mustBe true
        capturedPage.isDefined mustBe true
        capturedModel.get.getTabPage("drafts") mustBe 2 // Should be updated to page 2
      }

      "work with any tab ID (generic)" in {
        val model = PaginationModel(
          tabs = Map(
            "custom-tab" -> TabPagination(1, 30)
          )
        )

        val urlBuilder: (PaginationModel, Int) => String = { (_, page) =>
          s"/custom?tab=custom-tab&page=$page"
        }

        val pagination = model.createPaginationIfNeeded("custom-tab", urlBuilder)

        pagination.isDefined mustBe true
        pagination.get.currentPage mustBe 1
        pagination.get.totalPages mustBe 3
      }
    }

    "TabPagination" - {
      "serialize and deserialize correctly" in {
        val tabPagination = TabPagination(page = 3, total = 50)
        val json = Json.toJson(tabPagination)
        val parsed = json.as[TabPagination]

        parsed mustBe tabPagination
      }
    }
  }
}