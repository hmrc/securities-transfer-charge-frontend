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

package controllers.stf.agents.bulk

import base.SpecBase
import base.stubs.StubPersistentNavigator
import com.google.inject.name.Names
import org.mockito.ArgumentMatchers.anyString
import org.scalatestplus.mockito.MockitoSugar.mock
import org.mockito.Mockito.when
import play.api.inject.bind
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import uk.gov.hmrc.securitiestransferchargefrontend.controllers.stf.agents.bulk.routes
import uk.gov.hmrc.securitiestransferchargefrontend.models.shared.AgentReference
import uk.gov.hmrc.securitiestransferchargefrontend.models.stf.HowToNotifyAboutSecuritiesTransfer
import uk.gov.hmrc.securitiestransferchargefrontend.models.stf.fileupload.{ParsedStcRow, ParsedStcRowsDocument, ParsedValue}
import uk.gov.hmrc.securitiestransferchargefrontend.navigation.{Navigator, PersistentNavigator}
import uk.gov.hmrc.securitiestransferchargefrontend.pages.stf.shared.{AgentReferencePage, HowToNotifyAboutSecuritiesTransferPage}
import uk.gov.hmrc.securitiestransferchargefrontend.repositories.ParsedStcRowsRepository

import java.time.{Instant, LocalDate}
import scala.concurrent.Future

class CheckYourAnswersControllerSpec extends SpecBase {

  private def completeUserAnswers = {
    emptyUserAnswers
      .set(HowToNotifyAboutSecuritiesTransferPage,HowToNotifyAboutSecuritiesTransfer.MoreThanOneAtATime).success.value
      .set(AgentReferencePage,AgentReference(Some("Reference1"))).success.value
      .setFileUploadReference("testReference")
  }

  private val validRow: ParsedStcRow =
    ParsedStcRow(
      rowNumber = 3,
      buyerName = Some("Bob buyer"),
      buyerAddressInUK = Some(true),
      buyerAddressLine1 = Some("1 Seller Street"),
      buyerAddressLine2 = Some("Seller District"),
      buyerAddressLine3 = Some("Seller City"),
      buyerAddressLine4 = None,
      buyerPostcode = Some("AA1 1AA"),
      buyerCountry = Some("United Kingdom"),
      sellerName = Some("Seller Ltd"),
      sellerAddressInUK = Some(true),
      sellerAddressLine1 = Some("1 Test"),
      sellerAddressLine2 = Some("Test Region"),
      sellerAddressLine3 = None,
      sellerAddressLine4 = None,
      sellerPostcode = Some("AA1 1AA"),
      sellerCountry = None,
      connectedPersons = Some(true),
      applyingForRelief = Some(false),
      whatReliefAreYouApplyingFor = None,
      securitiesTarget = Some("Target Ltd"),
      companyRegistrationNumber = Some("12345678"),
      chargingPoint = ParsedValue.Valid(LocalDate.of(2026, 2, 20)),
      taxRate = Some(BigDecimal("0.5")),
      whatTypeOfSecurities = Some("Shares"),
      typeOfShares = Some("Ordinary"),
      securitiesQuantity = Some("10000"),
      amountPaidForSecurities = Some("15000"),
      totalMarketValue = Some("20000"),
      minSharePrice = None,
      maxSharePrice = None,
      sharePurchaseReason = None,
      purchaseForCancellation = None
    )

  private val mockRepository = mock[ParsedStcRowsRepository]
  private val rows: Seq[ParsedStcRow] = Seq(validRow)

  private val someTestDoc: ParsedStcRowsDocument = ParsedStcRowsDocument(_id = "id", rows = rows, createdAt = Instant.now, fileName = "testFile")


  "Check Your Answers Controller" - {

    "must return OK and the correct view for a GET" in {
      val stubNavigator = new StubPersistentNavigator(testNextPage, completeUserAnswers, "stf", "")
      when(mockRepository.findDocumentByReference("testReference")).thenReturn(Future.successful(Some(someTestDoc)))

      val application = applicationBuilder(userAnswers = Some(completeUserAnswers))
        .overrides(
          bind[Navigator].qualifiedWith(Names.named("agents")).toInstance(stubNavigator),
          bind[PersistentNavigator].qualifiedWith(Names.named("agents")).toInstance(stubNavigator),
          bind[ParsedStcRowsRepository].toInstance(mockRepository)
        ).build()

      running(application) {
        val request = FakeRequest(GET, routes.CheckYourAnswersController.onPageLoad().url)
        val result = route(application, request).value

        status(result) mustEqual OK
        contentAsString(result) must include("Check your answers")
        contentAsString(result) must include("File details")
      }
    }

    "must redirect to Journey Recovery for a GET if findDocumentByReference does not find a document" in {
      val stubNavigator = new StubPersistentNavigator(testNextPage, completeUserAnswers, "stf", "")
      when(mockRepository.findDocumentByReference(anyString())).thenReturn(Future.successful(None))

      val application = applicationBuilder(userAnswers = Some(completeUserAnswers))
        .overrides(
          bind[Navigator].qualifiedWith(Names.named("agents")).toInstance(stubNavigator),
          bind[PersistentNavigator].qualifiedWith(Names.named("agents")).toInstance(stubNavigator),
          bind[ParsedStcRowsRepository].toInstance(mockRepository)
        ).build()

      running(application) {
        val request = FakeRequest(GET, routes.CheckYourAnswersController.onPageLoad().url)
        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual uk.gov.hmrc.securitiestransferchargefrontend.controllers.routes.JourneyRecoveryController.onPageLoad().url
      }
    }

    "must redirect to Journey Recovery for a GET if no existing data is found" in {
      val application = applicationBuilder(userAnswers = None).build()

      running(application) {
        val request = FakeRequest(GET, routes.CheckYourAnswersController.onPageLoad().url)
        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual uk.gov.hmrc.securitiestransferchargefrontend.controllers.routes.JourneyRecoveryController.onPageLoad().url
      }
    }

    "must redirect to next page for a POST" in {
      val stubNavigator = new StubPersistentNavigator(testNextPage, completeUserAnswers, "stf", "")

      val application = applicationBuilder(userAnswers = Some(completeUserAnswers))
        .overrides(
          bind[Navigator].qualifiedWith(Names.named("agents")).toInstance(stubNavigator),
          bind[PersistentNavigator].qualifiedWith(Names.named("agents")).toInstance(stubNavigator)
        )
        .build()

      running(application) {
        val request = FakeRequest(POST, routes.CheckYourAnswersController.onSubmit().url)
        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual testNextPage.url
      }
    }

  }
}
