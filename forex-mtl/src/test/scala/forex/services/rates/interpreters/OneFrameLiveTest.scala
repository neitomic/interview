package forex.services.rates.interpreters

import cats.effect.{ IO, _ }
import forex.config.OneFrameConfig
import forex.domain
import forex.domain.Currency
import org.http4s.client.blaze.BlazeClientBuilder
import org.scalatest.BeforeAndAfterAll
import org.scalatest.matchers.should
import org.scalatest.wordspec.AnyWordSpec

import scala.concurrent.ExecutionContext.global

class OneFrameLiveTest extends AnyWordSpec with should.Matchers with BeforeAndAfterAll with OneFrameMocked {

  override val stubs: List[(List[String], String)] = List(
    (
      List("USDSGD"),
      """[{"from":"USD","to":"SGD","bid":0.8,"ask":0.3,"price":0.5,"time_stamp":"2022-11-05T04:15:46.143Z"}]"""
    ),
    (
      List("USDCAD"),
      """{"error":"bad request"}"""
    ),
    (
      List("USDUSD"),
      """[]"""
    ),
    (
      List("USDSGD", "USDJPY"),
      """[{"from":"USD","to":"SGD","bid":0.6118225421857174,"ask":0.8243869101616611,"price":0.71810472617368925,"time_stamp":"2022-11-05T07:29:42.763Z"},
        |{"from":"USD","to":"JPY","bid":0.8435259660697864,"ask":0.4175532166907524,"price":0.6305395913802694,"time_stamp":"2022-11-05T07:29:42.763Z"}]""".stripMargin
    ),
  )

  implicit val cs: ContextShift[IO] = IO.contextShift(global)
  implicit val timer: Timer[IO]     = IO.timer(global)

  override def beforeAll() = startMockServer()

  override def afterAll(): Unit = stopMockServer()

  trait TestContext {
    val oneFrameLive = new OneFrameLive[IO](
      BlazeClientBuilder[IO](global).resource.allocated.unsafeRunSync()._1,
      OneFrameConfig(mockBaseUrl(), mockToken())
    )
  }

  "get" should {
    "return correctly" in new TestContext {
      val pair = domain.Rate.Pair(Currency.USD, Currency.SGD)
      val result = oneFrameLive
        .get(pair)
        .unsafeRunSync()

      result.isRight shouldBe true
      val rate = result.getOrElse(throw new IllegalStateException())
      rate.pair shouldEqual pair
    }

    "propagate error" in new TestContext {
      val pair = domain.Rate.Pair(Currency.USD, Currency.CAD)
      val result = oneFrameLive
        .get(pair)
        .unsafeRunSync()

      result.isLeft shouldBe true
      val rate = result.left.getOrElse(throw new IllegalStateException())
      rate.msg shouldEqual "bad request"
    }

    "return error when from/to is the same currency" in new TestContext {
      val pair = domain.Rate.Pair(Currency.USD, Currency.USD)
      val result = oneFrameLive
        .get(pair)
        .unsafeRunSync()

      result.isLeft shouldBe true
      val rate = result.left.getOrElse(throw new IllegalStateException())
      rate.msg shouldEqual "OneFrame lookup returns 0 items. Expecting 1."
    }
  }

  "mget" should {
    "return correctly" in new TestContext {
      val pairs = List(domain.Rate.Pair(Currency.USD, Currency.SGD), domain.Rate.Pair(Currency.USD, Currency.JPY))
      val result = oneFrameLive
        .mget(pairs)
        .unsafeRunSync()

      result.isRight shouldBe true
      val rates = result.getOrElse(throw new IllegalStateException())
      rates.size shouldBe 2
      rates.head.pair shouldEqual domain.Rate.Pair(Currency.USD, Currency.SGD)
      rates(1).pair shouldEqual domain.Rate.Pair(Currency.USD, Currency.JPY)
    }
  }
}
