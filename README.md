# Excerpt-service-api

This service provides web api for excerpts processing (creation, retrieving, status check).
### Related components:
* `excerpt-worker` - service, which generate excerpt files
* Kafka for pushing messages to excerpt-worker
* Ceph storage for processing signatures and retrieving excerpt-worker result files 
* PostgreSQL database for excerpt info persistence (processing statuses etc.)

### Local deployment:
###### Prerequisites:

* Kafka is configured and running
* Ceph storage is configured and running (either local or remote)
* Database `excerpt` is configured and running

###### Excerpt database setup:
1. Create database `excerpt`
1. Run `initial-db-setup` script

###### Steps:
1. Check `src/main/resources/application-local.yaml` and replace if needed:
    * data-platform.datasource... properties with actual values from local db
    * data-platform.kafka.boostrap with url of local kafka
    * *-ceph properties with your ceph storage values
    * dso.url if current url is unavailable
2. (Optional) Package application into jar file with `mvn clean package`
3. Add `--spring.profiles.active=local` to application run arguments
4. Run application with your favourite IDE or via `java -jar ...` with jar file, created on step 2

Application starts by default on port 7001, to get familiar with available endpoints - visit swagger (`localhost:7001/openapi`)

###### Additional information
To create an excerpt, at least one record in `excerpt_template` table is required. It might be created manually or via `report-publisher`.
