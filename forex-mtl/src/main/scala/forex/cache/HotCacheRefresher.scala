package forex.cache

import forex.config.CacheConfig
import cats.effect._
import cats.syntax.apply._
import cats.syntax.flatMap._

class HotCacheRefresher[F[_]: Timer: Sync](
    hotCached: HotCached[F],
    cacheConfig: CacheConfig
) {

  private def reloadCacheTimer(): F[Unit] =
    Timer[F].sleep(cacheConfig.refreshInterval) *> hotCached.reloadCache()

  private def repeat(): F[Unit] =
    reloadCacheTimer() >> repeat()

  def scheduler(): F[Unit] =
    hotCached.reloadCache() >> repeat()
}

object HotCacheRefresher {
  def of[F[_]: Timer: Sync](
      hotCached: HotCached[F],
      cacheConfig: CacheConfig
  ): HotCacheRefresher[F] =
    new HotCacheRefresher(hotCached, cacheConfig)
}
