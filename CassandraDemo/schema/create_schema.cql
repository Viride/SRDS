CREATE KEYSPACE IF NOT EXISTS Test
  WITH REPLICATION = { 'class' : 'SimpleStrategy', 'replication_factor' : 3 };

USE Test;

CREATE TABLE Users (
  companyName varchar,
  name varchar,
  phone int,
  street varchar,
  pets list<varchar>,
  PRIMARY KEY (companyName, name)
);

CREATE TABLE screenings (
  hall varchar,
  clientId int,
  clientRequestId int,
  time timestamp,
  numberOfPlaces int,
  PRIMARY KEY ((hall, clientId, clientRequestId), time)
);
