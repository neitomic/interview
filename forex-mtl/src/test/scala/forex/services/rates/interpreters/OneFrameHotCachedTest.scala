package forex.services.rates.interpreters

import cats.effect._
import forex.cache.HotCacheRefresher
import forex.config.{CacheConfig, OneFrameConfig}
import org.http4s.client.blaze.BlazeClientBuilder
import org.scalatest.BeforeAndAfterAll
import org.scalatest.matchers.should
import org.scalatest.wordspec.AnyWordSpec
import forex.domain
import forex.domain.{Currency, Rate}

import java.time.OffsetDateTime
import scala.concurrent.ExecutionContext.global
import scala.concurrent.duration._

class OneFrameHotCachedTest extends AnyWordSpec with should.Matchers with BeforeAndAfterAll with OneFrameMocked {
  override val stubs: List[(List[String], String)] = List.empty

  override val defaultStub: Option[String] = Some(
    s"""[{"from":"USD","to":"SGD","bid":0.6118225421857174,"ask":0.8243869101616611,"price":0.71810472617368925,"time_stamp":"${OffsetDateTime.now.toString}"},
      |{"from":"USD","to":"JPY","bid":0.8435259660697864,"ask":0.4175532166907524,"price":0.6305395913802694,"time_stamp":"${OffsetDateTime.now.toString}"}]""".stripMargin
  )

  implicit val cs: ContextShift[IO] = IO.contextShift(global)
  implicit val timer: Timer[IO]     = IO.timer(global)

  var oneFrameLive: OneFrameLive[IO]        = null
  var oneFrameCached: OneFrameHotCached[IO] = null
  var cacheRefresher: HotCacheRefresher[IO] = null
  var refresherFiber: Fiber[IO, Unit]       = null

  override def beforeAll(): Unit = {
    startMockServer()
    oneFrameLive = new OneFrameLive[IO](
      BlazeClientBuilder[IO](global).resource.allocated.unsafeRunSync()._1,
      OneFrameConfig(mockBaseUrl(), mockToken())
    )
    val cacheConfig = CacheConfig(30.seconds, 1.minute)
    oneFrameCached = new OneFrameHotCached(oneFrameLive, cacheConfig)
    cacheRefresher = HotCacheRefresher.of(oneFrameCached, cacheConfig)
    refresherFiber = cacheRefresher
      .scheduler()
      .start
      .flatMap { fiber =>
        IO.sleep(2000.millis).map(_ => fiber)
      }
      .unsafeRunSync()
  }

  override def afterAll(): Unit = {
    refresherFiber.cancel.unsafeRunSync()
    stopMockServer()
  }

  "get" should {
    "return successful" in {
      val result = oneFrameCached.get(domain.Rate.Pair(Currency.USD, Currency.JPY)).unsafeRunSync()
      result.isRight shouldBe true
      val rate: Rate = result.getOrElse(throw new IllegalStateException())
      rate.pair shouldEqual Rate.Pair(Currency.USD, Currency.JPY)
    }

    "error if data is not in cache" in {
      val result = oneFrameCached.get(domain.Rate.Pair(Currency.USD, Currency.CHF)).unsafeRunSync()
      result.isLeft shouldBe true
      val error = result.left.getOrElse(throw new IllegalStateException())
      error.msg shouldEqual "The rate for USDCHF is not found in cache"
    }
  }

  "mget" should {
    "return correctly" in {
      val result = oneFrameCached
        .mget(
          List(
            domain.Rate.Pair(Currency.USD, Currency.JPY),
            domain.Rate.Pair(Currency.USD, Currency.SGD),
          )
        )
        .unsafeRunSync()
      result.isRight shouldBe true
      val rates: List[Rate] = result.getOrElse(throw new IllegalStateException())
      rates.size shouldBe 2
      rates.head.pair shouldEqual Rate.Pair(Currency.USD, Currency.JPY)
      rates(1).pair shouldEqual Rate.Pair(Currency.USD, Currency.SGD)
    }
  }

}
