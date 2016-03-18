import akka.actor.{Actor, ActorSystem, Props}
import akka.pattern.ask
import akka.util.Timeout

import scala.collection.mutable.{ListBuffer, ArrayBuffer}
import scala.concurrent.Await
import scala.concurrent.duration._


class Monitor extends Actor {
  def receive = {
    case "slotRequired" => {
      val slot = ParkingLot.parkingSlots.indexOf(true)
      if (slot >= 0) {
        ParkingLot.parkingSlots.update(slot, false)
      }
      sender ! slot
    }
    case "leavingSlot" => {
      val leavingSlot = ParkingLot.freeSlots(0)
      ParkingLot.parkingSlots.update(leavingSlot, true)
      sender ! leavingSlot

    }
  }

}

class Attendant extends Actor {
  implicit val timeout = Timeout(2.seconds)

  def receive = {
    case "Parking" => {
      val parkingSlot = Await.result((ParkingLot.Monitor ? "slotRequired").mapTo[Int], 2.seconds)
      println("Parking is done at " + parkingSlot + " slot")
    }
    case "leaving" => {
      val emptyparkingSlot = Await.result((ParkingLot.Monitor ? "leavingSlot").mapTo[Int], 2.seconds)
      println("car Departed from " + emptyparkingSlot + " slot")
    }
  }
}

object ParkingLot {
  val parkingSlots = ArrayBuffer.fill(20)(true)
  val system = ActorSystem("parkingLotSystem")
  val Monitor = system.actorOf(Props[Monitor], "Monitor")
  val attendant = system.actorOf(Props[Attendant], "Attendant")
  val freeSlots: ListBuffer[Int] = ListBuffer()

  def freeSlotsHandler(slotNoOfCarLeaving: Int) = {
    freeSlots += slotNoOfCarLeaving
  }

}

object Parking extends App {

  ParkingLot.attendant ! "Parking"
  ParkingLot.attendant ! "Parking"
  ParkingLot.freeSlotsHandler(1)
  ParkingLot.attendant ! "leaving"
}
