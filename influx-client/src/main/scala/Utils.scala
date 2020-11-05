import java.util.concurrent.ThreadLocalRandom

object Utils {

  def getRandom(min: Double, max: Double): Double = ThreadLocalRandom.current().nextDouble(min, max)

}