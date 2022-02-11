# Demo Processing Service

## Description
First setup of a processing service for Open Digital Specimen.
The application receives OpenDS from Kafka.
Validates the OpenDS and checks if the object is already in the database, needs to be updated or is already present.
Uses the Rest client of Cordra as the performance is much better than the DOIP interface (see https://github.com/DiSSCo/demo-performance-cordra).

## Parameter explanation
Parameters should be supplied as environmental arguments.
Application is expected to run as a docker container or kubernetes job.
Running als commandline application will require code changes (when providing the properties for the envs).

### Cordra parameters
`cordra.username` This parameter needs to be the username of a user with sufficient authorization to create objects   
`cordra.password` Password of the Cordra user  
`cordra.host` The hostname of the Cordra https endpoint, for example `https://localhost:8443`  

### Kafka parameters
`kafka.host` This parameter contains the hostname for the Kafka host, for example `localhost:9092`
`kafka.group` Parameter to indicate the Kafka group for the consumer
`kafka.topic` Topic on which the Consumer will listen to

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
