package forex.cache

import cats.Applicative
import forex.config.CacheRefreshConfig
import cats.effect._
import cats.syntax.apply._
import cats.syntax.flatMap._

class HotCacheRefresher[F[_]: Applicative: Timer: Sync](
    hotCached: HotCached[F],
    refreshConfig: CacheRefreshConfig
) {

  private def reloadCacheTimer(): F[Unit] =
    Timer[F].sleep(refreshConfig.refreshInterval) *> hotCached.reloadCache()

  private def repeat(): F[Unit] =
    reloadCacheTimer() >> repeat()

  def scheduler(): F[Unit] =
    hotCached.reloadCache() >> repeat()
}

object HotCacheRefresher {
  def of[F[_]: Applicative: Timer: Sync](
      hotCached: HotCached[F],
      cacheRefreshConfig: CacheRefreshConfig
  ): HotCacheRefresher[F] =
    new HotCacheRefresher(hotCached, cacheRefreshConfig)
}
