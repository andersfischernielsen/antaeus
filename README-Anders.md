# Overview

I went for a relatively simple implementation to avoid overcomplicating the solution. 

I decided to implement extensions to the REST API for handling billing, add a table/models to the data layers for keeping track of billing and handling the monthly billing via a cronjob running once a month. 

The BillingService was implemented with a simple `billAll` function in `BillingService`, taking the current Invoices as input and charging customers if certain criteria are met. 

These criteria are: 
- An Invoice is `PENDING` and a previous Payment (the newly added table to the DB) is either 
    - Not present, meaning that the customer has never been charged
    - At least a month old if the Invoice is `PENDING` for the same customer, meaning that the customer was charged last month, but is now being charged again according to the new Invoice

I drafted a simple YAML specification for a Kubernetes CronJob. This specification has not been tested, but *should* run once a month and execute two simple `curl` commands - one for `GET`ing pending Invoices and one for `POST`ing the pending Invoices to the `BillingService` (to keep everything RESTful), receiving the charged customers as JSON for potential logging in the CronJob side.

The changes I made to the project are: 
- Resolved an issue with recent Gradle version in `utils.kt` (a flag has been deprecated for runtime dependencies it seems)
- Added REST endpoints in `pleo-antaeus-rest`
- Added DB tables and models to `pleo-antaeus-models`
- Added models/mappings and functions for retrieving and creating data to `pleo-antaeus-data`
- Implemented the `BillingService` in `pleo-antaeus-core`
- Added a sample Kubernetes CronJob in `simple-kubernetes-cronjob-example.yaml`

The resulting solution can be run in Docker with 
```
docker run -p 7000:7000 antaeus
```
after building the image. Data can be retrieved and sent to/from the solution with
```
curl -s localhost:7000/rest/v1/payments/pending | curl -s -H "Content-Type: application/json" -X POST --data-binary @- localhost:7000/rest/v1/payments
```

I also kept a mini-journal for timekeeping and keeping track of my thought process, as seen below. 

# Mini-journal
## Day 1 (2 hours spent)
- Forked and cloned the repository locally in the afternoon
- Made sure everything was building properly and resolved a Gradle issue (JUnit) in `utils.kt`
    - This allowed the project to build in recent Gradle versions (Gradle was yelling at me on initial build, and I decided to fix the issue for futureproofing)
    - See: ["Dependencies should no longer be declared using the compile and runtime configurations"](https://docs.gradle.org/6.2/userguide/upgrading_version_5.html#dependencies_should_no_longer_be_declared_using_the_compile_and_runtime_configurations)
- Got an overview of the source, starting at `BillingService.kt` in `pleo-antaeus-core`
    - Worked my way around the solution, following the execution flow
    - Inspected the database schema and got an overview of the data types and database layer `AnaeusDal.kt`
    - Verified that all endpoints defined in `AntaeusRest.kt` were reachable on `localhost` through Docker 
- Added a REST endpoint for fetching pending invoices for future use at `invoices/pending`
- Debated pros/cons for setting up a monthly cronjob in Docker
    - In order to keep everything RESTful, I landed on the following: 
        - A cronjob (potentially hosted in a service elsewhere or instead as a Kubernetes CronJob) pings `/invoices/pending` on a monthly basis
        - These pending invoices are then POSTed to `/invoices` in order to 
            1. Call the `PaymentProvider`
            2. Update these invoices in the DB depending on the billing result (success/fail)
            3. Store failed/rejected invoices in a separate table in the DB for housekeeping/notification/e-mail purposes
        - The resulting state of all invoices is then sent back to the cronjob, which also notes which invoices failed to be paid (again for logging purposes)
- Decided think about this overnight :) 

## Day 2 (3 hours spent)
- Got to grips with the `Exposed` library
- Added models and data layer functions for updating invoices and creating payments
- Set up payment endpoints for POSTs
    - Added safety guards for POSTs received in undue time (e.g. too early, if the cronjob restarts)
    - Looked into `koltinx.coroutines` for async speedups in insertions and requests, but decided to leave async compute as a TODO
- Came up with simple example YAML definition for the cronjob

## Day 3 (2 hours spent)
- Got to grips with `Mockk`
- Wrote tests for the `BillingService` in isolation