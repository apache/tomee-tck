/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.openejb.cts;

import com.sun.ts.lib.porting.TSJMSAdminException;
import com.sun.ts.lib.porting.TSJMSAdminInterface;
import org.apache.activemq.ActiveMQConnection;
import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.activemq.command.ActiveMQQueue;
import org.apache.activemq.command.ActiveMQTopic;

import javax.jms.JMSException;
import java.io.PrintWriter;

public class JMSAdminImpl implements TSJMSAdminInterface {
    private static final String HEAD = "OpenEJB - ";
    private PrintWriter log;

    public void init(final PrintWriter writer) {
        this.log = writer;
        log.println(HEAD + "initialized JMSAdmin helper");
    }

    public void createQueueConnectionFactories(final String[] queueConnectionFactories, final String[] props)
        throws TSJMSAdminException {
    }

    public void createQueues(final String[] queues) throws TSJMSAdminException {
    }

    public void removeQueues(final String[] queues) throws TSJMSAdminException {
        ActiveMQConnection connection = null;
        try {
            final ActiveMQConnectionFactory factory = new ActiveMQConnectionFactory("tcp://localhost:61616");
            connection = (ActiveMQConnection) factory.createConnection();
            connection.start();
            for (int i = 0; i < queues.length; i++) {
                log.println(HEAD + "Destroying Queue: " + queues[i]);
                connection.destroyDestination(new ActiveMQQueue(queues[i]));
            }
        } catch (final JMSException e) {
            e.printStackTrace();
        } finally {
            try {
                connection.close();
            } catch (final Throwable ignore) {
            }
        }
    }

    public void createTopicConnectionFactories(final String[] topicConnectionFactories, final String[] props)
        throws TSJMSAdminException {
    }

    public void createTopics(final String[] topics) throws TSJMSAdminException {
    }

    public void removeTopics(final String[] topics) throws TSJMSAdminException {
        ActiveMQConnection connection = null;
        try {
            final ActiveMQConnectionFactory factory = new ActiveMQConnectionFactory("tcp://localhost:61616");
            connection = (ActiveMQConnection) factory.createConnection();
            connection.start();
            for (int i = 0; i < topics.length; i++) {
                log.println(HEAD + "Destroying Topic: " + topics[i]);
                connection.destroyDestination(new ActiveMQTopic(topics[i]));
            }
        } catch (final JMSException e) {
            e.printStackTrace();
        } finally {
            try {
                connection.close();
            } catch (final Throwable ignore) {
            }
        }
    }

    public void removeJmsConnectionFactories(final String[] jmsConnectionFactoryNames) throws TSJMSAdminException {
    }
}
