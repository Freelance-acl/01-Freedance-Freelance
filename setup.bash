# INSTALL PACKAGES
./mvnw.cmd clean install

# TO BUILD PROJECT
./mvnw.cmd package -DskipTests

# TO START RUNNING
docker-compose up -d

# TO STOP RUNNING
# docker-compose down