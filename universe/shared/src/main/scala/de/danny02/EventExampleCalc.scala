package de.danny02

import scala.annotation.tailrec

object EventExampleCalc {

  sealed trait Event
  object BOUGHT   extends Event
  object UPGRADED extends Event

  type Seconds = Int

  case class BuildingInfo(constuctionTime: Seconds, resPerSecond: Int)

  type LogEnty = (Seconds, Event)
  type Log     = List[LogEnty]

  def resourcesAt(time: Seconds, fullLog: Log, building: BuildingInfo): Int = {
    val measuredLog = fullLog.takeWhile(_._1 <= time)

    case class State(
        build: Option[Int] = None,
        upgradeLevel: Int = 0,
        resources: Int = 0,
        lastEventTime: Seconds = 0
    )

    def resUntil(time: Seconds, state: State): Int = {
      if (state.build.exists(_ < time)) {
        if (state.lastEventTime < time) {
          val elapsed = time - state.lastEventTime
          val rate    = building.resPerSecond + state.upgradeLevel
          state.resources + elapsed * rate
        } else {
          state.resources
        }
      } else {
        0
      }
    }

    @tailrec
    def buildState(log: Log, state: State = State()): State = log match {
      case (t, BOUGHT) :: tail => {
        val finished = building.constuctionTime + t
        buildState(tail, State(build = Option(finished), lastEventTime = finished))
      }
      case (t, UPGRADED) :: tail if state.build.exists(_ < t) => {
        buildState(
          tail,
          state.copy(
            upgradeLevel = state.upgradeLevel + 1,
            resources = resUntil(t, state),
            lastEventTime = t
          )
        )
      }
      case _ :: tail => {
        buildState(tail, state)
      }
      case Nil => state
    }

    val s = buildState(measuredLog)
    resUntil(time, s)
  }

  def doExperiment() = {
    val building = BuildingInfo(30, 2)
    val log = List(
      5  -> BOUGHT,
      45 -> UPGRADED
    )

    def printAt(t: Seconds) = {
      val r = resourcesAt(t, log, building)
      println(s"After $t s the building has produced $r resources")
    }
    List(4, 5, 40, 50).foreach(printAt)
  }
}
