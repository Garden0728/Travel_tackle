# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Commands

```bash
# Build
./gradlew build

# Run the application
./gradlew bootRun

# Run all tests
./gradlew test

# Run a single test class
./gradlew test --tests "Timeout.travel_tackle.<TestClassName>"

# Generate QueryDSL Q-classes (run after adding/modifying @Entity classes)
./gradlew compileJava

# Clean build outputs AND generated Q-classes
./gradlew clean
```

> After `./gradlew clean`, always re-run `./gradlew compileJava` to regenerate Q-classes before building.

## Architecture

**Stack**: Spring Boot 4.1.0 · Java 21 · Spring Data JPA · H2 (in-memory) · Lombok · QueryDSL 6.0 (OpenFeign, Jakarta)

**Root package**: `Timeout.travel_tackle` (note: PascalCase — intentional group convention)

### QueryDSL setup

Q-classes are generated into `src/main/generated/` (tracked in `sourceSets`, **not** inside `build/`). This directory is included as a source root and is wiped by `clean`. The annotation processors used are:

- `io.github.openfeign.querydsl:querydsl-apt:6.0`
- `jakarta.annotation:jakarta.annotation-api`
- `jakarta.persistence:jakarta.persistence-api`

When adding a new `@Entity`, run `./gradlew compileJava` to produce its corresponding `Q<EntityName>` class in `src/main/generated/`.

### Data layer

H2 runs in-memory; there is no external database to configure for local development or tests. JPA DDL is managed automatically by Hibernate on startup.
