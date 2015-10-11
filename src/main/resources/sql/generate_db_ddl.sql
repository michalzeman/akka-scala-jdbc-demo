/** set shema name */

SET SCHEMA 'akkademo';

DROP SCHEMA IF EXISTS akkademo CASCADE;

CREATE SCHEMA akkademo AUTHORIZATION postgres;

CREATE TABLE users (
  id  bigserial NOT NULL,
  last_name     VARCHAR(255),
  first_name    VARCHAR(255),
  address_id    bigint,
  CONSTRAINT users_pk PRIMARY KEY (id)
);

CREATE TABLE addresses (
  id  bigserial NOT NULL,
  street        VARCHAR(255),
  house_number  INTEGER,
  zip           VARCHAR(5),
  city          VARCHAR(255),
  CONSTRAINT addresses_pk PRIMARY KEY (id)
);

ALTER TABLE users ADD CONSTRAINT fk_users_addresses FOREIGN KEY (address_id) REFERENCES addresses(id) ON UPDATE CASCADE;