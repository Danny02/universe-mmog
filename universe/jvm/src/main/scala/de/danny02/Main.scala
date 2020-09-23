package de.danny02

import cats.SemigroupK.ops.toAllSemigroupKOps
import cats.effect._
import org.http4s.HttpRoutes
import org.http4s.dsl.io._
import org.http4s.headers._
import org.http4s.implicits._
import org.http4s.server.Router
import org.http4s.server.blaze._
import org.http4s.server.middleware.GZip
import org.http4s.server.staticcontent.{ResourceService, WebjarService, resourceService, webjarService}

import scala.concurrent.ExecutionContext.global

object Template {
  import scalatags.Text.all._
  import scalatags.Text.tags2.title

  val txt =
    "<!DOCTYPE html>" +
      html(
        head(
          title("Example Scala.js application"),
          meta(httpEquiv := "Content-Type", content := "text/html; charset=UTF-8"),
          script(`type` := "text/javascript", src := "/assets/universe-client-fastopt.js"),
          link(
            rel := "stylesheet",
            `type` := "text/css",
            href := "/deps/bootstrap/4.5.2/css/bootstrap.min.css"
          )
        ),
        body(margin := 0)(
          script("Main.run()")
        )
      )
}

object HelloImpl extends HelloApi {
  override def hello(name: String): String = s"Hello, $name."
}

object CalcImpl extends CalcApi {
  override def add(a: Int, b: Int): Int = a + b
}

object Main extends IOApp {

  def index = HttpRoutes
    .of[IO] { case GET -> Root =>
      Ok(Template.txt, `Content-Type`(mediaType"text/html;charset=UTF-8"))
    }

  def apiService = {
    implicit val ec: scala.concurrent.ExecutionContext = scala.concurrent.ExecutionContext.global
    import AutowireServer._

    Router(
      "api" -> api(
        route[HelloApi](HelloImpl),
        route[CalcApi](CalcImpl)
      )
    )
  }

  def staticFiles(blocker: Blocker) = {
    val router = Router(
      "assets" -> resourceService[IO](ResourceService.Config("/", blocker)),
      "deps"   -> webjarService[IO](WebjarService.Config(blocker))
    )

    GZip(router)
  }

  def all(blocker: Blocker) = (index <+> apiService <+> Main.staticFiles(blocker)).orNotFound

  def run(args: List[String]): IO[ExitCode] = {
    val port = sys.env.get("PORT").flatMap(_.toIntOption).getOrElse(8080)
    val app = for {
      blocker <- Blocker[IO]
      server <- BlazeServerBuilder[IO](global)
        .bindHttp(port, "0.0.0.0")
        .withHttpApp(all(blocker))
        .resource
    } yield server

    app.use(_ => IO.never).as(ExitCode.Success)
  }
}
