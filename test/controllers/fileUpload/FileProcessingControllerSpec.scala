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

import base.SpecBase
import org.mockito.ArgumentMatchers.{any, eq as eqTo}
import org.mockito.Mockito.*
import org.scalatestplus.mockito.MockitoSugar
import play.api.inject.bind
import play.api.mvc.{Request, Result}
import play.api.test.FakeRequest
import play.api.test.Helpers.{redirectLocation, *}
import uk.gov.hmrc.auth.core.AffinityGroup
import uk.gov.hmrc.securitiestransferchargefrontend.config.FrontendAppConfig
import uk.gov.hmrc.securitiestransferchargefrontend.controllers.fileUpload.routes
import uk.gov.hmrc.securitiestransferchargefrontend.controllers.routes.JourneyRecoveryController
import uk.gov.hmrc.securitiestransferchargefrontend.controllers.stf.agents.bulk.routes as stfBulkRoutes
import uk.gov.hmrc.securitiestransferchargefrontend.controllers.sh03.agents.bulk.routes as sh03BulkRoutes
import uk.gov.hmrc.securitiestransferchargefrontend.controllers.sh03.organisations.bulk.routes as sh03OrgBulkRoutes
import uk.gov.hmrc.securitiestransferchargefrontend.controllers.stf.individuals.bulk.routes as individualBulkRoutes
import uk.gov.hmrc.securitiestransferchargefrontend.controllers.sh03.shared.bulk.routes as sh03CyaRoutes
import uk.gov.hmrc.securitiestransferchargefrontend.models.stf.upscan.UpscanCallbackRequest.UploadDetails
import uk.gov.hmrc.securitiestransferchargefrontend.models.{JourneyType, NormalMode}
import uk.gov.hmrc.securitiestransferchargefrontend.models.stf.upscan.{FileUpload, UpscanJourneyStatus}
import uk.gov.hmrc.securitiestransferchargefrontend.repositories.UpscanJourneyRepository
import uk.gov.hmrc.securitiestransferchargefrontend.services.fileupload.processing.{FileProcessingRefreshCounter, FileProcessingRefreshCounterFactory, ProcessingService}
import uk.gov.hmrc.securitiestransferchargefrontend.views.html.fileUpload.FileProcessingView

import java.time.Instant
import scala.concurrent.Future

class MockFileProcessingRefreshCounterFactory(counter: FileProcessingRefreshCounter)
  extends FileProcessingRefreshCounterFactory {

  override def apply(request: Request[?]): FileProcessingRefreshCounter = counter
}

class FileProcessingControllerSpec extends SpecBase with MockitoSugar {

  private val reference = "ref"

  private def buildApp(
                        counter: FileProcessingRefreshCounter,
                        repository: UpscanJourneyRepository,
                        service: ProcessingService,
                        affinityGroup: AffinityGroup = AffinityGroup.Individual
                      ) =
    applicationBuilder(userAnswers = Some(emptyUserAnswers), affinityGroup)
      .overrides(
        bind[FileProcessingRefreshCounterFactory]
          .toInstance(new MockFileProcessingRefreshCounterFactory(counter)),
        bind[UpscanJourneyRepository].toInstance(repository),
        bind[ProcessingService].toInstance(service)
      )
      .build()

  private def mockCounter() = {
    val counter = mock[FileProcessingRefreshCounter]
    when(counter.isTimedOut).thenReturn(false)
    when(counter.withIncrementedCounter(any[Result]))
      .thenAnswer(inv => inv.getArgument[Result](0))
    when(counter.reset(any[Result]))
      .thenAnswer(inv => inv.getArgument[Result](0))
    counter
  }

  "FileProcessingController" - {

    Seq(JourneyType.STF, JourneyType.SH03).foreach { journeyType =>

      s"For JourneyType $journeyType" - {

        def fakeUpload(status: UpscanJourneyStatus) =
          FileUpload(reference = reference, status = status, journeyType = journeyType)

        "must return OK and the file processing view for a GET" in {

          val counter = mockCounter()
          val repository = mock[UpscanJourneyRepository]
          val service = mock[ProcessingService]

          when(repository.find(reference))
            .thenReturn(Future.successful(Some(fakeUpload(UpscanJourneyStatus.Initiated))))


          val app = buildApp(counter, repository, service)

          running(app) {

            val request = FakeRequest(GET, routes.FileProcessingController.onPageLoad(reference).url)
            val result = route(app, request).value
            val view = app.injector.instanceOf[FileProcessingView]
            val appConfig = app.injector.instanceOf[FrontendAppConfig]
            val refreshInterval = appConfig.spinnerPageRefreshInterval

            status(result) mustEqual OK
            contentAsString(result) mustEqual view(refreshInterval)(request, messages(app)).toString

            verify(counter).withIncrementedCounter(any[Result])
          }
        }

        "must redirect to timeout page when the counter timed out and clear the session var" in {

          val counter = mock[FileProcessingRefreshCounter]
          when(counter.isTimedOut).thenReturn(true)
          when(counter.reset(any[Result]))
            .thenAnswer(inv => inv.getArgument[Result](0))

          val repository = mock[UpscanJourneyRepository]
          val service = mock[ProcessingService]

          val app = buildApp(counter, repository, service)

          running(app) {

            val request =
              FakeRequest(GET, routes.FileProcessingController.onPageLoad(reference).url)
                .withSession("retryCount" -> "50")
            val result = route(app, request).value

            status(result) mustEqual SEE_OTHER
            redirectLocation(result).value mustEqual
              routes.FileProcessingController.onTimeout(reference).url

            session(result).get("retryCount") mustBe None
          }
        }

        "must call processingService.processReadyUpload and return to the file processing page when status is Ready" in {

          val counter = mockCounter()
          val repository = mock[UpscanJourneyRepository]
          val service = mock[ProcessingService]

          val upload = fakeUpload(UpscanJourneyStatus.Ready)

          when(repository.find(reference))
            .thenReturn(Future.successful(Some(upload)))

          when(service.processReadyUpload(any(), any(), any(), any())(any(), any(), any()))
            .thenReturn(Future.successful(()))

          val app = buildApp(counter, repository, service)

          running(app) {

            val result =
              route(app, FakeRequest(GET, routes.FileProcessingController.onPageLoad(reference).url)).value

            status(result) mustEqual OK

            verify(service)
              .processReadyUpload(eqTo(reference), eqTo(upload), any[String], eqTo(journeyType))(any(), any(), any())

            verify(counter).withIncrementedCounter(any[Result])
          }
        }

        "must return to the file processing page when the status is Processing" in {

          val counter = mockCounter()
          val repository = mock[UpscanJourneyRepository]
          val service = mock[ProcessingService]

          when(repository.find(reference))
            .thenReturn(Future.successful(Some(fakeUpload(UpscanJourneyStatus.Processing))))

          val app = buildApp(counter, repository, service)

          running(app) {

            val result =
              route(app, FakeRequest(GET, routes.FileProcessingController.onPageLoad(reference).url)).value

            status(result) mustEqual OK

            verify(counter).withIncrementedCounter(any[Result])
          }
        }

        "must redirect for RowLimitExceeded" in {

          val counter = mockCounter()
          val repository = mock[UpscanJourneyRepository]
          val service = mock[ProcessingService]

          when(repository.find(reference))
            .thenReturn(Future.successful(Some(fakeUpload(UpscanJourneyStatus.RowLimitExceeded))))

          val app = buildApp(counter, repository, service)

          running(app) {

            val result = route(app, FakeRequest(GET, routes.FileProcessingController.onPageLoad(reference).url)).value

            status(result) mustEqual SEE_OTHER
            redirectLocation(result).value mustEqual
              routes.BulkRowsErrorController.onPageLoad(journeyType).url
          }
        }

        "must redirect for an EmptyFile" in {

          val counter = mockCounter()
          val repository = mock[UpscanJourneyRepository]
          val service = mock[ProcessingService]

          when(repository.find(reference))
            .thenReturn(Future.successful(Some(fakeUpload(UpscanJourneyStatus.EmptyFile))))

          val app = buildApp(counter, repository, service)

          running(app) {

            val result = route(app, FakeRequest(GET, routes.FileProcessingController.onPageLoad(reference).url)).value

            status(result) mustEqual SEE_OTHER
            redirectLocation(result).value mustEqual
              routes.BulkUploadFileEmptyController.onPageLoad(journeyType).url
          }
        }

        "must redirect for FormatingErrors greater than 25" in {

          val counter = mockCounter()
          val repository = mock[UpscanJourneyRepository]
          val service = mock[ProcessingService]

          when(repository.find(reference))
            .thenReturn(Future.successful(Some(fakeUpload(UpscanJourneyStatus.TooManyErrors))))

          val app = buildApp(counter, repository, service)

          running(app) {

            val result = route(app, FakeRequest(GET, routes.FileProcessingController.onPageLoad(reference).url)).value

            status(result) mustEqual SEE_OTHER
            redirectLocation(result).value mustEqual
              routes.FormattingErrorController.onPageLoad(journeyType).url
          }
        }

        "must redirect for FormatingErrors less than 25" in {

          val counter = mockCounter()
          val repository = mock[UpscanJourneyRepository]
          val service = mock[ProcessingService]

          when(repository.find(reference))
            .thenReturn(Future.successful(Some(fakeUpload(UpscanJourneyStatus.FormatingErrors))))

          val app = buildApp(counter, repository, service)

          running(app) {

            val result = route(app, FakeRequest(GET, routes.FileProcessingController.onPageLoad(reference).url)).value

            status(result) mustEqual SEE_OTHER
            redirectLocation(result).value mustEqual
              routes.UploadedFileErrorController.onPageLoad(reference,journeyType).url
          }
        }

        "must redirect for an UpscanDownloadError" in {

          val counter = mockCounter()
          val repository = mock[UpscanJourneyRepository]
          val service = mock[ProcessingService]

          when(repository.find(reference))
            .thenReturn(Future.successful(Some(fakeUpload(UpscanJourneyStatus.UpscanDownloadError))))

          val app = buildApp(counter, repository, service)

          running(app) {

            val result = route(app, FakeRequest(GET, routes.FileProcessingController.onPageLoad(reference).url)).value

            status(result) mustEqual SEE_OTHER
            redirectLocation(result).value mustEqual
              routes.BulkUploadErrorController.onPageLoad(journeyType).url
          }
        }

        "must redirect for an InvalidTemplate" in {

          val counter = mockCounter()
          val repository = mock[UpscanJourneyRepository]
          val service = mock[ProcessingService]

          when(repository.find(reference))
            .thenReturn(Future.successful(Some(fakeUpload(UpscanJourneyStatus.InvalidTemplate))))

          val app = buildApp(counter, repository, service)

          running(app) {

            val result = route(app, FakeRequest(GET, routes.FileProcessingController.onPageLoad(reference).url)).value

            status(result) mustEqual SEE_OTHER
            redirectLocation(result).value mustEqual
              routes.BulkUploadInvalidTemplateController.onPageLoad(journeyType).url
          }
        }

        "must redirect for a FileParseError" in {

          val counter = mockCounter()
          val repository = mock[UpscanJourneyRepository]
          val service = mock[ProcessingService]

          when(repository.find(reference))
            .thenReturn(Future.successful(Some(fakeUpload(UpscanJourneyStatus.FileParseError))))

          val app = buildApp(counter, repository, service)

          running(app) {

            val result = route(app, FakeRequest(GET, routes.FileProcessingController.onPageLoad(reference).url)).value

            status(result) mustEqual SEE_OTHER
            redirectLocation(result).value mustEqual JourneyRecoveryController.onPageLoad().url
          }
        }

        "must redirect for a failed upload" in {

          val counter = mockCounter()
          val repository = mock[UpscanJourneyRepository]
          val service = mock[ProcessingService]

          val upload = fakeUpload(UpscanJourneyStatus.Failed)

          when(repository.find(reference))
            .thenReturn(Future.successful(Some(upload)))

          val app = buildApp(counter, repository, service)

          running(app) {

            val result = route(app, FakeRequest(GET, routes.FileProcessingController.onPageLoad(reference).url)).value

            status(result) mustEqual SEE_OTHER
            redirectLocation(result).value mustEqual routes.BulkUploadErrorController.onPageLoad(journeyType).url
          }
        }

        "must redirect for a failed upload (encrypted)" in {

          val counter = mockCounter()
          val repository = mock[UpscanJourneyRepository]
          val service = mock[ProcessingService]

          val upload = fakeUpload(UpscanJourneyStatus.Failed)
            .copy(failureReason = Some("QUARANTINE"),message = Some("EncryptedDoc"))

          when(repository.find(reference))
            .thenReturn(Future.successful(Some(upload)))

          val app = buildApp(counter, repository, service)

          running(app) {

            val result = route(app, FakeRequest(GET, routes.FileProcessingController.onPageLoad(reference).url)).value

            status(result) mustEqual SEE_OTHER
            redirectLocation(result).value mustEqual
              routes.EncryptedFileErrorController.onPageLoad(journeyType).url
          }
        }

        "must redirect for a failed upload (virus)" in {

          val counter = mockCounter()
          val repository = mock[UpscanJourneyRepository]
          val service = mock[ProcessingService]

          val upload = fakeUpload(UpscanJourneyStatus.Failed)
            .copy(failureReason = Some("QUARANTINE"),message = Some("virus"))

          when(repository.find(reference))
            .thenReturn(Future.successful(Some(upload)))

          val app = buildApp(counter, repository, service)

          running(app) {

            val result = route(app, FakeRequest(GET, routes.FileProcessingController.onPageLoad(reference).url)).value

            status(result) mustEqual SEE_OTHER
            redirectLocation(result).value mustEqual
              routes.BulkUploadVirusErrorController.onPageLoad(journeyType).url
          }
        }

        Seq(
          AffinityGroup.Individual,
          AffinityGroup.Organisation,
          AffinityGroup.Agent
        ).foreach {affinityGroup =>
          s"must redirect for a successful upload and validation for $affinityGroup" in {

            val counter = mockCounter()
            val repository = mock[UpscanJourneyRepository]
            val service = mock[ProcessingService]

            when(repository.find(reference))
              .thenReturn(Future.successful(Some(fakeUpload(UpscanJourneyStatus.Completed))))

            val app = buildApp(counter, repository, service, affinityGroup)

            running(app) {

              val result = route(app, FakeRequest(GET, routes.FileProcessingController.onPageLoad(reference).url)).value

              status(result) mustEqual SEE_OTHER


              (affinityGroup, journeyType) match {
                case (AffinityGroup.Agent, JourneyType.STF) => redirectLocation(result).value mustEqual stfBulkRoutes.AgentReferenceController.onPageLoad(NormalMode, reference).url

                case (AffinityGroup.Agent, JourneyType.SH03) => redirectLocation(result).value mustEqual sh03BulkRoutes.RoleAtPurchasingCompanyController.onPageLoad(NormalMode).url

                case (AffinityGroup.Organisation, JourneyType.SH03) => redirectLocation(result).value mustEqual sh03OrgBulkRoutes.RoleAtPurchasingCompanyController.onPageLoad(NormalMode).url

                case (_, JourneyType.STF) => redirectLocation(result).value mustEqual individualBulkRoutes.CheckYourAnswersController.onPageLoad(reference).url

                case (_, JourneyType.SH03) => redirectLocation(result).value mustEqual sh03CyaRoutes.CheckYourAnswersController.onPageLoad().url
              }
            }
          }
        }

        "must redirect when no file upload is found" in {

          val counter = mockCounter()
          val repository = mock[UpscanJourneyRepository]
          val service = mock[ProcessingService]

          when(repository.find(reference))
            .thenReturn(Future.successful(None))

          val app = buildApp(counter, repository, service)

          running(app) {

            val result = route(app, FakeRequest(GET, routes.FileProcessingController.onPageLoad(reference).url)).value

            status(result) mustEqual SEE_OTHER
            redirectLocation(result).value mustEqual JourneyRecoveryController.onPageLoad().url
          }
        }

        "must redirect for a failed upload (Invalid file type)" in {

          val counter = mockCounter()
          val repository = mock[UpscanJourneyRepository]
          val service = mock[ProcessingService]

          val upload = fakeUpload(UpscanJourneyStatus.Failed).copy(failureReason = Some("REJECTED"), message = Some("mime type"))

          when(repository.find(reference)).thenReturn(Future.successful(Some(upload)))

          val app = buildApp(counter, repository, service)

          running(app) {

            val result = route(app, FakeRequest(GET, routes.FileProcessingController.onPageLoad(reference).url)).value

            status(result) mustEqual SEE_OTHER
            redirectLocation(result).value mustEqual routes.FileTypeErrorController.onPageLoad(journeyType).url
          }
        }

        "must redirect for an empty file (zero bytes)" in {

          val counter = mockCounter()
          val repository = mock[UpscanJourneyRepository]
          val service = mock[ProcessingService]

          val upload = FileUpload(reference = reference,
            status = UpscanJourneyStatus.Ready,
            downloadUrl = Some("some-url.com"),
            uploadDetails = Some(UploadDetails(
              uploadTimestamp = Instant.now(),
              checksum = "checksum",
              fileMimeType = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
              fileName = "zeroBytes.xlxs",
              size = 0)),journeyType = journeyType)

          when(repository.find(reference)).thenReturn(Future.successful(Some(upload)))

          val app = buildApp(counter, repository, service)

          running(app) {

            val result = route(app, FakeRequest(GET, routes.FileProcessingController.onPageLoad(reference).url)).value

            status(result) mustEqual SEE_OTHER
            redirectLocation(result).value mustEqual routes.BulkUploadFileEmptyController.onPageLoad(journeyType).url
          }
        }
      }
    }
  }
}