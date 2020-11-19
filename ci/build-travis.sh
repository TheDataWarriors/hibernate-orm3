#! /bin/bash

java -version
./gradlew assemble
if [ "$RDBMS" == 'mysql' ]; then
  sudo service mysql stop
  bash ../docker_db.sh mysql_5_7
elif [ "$RDBMS" == 'mysql8' ]; then
  sudo service mysql stop
  bash ../docker_db.sh mysql_8_0
elif [ "$RDBMS" == 'mariadb' ]; then
  sudo service mysql stop
  bash ../docker_db.sh mariadb
elif [ "$RDBMS" == 'postgresql' ]; then
  sudo service postgres stop
  bash ../docker_db.sh postgresql_9_5
elif [ "$RDBMS" == 'db2' ]; then
  bash ../docker_db.sh db2
elif [ "$RDBMS" == 'oracle' ]; then
  bash ../docker_db.sh oracle
elif [ "$RDBMS" == 'mssql' ]; then
  bash ../docker_db.sh mssql
fi

exec bash ./build.sh