package controllers

import play.api.mvc._
import play.modules.reactivemongo.MongoController
import scala.concurrent.Future

/**
 * Question and Answer document controller.
 */
object QAController extends Controller with MongoController {
  def list = Action { implicit request =>
    Async {
      Future(Ok("Hello"))
    }
  }
}
