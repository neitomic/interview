package forex.services.rates.interpreters

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder
import com.github.tomakehurst.wiremock.core.WireMockConfiguration.options
import com.github.tomakehurst.wiremock.matching.{ EqualToPattern, RequestPatternBuilder }
import com.github.tomakehurst.wiremock.stubbing.StubMapping

import java.util.UUID

trait OneFrameMocked {
  private val wiremockSever: WireMockServer = new WireMockServer(options().dynamicPort())
  private val token                         = UUID.randomUUID().toString

  def stubs: List[(List[String], String)]
  def defaultStub: Option[String] = None

  def addStub(
      pairs: List[String],
      response: String
  ): Unit =
    wiremockSever.addStubMapping(
      new StubMapping(
        RequestPatternBuilder
          .newRequestPattern()
          .withUrl(s"/rates?${pairs.map(pair => s"pair=$pair").mkString("&")}")
          .withHeader("token", new EqualToPattern(token))
          .build(),
        ResponseDefinitionBuilder
          .responseDefinition()
          .withBody(response)
          .withStatus(200)
          .withHeader("Content-Type", "application/json")
          .build()
      )
    )

  def mockBaseUrl(): String =
    wiremockSever.baseUrl()

  def mockToken(): String = token

  def startMockServer(): Unit = {
    for ((pair, resp) <- stubs) {
      addStub(pair, resp)
    }
    defaultStub.foreach { defaultResp =>
      wiremockSever.addStubMapping(
        new StubMapping(
          RequestPatternBuilder
            .newRequestPattern()
            .withHeader("token", new EqualToPattern(token))
            .build(),
          ResponseDefinitionBuilder
            .responseDefinition()
            .withBody(defaultResp)
            .withStatus(200)
            .withHeader("Content-Type", "application/json")
            .build()
        )
      )
    }
    wiremockSever.start()
  }
  def stopMockServer(): Unit = wiremockSever.stop()
}
