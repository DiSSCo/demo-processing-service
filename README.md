# Demo Processing Service

## Description
First setup of a processing service for Open Digital Specimen.
The application receives OpenDS from either Kafka (`kafka` profile) or a REST endpoint (`web` profile).
All data endpoints are based on CloudEvents, which provides metadata to the data.
The application validates the OpenDS and checks if the object is already in the database, needs to be updated or is already present.
If an OpenDS is new then it checks if there are any enrichment services which need to be triggered (`enrichment` section in Cloud event). 
Collects the JsonSchema from Cordra to preform JsonSchema validation on the object.
Uses the Rest client of Cordra as the performance is much better than the DOIP interface (see https://github.com/DiSSCo/demo-performance-cordra).
For the web applications it will return the newly created object and it also provides a couple of search endpoints.

### Kafka profile
The Kafka profile receives Cloud Event from a queue.
It uses Kafka batches to batch data together for the use of the Cordra batch endpoint.
Together with the Async processing this enhances performance significantly.
Be aware that when the same object is available multiple times in one batch only the first will be processed, the others will be send back to the queue.
This process will be repeated until all updates on the instance are processed.

### Web profile
The Web profile was build to create direct response to a request (sync processing).
This way a user or another application (such as OpenRefine) can get direct feedback on their operation.
This endpoint is for use of small datasets only and handle only one item for create/update per call (no batches).
It has some search/query endpoints as well to enable this, so that user don't have to call two separate endpoints (processing and cordra).
All request need to be authenticated.

## Parameter explanation
Parameters should be supplied as environmental arguments.
Application is expected to run as a docker container or kubernetes job.
Running als commandline application will require code changes (when providing the properties for the envs).

### Profiles
`spring.profiles.active` This determines which profile the application will run with. There are two options:
- `kafka` Which uses a kafka consumer on the queue
- `web` Which runs a web application with REST endpoints, the create endpoint only expects one OpenDS per call

### Cordra parameters
`cordra.username` This parameter needs to be the username of a user with sufficient authorization to create objects   
`cordra.password` Password of the Cordra user  
`cordra.host` The hostname of the Cordra https endpoint, for example `https://localhost:8443`  
`cordra.type` The type for which the JsonSchema needs to be collected, for example `ODStypeV0.2-Test`  

### Kafka consumer parameters (only needed with `kafka` profile)
`kafka.consumer.host` This parameter contains the hostname for the Kafka host, for example `localhost:9092`  
`kafka.consumer.group` Parameter to indicate the Kafka group for the consumer  
`kafka.consumer.topic` Topic on which the Consumer will listen to  

### Kafka publisher parameters
`kafka.publisher.host` This parameter contains the hostname for the Kafka host, for example `localhost:9092`  

### Keyclaok parameters
When running the application with the `web` profile we authenticate calls through Keycloak
`keycloak.auth-server-url` The keycloak server auth endpoint url  
`keycloak.realm` The Keyclaok realm  
`keycloak.resource`  The Keycloak client used  

#### Example
```
cordra.host=https://localhost:8443
cordra.username=
cordra.password=
cordra.type=ODStypeV0.2-Demo

kafka.publisher.host=localhost:9092

spring.profiles.active=web

keycloak.auth-server-url=
keycloak.realm=
keycloak.resource=
```

### Installation instructions

### IDE
Pull the code from Github and open in your IDE.
Fill in the `application.properties` with the parameters described above.
Run the application.

### Docker
Ensure that parameters are either available as environmental variables are added in the `application.properties`.
Build the Dockerfile with `docker build . -t demo-naturalis-api`
Run the container with `docker run demo-naturalis-api`

### Kubernetes
Added is a Kubernetes yaml for easy deployment on a kubernetes cluster.
