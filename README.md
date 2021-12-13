Cloud Bowl Sample - Java Spring Boot
------------------------------------

To make changes, edit the `src/main/java/hello/Application.java` file.

Run Locally:
```
./mvnw spring-boot:run
```

[![Run on Google Cloud](https://deploy.cloud.run/button.svg)](https://deploy.cloud.run)

Containerize & Run Locally:
```
export PROJECT_ID=YOUR_GCP_PROJECT_ID
pack build --builder=gcr.io/buildpacks/builder gcr.io/$PROJECT_ID/cloudbowl-samples-java-springboot
docker run -it -ePORT=8080 -p8080:8080 gcr.io/$PROJECT_ID/cloudbowl-samples-java-springboot
```
