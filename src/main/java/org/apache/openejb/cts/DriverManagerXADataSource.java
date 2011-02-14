/**
 *
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.apache.openejb.cts;

import javax.sql.ConnectionEventListener;
//import javax.sql.StatementEventListener;
import javax.sql.XAConnection;
import javax.sql.XADataSource;
import javax.sql.StatementEventListener;
import javax.transaction.xa.XAException;
import javax.transaction.xa.XAResource;
import javax.transaction.xa.Xid;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DriverManagerXADataSource implements XADataSource {
    private String jdbcUrl;
    private PrintWriter logWriter;
    private int loginTimeout;

    public String getJdbcUrl() {
        return jdbcUrl;
    }

    public void setJdbcUrl(String jdbcUrl) {
        this.jdbcUrl = jdbcUrl;
    }

    public XAConnection getXAConnection() throws SQLException {
        return new DriverManagerXAConnection(jdbcUrl);
    }

    public XAConnection getXAConnection(String user, String password) throws SQLException {
        return new DriverManagerXAConnection(jdbcUrl, user, password);
    }

    public PrintWriter getLogWriter() {
        return logWriter;
    }

    public void setLogWriter(PrintWriter logWriter) {
        this.logWriter = logWriter;
    }


    public int getLoginTimeout() {
        return loginTimeout;
    }

    public void setLoginTimeout(int loginTimeout) {
        this.loginTimeout = loginTimeout;
    }

    private static class DriverManagerXAConnection implements XAConnection {
        protected Connection connection;
        protected LocalXAResource xaResource;

        public DriverManagerXAConnection(String jdbcUrl) throws SQLException {
            this(jdbcUrl, null, null);
        }

        public DriverManagerXAConnection(String jdbcUrl, String user, String password) throws SQLException {
            connection = null;
            if (user == null) {
                connection = DriverManager.getConnection(jdbcUrl);
            } else {
                connection = DriverManager.getConnection(jdbcUrl, user, password);
            }
            xaResource = new LocalXAResource(connection);
        }

        public XAResource getXAResource() {
            return xaResource;
        }

        public Connection getConnection() throws SQLException {
            return connection;
        }

        public void close() throws SQLException {
            connection.close();
        }

        public void addConnectionEventListener(ConnectionEventListener listener) {
        }

        public void removeConnectionEventListener(ConnectionEventListener listener) {
        }

        public void addStatementEventListener(StatementEventListener listener) {
        }

        public void removeStatementEventListener(StatementEventListener listener) {
        }
    }

    //
    // Coppied from commons dbcp
    //
    protected static class LocalXAResource implements XAResource {
        private final Connection connection;
        private Xid xid;
        private boolean originalAutoCommit;

        public LocalXAResource(Connection localTransaction) {
            this.connection = localTransaction;
        }

        public synchronized Xid getXid() {
            return xid;
        }

        public synchronized void start(Xid xid, int flag) throws XAException {
            if (flag == XAResource.TMNOFLAGS) {
                // first time in this transaction

                // make sure we aren't already in another tx
                if (this.xid != null) {
                    throw new XAException("Already enlisted in another transaction with xid " + xid);
                }

                // save off the current auto commit flag so it can be restored after the transaction completes
                try {
                    originalAutoCommit = connection.getAutoCommit();
                } catch (SQLException ignored) {
                    // no big deal, just assume it was off
                    originalAutoCommit = true;
                }

                // update the auto commit flag
                try {
                    connection.setAutoCommit(false);
                } catch (SQLException e) {
                    throw (XAException) new XAException("Count not turn off auto commit for a XA transaction").initCause(e);
                }

                this.xid = xid;
            } else if (flag == XAResource.TMRESUME) {
                if (xid != this.xid) {
                    throw new XAException("Attempting to resume in different transaction: expected " + this.xid + ", but was " + xid);
                }
            } else {
                throw new XAException("Unknown start flag " + flag);
            }
        }

        public synchronized void end(Xid xid, int flag) throws XAException {
            if (xid == null) throw new NullPointerException("xid is null");
            if (!this.xid.equals(xid)) throw new XAException("Invalid Xid: expected " + this.xid + ", but was " + xid);

            // This notification tells us that the application server is done using this
            // connection for the time being.  The connection is still associated with an
            // open transaction, so we must still wait for the commit or rollback method
        }

        public synchronized int prepare(Xid xid) {
            // if the connection is read-only, then the resource is read-only
            // NOTE: this assumes that the outer proxy throws an exception when application code
            // attempts to set this in a transaction
            try {
                if (connection.isReadOnly()) {
                    // update the auto commit flag
                    connection.setAutoCommit(originalAutoCommit);

                    // tell the transaction manager we are read only
                    return XAResource.XA_RDONLY;
                }
            } catch (SQLException ignored) {
                // no big deal
            }

            // this is a local (one phase) only connectiion, so we can't prepare
            return XAResource.XA_OK;
        }

        public synchronized void commit(Xid xid, boolean flag) throws XAException {
            if (xid == null) throw new NullPointerException("xid is null");
            if (!this.xid.equals(xid)) throw new XAException("Invalid Xid: expected " + this.xid + ", but was " + xid);

            try {
                // make sure the connection isn't already closed
                if (connection.isClosed()) {
                    throw new XAException("Conection is closed");
                }

                // A read only connection should not be committed
                if (!connection.isReadOnly()) {
                    connection.commit();
                }
            } catch (SQLException e) {
                throw (XAException) new XAException().initCause(e);
            } finally {
                try {
                    connection.setAutoCommit(originalAutoCommit);
                } catch (SQLException e) {
                }
                this.xid = null;
            }
        }

        public synchronized void rollback(Xid xid) throws XAException {
            if (xid == null) throw new NullPointerException("xid is null");
            if (!this.xid.equals(xid)) throw new XAException("Invalid Xid: expected " + this.xid + ", but was " + xid);

            try {
                connection.rollback();
            } catch (SQLException e) {
                throw (XAException) new XAException().initCause(e);
            } finally {
                try {
                    connection.setAutoCommit(originalAutoCommit);
                } catch (SQLException e) {
                }
                this.xid = null;
            }
        }

        public boolean isSameRM(XAResource xaResource) {
            return this == xaResource;
        }

        public synchronized void forget(Xid xid) {
            if (xid != null && this.xid.equals(xid)) {
                this.xid = null;
            }
        }

        public Xid[] recover(int flag) {
            return new Xid[0];
        }

        public int getTransactionTimeout() {
            return 0;
        }

        public boolean setTransactionTimeout(int transactionTimeout) {
            return false;
        }
    }
}
