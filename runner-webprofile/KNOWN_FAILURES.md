# Known Web Profile compatibility gaps

Exclusions keep the remaining suite runnable; they do not turn a result into a
compatibility pass. Each entry corresponds to a narrow pattern in `exclusions/`.

The generated TomEE overlay also replaces `taglibs-shade`, which still contains
legacy JSTL TLD URIs, with `org.glassfish.web:jakarta.servlet.jsp.jstl:3.0.1`.
TomEE/Tomcat still does not expose the replacement jar's `jakarta.tags.*` TLD
mappings to applications. All 50 Tags classes exercise those URIs and are
excluded below; one method in the compatibility class avoids them, but retaining
one method would not constitute useful Tags coverage.

| Partition | Excluded class | Reproduced | Product gap |
|---|---|---|---|
| `tags` | All 50 classes using the Jakarta Tags 3 short URIs | Java 21, 2026-07-14 | Jasper treats `jakarta.tags.core`, `jakarta.tags.fmt`, `jakarta.tags.functions`, `jakarta.tags.sql`, and `jakarta.tags.xml` as resource paths because the TomEE common-library TLDs are not registered. |
| `persistence-servlet` | `ee.jakarta.tck.persistence.ee.cdi.ServletEMLookupTest` | Java 21, 2026-07-14 | TomEE/OpenJPA does not provide the Jakarta Persistence 3.2 CDI `PersistenceUnitUtil` bean with the TCK qualifier. [OPENJPA-2940](https://issues.apache.org/jira/browse/OPENJPA-2940) tracks the incomplete dependency-injection integration. |
| `rest` | Two methods in `jaxrs21.platform.providers.jsonp.JAXRSClientIT` | Java 21, 2026-07-14 | The TomEE/CXF client has no message-body writer for scalar JSON-P `JsonString` and `JsonNumber` entities. This is part of the default-provider work tracked by [TOMEE-4436](https://issues.apache.org/jira/browse/TOMEE-4436). |
| `rest` | Ten methods in `platform.beanvalidation.annotation.JAXRSClientIT`, plus both exception-mapper classes | Java 21, 2026-07-14 | `CxfRsHttpListener` does not install JAX-RS Bean Validation interceptors when CDI is active, so invalid arguments and return values are not validated. This remains recorded under the [TomEE REST TCK umbrella](https://issues.apache.org/jira/browse/TOMEE-4166). |
| `rest` | `platform.environment.servlet.JAXRSClientIT#checkServletExtensionTest` | Java 21, 2026-07-14 | `@Context ServletConfig` resolves to `null` through `OpenEJBRestServlet`; the other three servlet-environment tests pass. This remains recorded under the [TomEE REST TCK umbrella](https://issues.apache.org/jira/browse/TOMEE-4166). |
| `rest` | `platform.managedbean299.JAXRSClientIT` | Java 21, 2026-07-14 | TomEE hands the CDI-managed `ApplicationHolderSingleton` to CXF as a constructible resource class. CXF rejects its non-public constructor and the application has no active endpoints. This remains recorded under the [TomEE REST TCK umbrella](https://issues.apache.org/jira/browse/TOMEE-4166). |
