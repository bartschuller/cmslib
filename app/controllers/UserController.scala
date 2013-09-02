package controllers

import play.api.mvc._
import models.User
import org.mindrot.jbcrypt.BCrypt
import reactivemongo.api.collections.default.BSONCollection
import play.api.Logger
import reactivemongo.bson.{BSONDocument, BSONObjectID}
import play.api.data._
import play.api.data.Forms._
import scala.concurrent.Future
import play.api.libs.Crypto

object UserController extends Controller with CMSController {
  val collection = db[BSONCollection]("users")

  def createForm = withDeadbolt { implicit deadbolt => implicit request =>
    Ok(views.html.user.edit(None, User.form))
  }

  def signup = withDeadbolt { implicit deadbolt => implicit request =>
    User.form.bindFromRequest.fold(
      formWithErrors => Ok(views.html.user.edit(None, formWithErrors)),
      userNoCrypt => {
        val user = userNoCrypt.copy(password = BCrypt.hashpw(userNoCrypt.password, BCrypt.gensalt))
        AsyncResult {
          collection.insert(user).map(lastError =>
            if (lastError.ok)
              Redirect(routes.Application.index).flashing("success" -> "User successfully created.")
            else {
              Logger.error(s"MongoDB error saving User ${user.name}: ${lastError.errMsg.getOrElse("unknown")}")
              Redirect(routes.Application.index).flashing("warning" -> "Error saving User.")
          })
        }
      }
    )
  }

  val loginForm = Form(
    tuple(
      "userName" -> nonEmptyText,
      "password" -> nonEmptyText(minLength=12),
      "rememberMe" -> boolean
    )
  )

  val emptyUser = User(None, "", BCrypt.gensalt, "", Seq())

  def login = withDeadbolt { implicit deadbolt => implicit request =>
    Async {
      loginForm.bindFromRequest.fold(
        formWithErrors => Future(Redirect(routes.Application.index).flashing("warning" -> "Login failed.")),
        { case (userName, password, rememberMe) =>
            collection.find(BSONDocument("name" -> userName)).one[User].map { ou =>
              val user = ou.getOrElse(emptyUser)
              if (BCrypt.checkpw(password, user.password)) {
                val userId = user.id.get
                val res = Redirect(routes.Application.index).flashing("success" -> "Login successful.").withSession(
                  "userId" -> userId
                )
                if (rememberMe) {
                  val rememberData = Crypto.sign(userId) + "-" + userId
                  res.withCookies(Cookie("rememberMe", rememberData))
                } else
                  res.discardingCookies(DiscardingCookie("rememberMe"))
              } else {
                Redirect(routes.Application.index).flashing("warning" -> "Login failed.")
              }
            }
        }
      )
    }
  }

  def logout = withDeadbolt { implicit deadbolt => implicit request =>
    Redirect(routes.Application.index).discardingCookies(DiscardingCookie("rememberMe")).withNewSession.flashing("success" -> "Logout succesful.")
  }

  def editForm(id: String) = dynamicRestrictions("ownProfile", id) { implicit deadbolt => implicit request =>
    Async {
      collection.find(BSONDocument("_id" -> new BSONObjectID(id))).one[User].map( ou =>
        ou.fold[Result](NotFound)(user => Ok(views.html.user.edit(Some(id), User.form.fill(user))))
      )
    }
  }

  def edit(id: String) = dynamicRestrictions("ownProfile", id) { implicit deadbolt => implicit request =>
    User.form.bindFromRequest.fold(
      formWithErrors => Ok(views.html.user.edit(Some(id), formWithErrors)),
      userNoCrypt => {
        val user = userNoCrypt.copy(id = Some(id), password = BCrypt.hashpw(userNoCrypt.password, BCrypt.gensalt))
        AsyncResult {
          collection.update(BSONDocument("_id" -> new BSONObjectID(id)), user).map(lastError =>
            if (lastError.ok)
              Redirect(routes.Application.index).flashing("success" -> "User successfully updated.")
            else {
              Logger.error(s"MongoDB error saving User ${user.name}: ${lastError.errMsg.getOrElse("unknown")}")
              Redirect(routes.Application.index).flashing("warning" -> "Error saving User.")
            })
        }
      }
    )
  }
}
