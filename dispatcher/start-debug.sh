git pull
mvn clean package -DskipTests=true
java -agentlib:jdwp=transport=dt_socket,address=*:8000,suspend=n,server=y -jar target/dispatcher-1.0-SNAPSHOT.jar
