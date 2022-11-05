package forex.config

import scala.concurrent.duration.FiniteDuration

case class ApplicationConfig(
    http: HttpConfig,
    cacheRefresh: CacheRefreshConfig,
    oneFrame: OneFrameConfig,
)

case class HttpConfig(
    host: String,
    port: Int,
    timeout: FiniteDuration
)

case class CacheRefreshConfig(
    refreshInterval: FiniteDuration
)

case class OneFrameConfig(
    url: String,
    token: String
)
