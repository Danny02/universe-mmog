package de.danny02

import java.util.UUID

import cats._
import cats.effect._
import cats.implicits._
import cats.data._
import org.http4s._
import org.http4s.dsl.io._
import org.http4s.headers.{Location, `Content-Type`, `Set-Cookie`}
import org.http4s.implicits.http4sLiteralsSyntax
import org.http4s.server._

import scala.collection.mutable

case class User(id: UUID, name: String)

object Auth {

  private object Template {
    import scalatags.Text.all._
    import scalatags.Text.tags2.title

    val usernameParam = "username"

    val txt =
      "<!DOCTYPE html>" +
        html(
          head(
            title("Example Scala.js application"),
            meta(httpEquiv := "Content-Type", content := "text/html; charset=UTF-8"),
            link(
              rel := "stylesheet",
              `type` := "text/css",
              href := "/deps/bootstrap/4.5.2/css/bootstrap.min.css"
            )
          ),
          body(margin := 0)(
            div(
              cls := "container",
              h1("Not Logged In"),
              form(
                method := "POST",
                div(
                  cls := "form-group",
                  label(
                    "Username",
                    input(cls := "form-control", name := usernameParam)
                  )
                ),
                button(`type` := "submit", cls := "btn btn-primary", "Create")
              )
            )
          )
        )
  }

  private val cookieName = "user"

  object UserRepo {
    private val users                      = mutable.Buffer[User]()
    def create(u: User): IO[Unit]          = IO(users.addOne(u))
    def lookup(id: UUID): IO[Option[User]] = IO(users.find(_.id == id))
  }

  private val authUser: Kleisli[IO, Request[IO], Either[String, User]] = Kleisli({ request =>
    val userId = for {
      header <- headers.Cookie.from(request.headers).toRight("Cookie parsing error")
      cookie <- header.values.toList.find(_.name == cookieName).toRight("Couldn't find the authcookie")
    } yield UUID.fromString(cookie.content)
    userId.traverse(UserRepo.lookup(_)).map(u => u.flatMap(_.toRight("No user found with id")))
  })

  private val onFailure: AuthedRoutes[String, IO] =
    Kleisli(req =>
      OptionT.liftF {
        req.req
          .decode[UrlForm] { form =>
            {
              form
                .getFirst(Template.usernameParam)
                .map { un =>
                  val createUser = User(UUID.randomUUID(), un)
                  val plainUri   = req.req.uri.setQueryParams[String, String](Map())

                  UserRepo.create(createUser) *> Found(
                    Location(plainUri),
                    `Set-Cookie`(ResponseCookie(cookieName, createUser.id.toString, httpOnly = true))
                  )
                }
                .getOrElse(IO.raiseError(new IllegalArgumentException("missing form usernamne")))
            }
          }
          .handleErrorWith(_ => Ok(Template.txt, `Content-Type`(mediaType"text/html;charset=UTF-8")))
      }
    )
  val middleware: AuthMiddleware[IO, User] = AuthMiddleware(authUser, onFailure)
}
