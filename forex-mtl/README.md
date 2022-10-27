# Impl

## Questions 
- [ ] how many currency pairs we can support?
- [ ] forex api token is the same with upstream token?

## Design consideration
- 10000 request per day per api token
- data freshness: <5m
- upstream limit: 1000 request per day per token
- support all currencies (180 currencies => 64440 pairs)?

=> Caching



### Requirement:
- 10000 per day per token (7 req/min)
- cache time 5 mins

### Limitations:
- 1000 per day per token

