package uk.gov.hmrc.securitiestransferchargefrontend.controllers.sh03.organisations.single

import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import uk.gov.hmrc.securitiestransferchargefrontend.controllers.actions.*
import uk.gov.hmrc.securitiestransferchargefrontend.controllers.stf.shared.SaveAndReturnButton.isReturn
import uk.gov.hmrc.securitiestransferchargefrontend.forms.sh03.organisations.single.WhatReliefAreYouApplyingForViewFormProvider
import uk.gov.hmrc.securitiestransferchargefrontend.models.Mode
import uk.gov.hmrc.securitiestransferchargefrontend.navigation.Navigator
import uk.gov.hmrc.securitiestransferchargefrontend.pages.sh03.WhatReliefAreYouApplyingForViewPage
import uk.gov.hmrc.securitiestransferchargefrontend.pages.stf.single.WhatReliefAreYouApplyingForPage
import uk.gov.hmrc.securitiestransferchargefrontend.repositories.SessionRepository
import uk.gov.hmrc.securitiestransferchargefrontend.views.html.sh03.organisations.single.WhatReliefAreYouApplyingForViewView

import javax.inject.{Inject, Named}
import scala.concurrent.{ExecutionContext, Future}

class WhatReliefAreYouApplyingForViewController @Inject()(
                                        override val messagesApi: MessagesApi,
                                        sessionRepository: SessionRepository,
                                        @Named("orgSh03") navigator: Navigator,
                                        identify: IdentifierAction,
                                        stcAuthEnrolled: StcAuthEnrolledAction,
                                        getData: StcDataRetrievalAction,
                                        requireData: StcDataRequiredAction,
                                        formProvider: WhatReliefAreYouApplyingForViewFormProvider,
                                        val controllerComponents: MessagesControllerComponents,
                                        view: WhatReliefAreYouApplyingForViewView
                                    )(implicit ec: ExecutionContext) extends FrontendBaseController with I18nSupport {

  val form = formProvider()

  def onPageLoad(mode: Mode): Action[AnyContent] = (stcAuthEnrolled andThen getData andThen requireData) {
    implicit request =>

      val preparedForm = request.userAnswers.get(WhatReliefAreYouApplyingForViewPage) match {
        case None => form
        case Some(value) => form.fill(value)
      }

      Ok(view(preparedForm, mode))
  }

  def onSubmit(mode: Mode): Action[AnyContent] = (stcAuthEnrolled andThen getData andThen requireData).async {
    implicit request =>

      form.bindFromRequest().fold(
        formWithErrors =>
          Future.successful(BadRequest(view(formWithErrors, mode))),

        value =>
          for {
            updatedAnswers <- Future.fromTry(request.userAnswers.set(WhatReliefAreYouApplyingForViewPage, value))
            nextPage       <- navigator.nextPage(WhatReliefAreYouApplyingForPage, mode, updatedAnswers, isReturn(request))
          } yield Redirect(nextPage)
      )
  }
}
