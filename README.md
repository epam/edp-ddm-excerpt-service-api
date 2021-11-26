# Excerpt-service-api

This service provides web api for excerpts processing (creation, retrieving, status check).

### Related components:
* `excerpt-worker` - service, which generate excerpt files
* Kafka for pushing messages to `excerpt-worker`
* Ceph storage for processing signatures and retrieving result files 
* PostgreSQL database for excerpt info persistence (processing statuses etc.)

### Local development:
###### Prerequisites:
* Kafka is configured and running
* Ceph/S3-like storage is configured and running
* Database `excerpt` is configured and running

###### Database setup:
1. Create database `excerpt`
1. Run `/platform-db/changesets/excerpt/` script(s) from the `citus` repository

###### Configuration:
1. Check `src/main/resources/application-local.yaml` and replace if needed:
   * data-platform.datasource... properties with actual values from local db
   * data-platform.kafka.boostrap with url of local kafka
   * *-ceph properties with your ceph storage values
   * dso.url if current url is unavailable

###### Steps:
1. (Optional) Package application into jar file with `mvn clean package`
2. Add `--spring.profiles.active=local` to application run arguments
3. Run application with your favourite IDE or via `java -jar ...` with jar file, created above

Application starts by default on port 7001, to get familiar with available endpoints - visit swagger (`localhost:7001/openapi`).

###### Additional information
The `excerpt_template` table might be filled in via `report-publisher`.

### License
excerpt-service-api is Open Source software released under the Apache 2.0 license.