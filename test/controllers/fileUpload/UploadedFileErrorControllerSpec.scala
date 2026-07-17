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

package controllers.fileUpload

import base.{FileUploadFixtures, SpecBase}
import org.mockito.Mockito.{reset, when}
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.mockito.MockitoSugar.mock
import play.api.inject
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import uk.gov.hmrc.securitiestransferchargefrontend.controllers.fileUpload.routes
import uk.gov.hmrc.securitiestransferchargefrontend.models.JourneyType
import uk.gov.hmrc.securitiestransferchargefrontend.repositories.ValidationErrorRepository
import uk.gov.hmrc.securitiestransferchargefrontend.viewmodels.stf.fileupload.UploadedFileErrorMapper
import uk.gov.hmrc.securitiestransferchargefrontend.views.html.fileUpload.UploadedFileErrorView

import scala.concurrent.Future

class UploadedFileErrorControllerSpec extends SpecBase with BeforeAndAfterEach with FileUploadFixtures {

  private val mockValidationErrorRepository = mock[ValidationErrorRepository]

  override protected def beforeEach(): Unit = {
    super.beforeEach()
    reset(mockValidationErrorRepository)
  }


  "UploadedFileErrorController" - {

    Seq(JourneyType.STF, JourneyType.SH03).foreach { journeyType =>

      s"$journeyType must return OK and the correct view for a GET" in {

        val application =
          applicationBuilder(userAnswers = Some(emptyUserAnswers))
            .overrides(
              inject.bind[ValidationErrorRepository].toInstance(mockValidationErrorRepository)
            ).build()

        when(mockValidationErrorRepository.findByReference(reference))
          .thenReturn(Future.successful(blockingValidationErrors))

        running(application) {

          val request =
            FakeRequest(GET, routes.UploadedFileErrorController.onPageLoad(reference, journeyType).url)

          val result = route(application, request).value

          val view = application.injector.instanceOf[UploadedFileErrorView]

          status(result) mustEqual OK

          contentAsString(result) mustEqual view(UploadedFileErrorMapper.from(blockingValidationErrors), journeyType)(request, messages(application)).toString
        }
      }
    }
  }
}