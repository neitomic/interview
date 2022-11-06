package forex.cache

import cats.effect.{ ContextShift, IO, Timer }
import forex.config.CacheRefreshConfig
import org.scalatest.matchers.should
import org.scalatest.wordspec.AnyWordSpec

import java.util.concurrent.atomic.AtomicInteger
import scala.concurrent.ExecutionContext.global
import scala.concurrent.duration._

class HotCacheRefresherTest extends AnyWordSpec with should.Matchers {

  implicit val cs: ContextShift[IO] = IO.contextShift(global)
  implicit val timer: Timer[IO]     = IO.timer(global)

  val hotCached = new HotCached[IO] {
    val counter                          = new AtomicInteger(0)
    override def reloadCache(): IO[Unit] = IO.pure(counter.incrementAndGet()).flatMap(_ => IO.unit)

    def getCounter(): Int = counter.get()
  }
  val refresher = HotCacheRefresher.of(hotCached, CacheRefreshConfig(200.millis))

  "HotCacheRefresher" should {
    "refresh" in {
      refresher.scheduler().start.unsafeRunSync()
      Thread.sleep(1000)
      import scala.language.reflectiveCalls
      hotCached.getCounter() should be >= 5
    }
  }

}
