# akka-scala-jdbc-demo
Akka jdbc demo
with Database PostgreSQL 9.4

# Application details

## DataSourceSupervisorActor
 is parent actor for DataSourceActor and is responsible by creating and monitoring of DataSourceActor
This actor is created when actor system is starting

## DataSourceActor
This actor is created by  DataSourceSupervisorActor at the beginning of start application,
contains  DataSource as a connection pool for RDBMS

## UserActionActor
this actor represent particular business process in our case it is registration of user
Actor per request
Is parent of the rest Actors used for this business process
Represents also boundary of transaction

## JDBCConncetionActor
represents SQL transaction (for JDBC is connection == transaction)
Is stateful actor and its state is representing with connection obtained from DataSourceActor
Each sql statement is executed in this actor
This actor changes dynamically behavior depends on connection state

## ConnectionInterceptorActor
This actor intercepting if JDBCConnectionActor get connection in specific time from DataSourceActor, if not actor will kill JDBCConncetionActor

## UserServiceActor, AddressServiceActor
Are service actors oriented to domains object “User”, “Address”
Are stateless actors

## UserRespositoryActor, AddressRepositoryActor
Are responsible by creating SQL queries and sending those queries to JDBCConnetcionActor
Also are stateless actors 
