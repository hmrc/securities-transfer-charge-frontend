/*
 * Copyright 2026 HM Revenue & Customs
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

package controllers.stf.individuals.bulk

import base.{FileUploadFixtures, SpecBase}
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.scalatestplus.mockito.MockitoSugar
import play.api.i18n.Lang
import play.api.inject.bind
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import uk.gov.hmrc.securitiestransferchargefrontend.controllers.routes
import uk.gov.hmrc.securitiestransferchargefrontend.controllers.stf.individuals.bulk.routes as bulkRoutes
import uk.gov.hmrc.securitiestransferchargefrontend.models.stf.fileupload.{ParsedStcRow, ParsedStcRowsDocument}
import uk.gov.hmrc.securitiestransferchargefrontend.navigation.Navigator
import uk.gov.hmrc.securitiestransferchargefrontend.repositories.ParsedStcRowsRepository
import uk.gov.hmrc.securitiestransferchargefrontend.services.stf.bulk.CheckYourAnswersService
import uk.gov.hmrc.securitiestransferchargefrontend.viewmodels.stf.fileupload.{CheckYourAnswersViewModel, Transfer}
import uk.gov.hmrc.securitiestransferchargefrontend.views.html.stf.individuals.bulk.CheckYourAnswersView

import scala.concurrent.Future

class CheckYourAnswersControllerSpec extends SpecBase with MockitoSugar with FileUploadFixtures {


  lazy val checkYourAnswersRoute: String = bulkRoutes.CheckYourAnswersController.onPageLoad(reference).url

  val row1: ParsedStcRow = parsedStcRow(1)
  val testFileName: String = "test-file.csv"

  "CheckYourAnswers Controller" - {

    "must return OK and the correct view for a GET when a document exists" in {

      val parsedStcRowsDocument = ParsedStcRowsDocument(
        _id = reference,
        rows = Seq(row1),
        fileName = testFileName
      )
      val viewModel = CheckYourAnswersViewModel(
        fileName = testFileName,
        numberOfTransfers = 1,
        taxDue = "£100.00",
        paymentDueBy = "26 September 2026",
        transfers = Seq(
          Transfer(
            seller = "Test Seller",
            securitiesBoughtIn = "Test Company",
            consideration = BigDecimal(10000),
            taxDue = BigDecimal(100)
          )
        )
      )

      val checkYourAnswersService = mock[CheckYourAnswersService]
      when(checkYourAnswersService.buildViewModel(any())(any[Lang]))
        .thenReturn(viewModel)

      val parsedStcRowsRepository = mock[ParsedStcRowsRepository]
      when(parsedStcRowsRepository.findDocumentByReference(reference))
        .thenReturn(Future.successful(Some(parsedStcRowsDocument)))

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers), affinityGroup = individualAffinity)
          .overrides(
            bind[Navigator].qualifiedWith("individuals").toInstance(getNavigator),
            bind[CheckYourAnswersService].toInstance(checkYourAnswersService),
            bind[ParsedStcRowsRepository].toInstance(parsedStcRowsRepository)
          )
          .build()

      running(application) {
        val request = FakeRequest(GET, checkYourAnswersRoute)

        val result = route(application, request).value

        val view = application.injector.instanceOf[CheckYourAnswersView]

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(viewModel, testBackLinkRoute)(request, messages(application)).toString
      }
    }

    "must redirect to Journey Recovery for a GET when no document exists" in {

      val parsedStcRowsRepository = mock[ParsedStcRowsRepository]

      when(parsedStcRowsRepository.findDocumentByReference(reference))
        .thenReturn(Future.successful(None))

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers), affinityGroup = individualAffinity)
          .overrides(
            bind[Navigator].qualifiedWith("individuals").toInstance(getNavigator),
            bind[ParsedStcRowsRepository].toInstance(parsedStcRowsRepository)
          )
          .build()

      running(application) {
        val request = FakeRequest(GET, checkYourAnswersRoute)

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
      }
    }

    "must redirect to the next page when valid data is submitted" in {

      val application =
        applicationBuilder(userAnswers = Some(emptyUserAnswers), affinityGroup = individualAffinity)
          .overrides(
            bind[Navigator].qualifiedWith("individuals").toInstance(getNavigator)
          )
          .build()

      running(application) {
        val request = FakeRequest(
          POST,
          bulkRoutes.CheckYourAnswersController.onSubmit().url
        )

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual testNextPage.url
      }
    }

  }
}