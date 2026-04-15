./mvnw.cmd package -DskipTests

docker-compose up -d --build

# try go to http://localhost:8081/api/users/health or http://localhost:8082/api/jobs/health

# Service          InternalPort     DockerPort
# user-service     8080                8081
# job-service      8080                8082
# proposal-service 8080                8083
# contract-service 8080                8084
# wallet-service   8080                8085

# TO STOP RUNNING
# docker-compose down 