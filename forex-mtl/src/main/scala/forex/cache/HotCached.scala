package forex.cache

trait HotCached[F[_]] {
  def reloadCache(): F[Unit]
}
