package de.danny02

import autowire.Core
import cats.effect.{ContextShift, IO}
import org.http4s.dsl.io.{->, /:, Ok, POST}
import org.http4s.headers.`Content-Type`
import org.http4s.implicits.http4sLiteralsSyntax
import org.http4s.{EntityDecoder, EntityEncoder, HttpRoutes}
import upickle.default._
import org.http4s.dsl.io._

object AutowireServer extends autowire.Server[ujson.Value, Reader, Writer] {

  def callApi(path: Seq[String], data: Map[String, ujson.Value], routes: Core.Router[ujson.Value])(implicit
      contextShift: ContextShift[IO]
  ): IO[ujson.Value] = {
    IO.fromFuture(IO(routes(autowire.Core.Request(path, data))))
  }

  def read[Result: Reader](p: ujson.Value) = upickle.default.read[Result](p)
  def write[Result: Writer](r: Result)     = upickle.default.writeJs(r)

  implicit val jsonDecoder =
    EntityDecoder.text[IO].map(upickle.default.read[Map[String, ujson.Value]](_, false))

  implicit val jsonEncoder =
    EntityEncoder.stringEncoder[IO].contramap[ujson.Value](upickle.default.write(_))

  def api(router: Core.Router[ujson.Value]*)(implicit contextShift: ContextShift[IO]) = {
    val allRoutes = router.reduce(_ orElse _)
    HttpRoutes
      .of[IO] {
        case req @ POST -> path => {
          println(path)
          for {
            data   <- req.as[Map[String, ujson.Value]]
            result <- callApi(path.toList, data, allRoutes)
            resp   <- Ok(result, `Content-Type`(mediaType"application/json"))
          } yield resp
        }
      }
  }
}
