# HibernateStudentApp

Maven-based Hibernate application that maps a `Student` entity to a MySQL table.

## Features & Improvements
- **JUnit 5 Unit & Integration Tests**: Run in-memory H2 database testing offline (`mvn test`).
- **Fat Executable JAR Deployment**: Package all dependencies into a standalone executable JAR (`mvn package`).
- **JDK 17/25 Compatibility**: Modern compiler settings and Jakarta Persistence API integration.

## Database
- **Host**: `db01.dbhost.dev`
- **Port**: `5051`
- **Database**: `db_4559rmz8s`

## Testing

Run automated JUnit 5 & H2 in-memory database tests:

```bash
mvn clean test
```

## Build & Deploy Executable JAR

Build the self-contained executable Fat JAR with all bundled dependencies:

```bash
mvn clean package
```

Run the deployed executable JAR:

```bash
java -jar target/HibernateStudentApp-1.0-SNAPSHOT-executable.jar
```

Or run via Maven:

```bash
mvn exec:java
```

## Verify Database Content

Run in MySQL:

```sql
SELECT * FROM student;
```

> **Note**: Database connection details are configured in `src/main/resources/hibernate.cfg.xml`.

## Requested Default Student
- **ID:** 101
- **Name:** Vasanth Prasath S
- **Email:** vasanthprasathsekar@gmail.com
- **Course:** Artificial Intelligence and Data Science

The application startup code updates the ID 101 record to this profile every time the application starts.
For an already-running remote database, run `database_update.sql` once before verifying the deployed UI.
