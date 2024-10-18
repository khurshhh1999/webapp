#!/bin/bash

DB_USER=${DB_USER:-your_db_user}
DB_PASSWORD=${DB_PASSWORD:-your_db_password}

# Switch to postgres user and run PostgreSQL commands
sudo -i -u postgres bash << EOF

# Create the database
psql -c "CREATE DATABASE webapp;"

# Create the user with a password
psql -c "CREATE USER $DB_USER WITH PASSWORD '$DB_PASSWORD';"

# Grant privileges to the user on the database
psql -c "GRANT ALL PRIVILEGES ON DATABASE webapp TO $DB_USER;"

# Grant privileges on schema and tables
psql -d webapp -c "GRANT ALL PRIVILEGES ON SCHEMA public TO $DB_USER;"
psql -d webapp -c "GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA public TO $DB_USER;"
psql -d webapp -c "ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL PRIVILEGES ON TABLES TO $DB_USER;"

# Change ownership of the database
psql -c "ALTER DATABASE webapp OWNER TO $DB_USER;"

EOF
