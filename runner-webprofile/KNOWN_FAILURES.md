# Known Web Profile compatibility gaps

Exclusions keep the remaining suite runnable; they do not turn a result into a
compatibility pass. Each entry corresponds to a narrow pattern in `exclusions/`.

The EclipseLink-based TomEE Plume distribution is the default target under
test. The table below reflects the default Plume suite; the persistence
results are in the [TomEE Plume section](#tomee-plume-eclipselink) below.
`TOMEE_CLASSIFIER=webprofile` runs use the overrides in
`exclusions/webprofile/` instead.

The generated TomEE overlay also replaces `taglibs-shade`, which still contains
legacy JSTL TLD URIs, with the Jakarta Tags 3.0 API and GlassFish
implementation. TomEE/Tomcat still does not expose the replacement jar's
`jakarta.tags.*` TLD mappings to applications. All 50 Tags classes exercise
those URIs and are excluded below; one method in the compatibility class avoids
them, but retaining one method would not constitute useful Tags coverage.

| Partition | Excluded class | Reproduced | Product gap |
|---|---|---|---|
| `enterprise-beans-32` | All 18 `ClientEjblitejspTest` vehicle classes | Java 21, 2026-07-14 | The shared EJB Lite JSP vehicle imports `jakarta.tags.core`, so Jasper returns HTTP 500 before any EJB assertion can run. The corresponding servlet, filtered-servlet, and Faces vehicles retain the EJB coverage; this exclusion is a secondary effect of the Jakarta Tags URI gap. |
| `enterprise-beans-32` | Six non-JSP schedule-transaction vehicle classes, covering persistent and nonpersistent timers | Java 21, 2026-07-14 | Timer callbacks reproduce TomEE's transaction-manager fault: an EJB starts but does not complete its transaction, failed timeout callbacks are not retried as required, and rollback state leaks between methods. |
| `enterprise-beans-30` | All 73 `ClientEjblitejspTest` vehicle classes | Java 21, 2026-07-14 | The shared EJB Lite JSP vehicle imports `jakarta.tags.core`, so Jasper returns HTTP 500 before any EJB assertion can run. The corresponding servlet, filtered-servlet, and Faces vehicles retain the EJB coverage; this exclusion is a secondary effect of the Jakarta Tags URI gap. |
| `enterprise-beans-30` | Three Faces/CDI lifecycle or concurrency classes | Java 21, 2026-07-14 | OpenWebBeans refuses to proxy interceptor hierarchies containing final lifecycle or business-interceptor methods, so deployment fails before the EJB assertions. Corresponding servlet variants remain enabled. |
| `enterprise-beans-30` | Three retained naming-context vehicles | Java 21, 2026-07-14 | TomEE permits mutation of `java:comp`; bind, rebind, and rename operations succeed where the EJB naming-context contract requires `OperationNotSupportedException`. |
| `enterprise-beans-30` | Twelve lifecycle, managed-bean, and singleton-concurrency vehicle classes | Java 21, 2026-07-14 | A failed CDI/EJB deployment leaves deployment IDs registered in TomEE. Related applications then fail with `DuplicateDeploymentIdException` rather than reaching their assertions. |
| `enterprise-beans-30` | Twenty-one bean- and container-managed transaction vehicle classes | Java 21, 2026-07-14 | TomEE reports that an EJB started but did not complete a transaction while the tests exercise transaction-attribute semantics across singleton, stateful, and stateless beans. |
| `signature` | Both Web Profile signature vehicle wrappers | Java 21, 2026-07-14 | TomEE's `webprofile` distribution exposes Jakarta Batch and Jakarta Messaging packages through its combined `jakartaee-api` jar even though these optional technologies are not declared by `javaee.level=web`. The required Web Profile packages, including the corrected Jakarta Tags API, pass the signature check. Optional technologies must instead be removed from this distribution or declared and covered by their complete TCKs. |
| `json-binding` | Serializer customization CDI test in JSP and servlet vehicles | Java 21, 2026-07-14 | TomEE's Johnzon integration does not inject the CDI-managed field in the `@JsonbTypeDeserializer` used for a nested generic type. CDI adapter injection and provider-selection tests remain covered. |
| `transactions` | Four `jta.ee.transactional.ClientEjblite*Test` vehicle classes | Java 21, 2026-07-14 | TomEE's CDI transactional interceptors fail transaction propagation, rollback-rule, and `TransactionScoped` context assertions across the servlet, JSP, JSF, and filtered-servlet vehicles. A failed assertion can leave transaction state active and cascade into later methods, so the affected vehicle classes are excluded as a unit. |
| `transactions` | JSP and servlet vehicles for `UserTransaction` rollback, `setRollbackOnly`, and timeout (six classes) | Java 21, 2026-07-14 | TomEE misses required `IllegalStateException`/`RollbackException` outcomes, and timed-out transactions remain associated with request threads. The resulting `NotSupportedException` failures move between methods according to execution order, so method-level exclusions would be unstable. Begin, commit, and status classes remain covered. |
| `tags` | All 50 classes using the Jakarta Tags 3 short URIs | Java 21, 2026-07-14 | Jasper treats `jakarta.tags.core`, `jakarta.tags.fmt`, `jakarta.tags.functions`, `jakarta.tags.sql`, and `jakarta.tags.xml` as resource paths because the TomEE common-library TLDs are not registered. |
| `persistence-servlet` | `ee.jakarta.tck.persistence.ee.cdi.ServletEMLookupTest` | Java 21, 2026-07-17 | TomEE does not register the Jakarta Persistence 3.2 CDI qualifier beans (`EntityManagerFactory`/`EntityManager`/`PersistenceUnitUtil`) declared in `persistence.xml`; deployment fails with an `UnsatisfiedResolutionException` for `@CtsEm2Qualifier`. |
| `rest` | Two methods in `jaxrs21.platform.providers.jsonp.JAXRSClientIT` | Java 21, 2026-07-14 | The TomEE/CXF client has no message-body writer for scalar JSON-P `JsonString` and `JsonNumber` entities. This is part of the default-provider work tracked by [TOMEE-4436](https://issues.apache.org/jira/browse/TOMEE-4436). |
| `rest` | Ten methods in `platform.beanvalidation.annotation.JAXRSClientIT`, plus both exception-mapper classes | Java 21, 2026-07-14 | `CxfRsHttpListener` does not install JAX-RS Bean Validation interceptors when CDI is active, so invalid arguments and return values are not validated. This remains recorded under the [TomEE REST TCK umbrella](https://issues.apache.org/jira/browse/TOMEE-4166). |
| `rest` | `platform.environment.servlet.JAXRSClientIT#checkServletExtensionTest` | Java 21, 2026-07-14 | `@Context ServletConfig` resolves to `null` through `OpenEJBRestServlet`; the other three servlet-environment tests pass. This remains recorded under the [TomEE REST TCK umbrella](https://issues.apache.org/jira/browse/TOMEE-4166). |
| `rest` | `platform.managedbean299.JAXRSClientIT` | Java 21, 2026-07-14 | TomEE hands the CDI-managed `ApplicationHolderSingleton` to CXF as a constructible resource class. CXF rejects its non-public constructor and the application has no active endpoints. This remains recorded under the [TomEE REST TCK umbrella](https://issues.apache.org/jira/browse/TOMEE-4166). |

## TomEE Plume (EclipseLink)

The complete 450-class `persistence-javatest` catalog and the
`persistence-servlet` class were run against the TomEE Plume snapshot
(EclipseLink 5.0.1) on Java 21, 2026-07-17, with no exclusions applied.
448 of 450 javatest classes pass (3,817 tests). The default persistence
exclusions (`exclusions/persistence-javatest.txt` and
`exclusions/persistence-servlet.txt`) contain only:

| Partition | Excluded class | Reproduced | Product gap |
|---|---|---|---|
| `persistence-javatest` | Both `entityManagerFactoryCloseExceptions` servlet vehicles | Java 21, 2026-07-17 | The `exceptionsTest` methods themselves pass. The test legitimately closes the container-managed `EntityManagerFactory`; TomEE's subsequent undeploy calls `close()` again and `Assembler.destroyApplication` fails with "Attempting to execute an operation on a closed EntityManagerFactory", so the class reports an undeploy error. TomEE must tolerate an already-closed EMF during undeploy. |
| `persistence-servlet` | `ee.jakarta.tck.persistence.ee.cdi.ServletEMLookupTest` | Java 21, 2026-07-17 | TomEE does not register the Jakarta Persistence 3.2 CDI qualifier beans (`EntityManagerFactory`/`EntityManager`/`PersistenceUnitUtil`) declared in `persistence.xml`; deployment fails with an `UnsatisfiedResolutionException` for `@CtsEm2Qualifier`. |

All remaining partitions were re-run on Plume on Java 21, 2026-07-17: every
partition passes with the same expectations and exclusions recorded above.
