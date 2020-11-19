package org.apache.openejb.cts;

import org.apache.catalina.connector.Request;
import org.apache.catalina.connector.Response;
import org.apache.catalina.valves.ValveBase;

import javax.naming.InitialContext;
import jakarta.servlet.ServletException;
import jakarta.transaction.UserTransaction;
import java.io.IOException;

/**
 * This is a workaround for the Transaction tests in JTA
 * See https://github.com/eclipse-ee4j/jakartaee-tck/issues/573
 */
public class TransactionalWorkaroundLeakGuardValve extends ValveBase {

    public TransactionalWorkaroundLeakGuardValve() {
        this(true);
    }

    public TransactionalWorkaroundLeakGuardValve(final boolean asyncSupported) {
        super(asyncSupported);
    }

    @Override
    public void invoke(final Request request, final Response response) throws IOException, ServletException {

        // attempt to cleanup the thread if any transaction is started.
        try {
            final UserTransaction userTransaction =
                (UserTransaction) new InitialContext().lookup("java:comp/UserTransaction");
            userTransaction.rollback();

        } catch (final Exception e) {
            // ignore and swallow - nothing else we can do quickly to fix TCK garbage

        } finally {
            getNext().invoke(request, response);
        }

    }
}
