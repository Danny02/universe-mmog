package de.danny02

import scala.scalajs.js.annotation.{JSExport, JSExportTopLevel}
import org.scalajs.dom
import scalatags.JsDom.all._

import upickle.default._
import autowire._

import scala.concurrent.Future
import scalajs.concurrent.JSExecutionContext.Implicits.queue

object Client extends autowire.Client[ujson.Value, Reader, Writer] {
  override def doCall(req: Request): Future[ujson.Value] = {
    dom.ext.Ajax
      .post(
        url = "/api/" + req.path.mkString("/"),
        data = upickle.default.write(ujson.Obj.from(req.args.toSeq))
      )
      .map(_.responseText)
      .map(upickle.default.read[ujson.Value](_, false))
  }

  override def read[Result: Reader](p: ujson.Value) = upickle.default.read[Result](p)
  override def write[Result: Writer](r: Result)     = writeJs(r)
}

@JSExportTopLevel("Main")
object MainJs {

  @JSExport
  def run(): Unit = {
    EventExampleCalc.doExperiment()

    val inputBox  = input.render
    val outputBox = div.render

    def updateOutput() = {
      Client[HelloApi].hello(inputBox.value).call().foreach { text =>
        outputBox.innerHTML = text
      }
    }
    inputBox.onkeyup = { (e: dom.Event) =>
      updateOutput()
    }
    updateOutput()

    val aBox      = input(`type` := "number").render
    val bBox      = input(`type` := "number").render
    val resultBox = div.render

    def updateResult() = {
      Client[CalcApi]
        .add(aBox.value.toIntOption.getOrElse(0), bBox.value.toIntOption.getOrElse(0))
        .call()
        .foreach { res =>
          resultBox.innerHTML = s"The sum of both numbers is $res"
        }
    }
    aBox.onchange = { (e: dom.Event) =>
      updateResult()
    }
    bBox.onchange = { (e: dom.Event) =>
      updateResult()
    }
    updateResult()

    dom.document.body.appendChild(
      div(
        div(
          cls := "container",
          h1("Greeter"),
          p("Enter your Name"),
          inputBox,
          outputBox
        ),
        div(
          cls := "container",
          h1("Calculator"),
          p("Enter a Number"),
          aBox,
          p("Enter another Number"),
          bBox,
          resultBox
        )
      ).render
    )
  }
}
