CREATE USER IF NOT EXISTS sample_user WITH PASSWORD 'password' NOSUPERUSER;

CREATE KEYSPACE IF NOT EXISTS sample_keyspace
    WITH REPLICATION = { 'class' : 'SimpleStrategy', 'replication_factor' : 1 }
    AND DURABLE_WRITES = true;

GRANT ALL PERMISSIONS ON KEYSPACE sample_keyspace TO sample_user;
