# bw-database [![Build Status](https://travis-ci.org/Bedework/bw-util2.svg)](https://travis-ci.org/Bedework/bw-util2)
Database access classes for
[Bedework](https://www.apereo.org/projects/bedework).

## Requirements

1. JDK 17
2. Maven 3

## Building Locally

> mvn clean install

## Releasing

Releases of this fork are published to Maven Central via Sonatype.

To create a release, you must have:

1. Permissions to publish to the `org.bedework` groupId.
2. `gpg` installed with a published key (release artifacts are signed).

To perform a new release:

> mvn -P bedework-dev release:clean release:prepare

When prompted, select the desired version; accept the defaults for scm tag and next development version.
When the build completes, and the changes are committed and pushed successfully, execute:

> mvn -P bedework-dev release:perform

For full details, see [Sonatype's documentation for using Maven to publish releases](http://central.sonatype.org/pages/apache-maven.html).

## Release Notes
### 1.0.0
* Largely moving util-hibernate into the new bw-database module - including refactor of the wildfly module deployment.
* Remove all the blob refs from the source. Mapping now handles byte[] to blob
* Remove all references to cacheable query. It's hibernate specific and other ways need to be found to achieve that.
* Move non db specific files into database/db
  Remove Hibsession.restore() - not used and not JPA
* Align the implementations. Add refresh(...)
* Fix various pom issues after refactor
* Align the various implementations and interfaces.
* Mostly remove saveOrUpdate - a hibernate only feature - and replace with jpa compliant calls to add or update.
* Fix up some non-jpa stuff.
  Renamed VersionedDbEntity as InterceptorDbEntity and add some methods
* Switch to use DbSession from bw-database.
* Hibernate implementation largely uses the bw-database version with a couple of overrides.
