package utils

import java.util.concurrent.ThreadLocalRandom

import scala.concurrent.duration.DurationInt
import scala.concurrent.{Await, Future}
import scala.language.postfixOps

object Utils {

  def getRandom(min: Double, max: Double): Double = ThreadLocalRandom.current().nextDouble(min, max)

  object Implicits {

    implicit class EnhancedFuture[T](future: Future[T]){
      def awaitForTenSeconds: T = Await.result(future, 10000 millis)
    }

  }

}