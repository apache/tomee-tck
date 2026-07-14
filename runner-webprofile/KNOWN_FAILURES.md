# Known Web Profile compatibility gaps

Exclusions keep the remaining suite runnable; they do not turn a result into a
compatibility pass. Each entry corresponds to a narrow pattern in `exclusions/`.

| Partition | Excluded class | Reproduced | Product gap |
|---|---|---|---|
| `persistence-servlet` | `ee.jakarta.tck.persistence.ee.cdi.ServletEMLookupTest` | Java 21, 2026-07-14 | TomEE/OpenJPA does not provide the Jakarta Persistence 3.2 CDI `PersistenceUnitUtil` bean with the TCK qualifier. [OPENJPA-2940](https://issues.apache.org/jira/browse/OPENJPA-2940) tracks the incomplete dependency-injection integration. |
