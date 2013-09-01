package controllers

import play.api.mvc._

object Application extends Controller with CMSController {
  
  def index = withDeadbolt { implicit deadbolt => implicit request =>
    Ok(views.html.index())
  }
  
}