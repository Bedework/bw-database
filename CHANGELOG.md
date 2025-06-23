# Release Notes

This project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## Unreleased (6.0.0-SNAPSHOT)
* First jakarta release

## [5.0.5] - 2025-02-06
* First release.
* Mostly a refactoring from the hibernate code in util-hibernate. Generalised and made jpa compliant.
* Largely moving util-hibernate into the new bw-database module - including refactor of the wildfly module deployment.
* Remove all the blob refs from the source. Mapping now handles byte[] to blob
* Remove all references to cacheable query. It is hibernate specific, and other ways need to be found to achieve that.
* Move non db specific files into database/db
* Remove Hibsession.restore() - not used and not JPA
* Align the implementations. Add refresh(...)
* Fix various pom issues after refactor
* Align the various implementations and interfaces.
* Mostly remove saveOrUpdate - a hibernate only feature - and replace with jpa compliant calls to add or update.
* Fix up some non-jpa stuff.
  Renamed VersionedDbEntity as InterceptorDbEntity and add some methods
* Switch to use DbSession from bw-database.
* Hibernate implementation largely uses the bw-database version with a couple of overrides.
