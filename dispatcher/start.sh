git pull
mvn clean package -DskipTests=true
java -jar target/dispatcher-1.0-SNAPSHOT.jar
