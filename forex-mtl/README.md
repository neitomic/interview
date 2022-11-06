# Forex proxy

## Requirement:
- OneFrame only can support 1000 request per day
- The proxy need to support 10000 request per day
- Data freshness < 5 minutes

## Additional though

- How many currency pairs we can support? Total 180 currencies in the world.
- We serve the rate for a single pair per request, but the one-frame support multiple pairs per request.
- If the load is fairly distributed within a day, then we need to support ~35 requests per 5 minutes. But we can only call the one-frame ~3.4 request per 5 minutes.


## Design consideration

From the though, we need caching mechanism to cache the rate data to serve more downstream requests with less upstream request.

### On-demand cache reload

1. Check the cache if rate existed
2. If rate existed, return
3. If not existed, call one-frame upstream to fetch the rate
4. Store rate into cache with TTL 5 minutes
5. Return the rate

**Though**
- This is a straightforward interpreter implementation, directly propagate upstream error into downstream
- Spike response time when cache-miss
- Normally the load will mostly come on the office-hours ( => high cache-hit), but we still need to maintain the uptime of the proxy => let's assume the load is fairly distributed. So from the calculation above, we can only call the one-frame upstream 3.4 requests per 5 minutes => we can only support 3 currency pair?
- We can increase the number of pairs when call one-frame? (e.g. fetch all currency pairs) => increase refresh time
  - => spike response time.
  - => concurrency issue (multiple clients fetch related rates at the same time.)

### Hot reloaded cache (chosen one)

1. A background process keep calling the one-frame upstream to fetch all rates and store in the cache every 5 minutes
2. When receive request for a rate, get rate from the cache only.
3. Verify if the rate is still valid,

**Though**
- No spike in response time
- Can serve downstream as much as possible, without depends on the upstream limitation
- Complicated implementation with background worker to refresh the rates.
- Need to carefully monitor the worker to make sure it's working.
- Long start-up time


## Implementation

- interpreters
  - OneFrameLive: OneFrame interpreter without caching
  - OneFrameHotCached: OneFrame interpreter with in memory hot-cached, use other interpreter as underlying data source.
  - HotCacheRefresher: a background worker for hot-cache refresh.
- behaviors
  - HotCacheRefresher run every 4 minutes (smaller than data freshness requirement)
  - If reload failed, re-run after 4 minutes (no retry yet.) => should add exponential retry with backoff here.
  - OneFrameHotCached only read rates from cache. If rates staled, return internal server error.


## What else?
- Scaling: we have in-memory cache, which will increase calls to upstream if we scale out to multiple instance. => Solution: use external storage for cache (Redis, Memcached,...), split the cache refresher into another service, use external cache storage to serve downstream.
- Long start-up time: need to verify the service is completely started (with cache reloaded) before start handling requests. => Healthcheck endpoint.
- Monitoring: monitor closely the cache refresh worker.