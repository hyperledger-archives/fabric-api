/**
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.hyperledger.api;

import org.hyperledger.connector.*;
import org.hyperledger.jms.JMSConnectorFactory;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.concurrent.Semaphore;

import static org.junit.Assert.assertTrue;

public class JMSConnectorTest {
    private final Semaphore ready = new Semaphore(0);

    private static JMSConnectorFactory factory = new JMSConnectorFactory(null, null, null);
    private static Connector connection;

    @BeforeClass
    public static void startJMS() {
        factory.setBrokerUrl("vm://localhost?broker.persistent=false");
        try {
            connection = factory.getConnector();
            connection.start();
        } catch (ConnectorException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void mockTopicTest() throws ConnectorException {
        final ConnectorSession session = connection.createSession();
        ConnectorProducer producer = session.createProducer(session.createTopic("test"));
        final ConnectorConsumer consumer = session.createConsumer(session.createTopic("test"));
        consumer.setMessageListener(message -> {
            try {
                assertTrue(new String(message.getPayload()).equals("hello"));
            } catch (ConnectorException e) {
                assertTrue(false);
            }
            ready.release();
        });

        ConnectorMessage m = session.createMessage();
        m.setPayload("hello".getBytes());
        producer.send(m);
        ready.acquireUninterruptibly();
    }

    // @Test
    public void mockQueueTest() throws ConnectorException {
        final ConnectorSession session = connection.createSession();
        ConnectorProducer producer = session.createProducer(session.createTopic("test"));
        final ConnectorConsumer consumer = session.createConsumer(session.createTopic("test"));
        consumer.setMessageListener(message -> {
            try {
                assertTrue(new String(message.getPayload()).equals("hello"));

                ConnectorProducer replyProducer = message.getReplyProducer();
                replyProducer.send(message);
            } catch (ConnectorException e) {
                assertTrue(false);
            }
        });

        ConnectorDestination temp = session.createTemporaryQueue();
        final ConnectorConsumer replyConsumer = session.createConsumer(temp);
        replyConsumer.setMessageListener(message -> {
            try {
                assertTrue(new String(message.getPayload()).equals("hello"));
            } catch (ConnectorException e) {
                assertTrue(false);
            }
            ready.release();
        });

        ConnectorMessage m = session.createMessage();
        m.setPayload("hello".getBytes());
        m.setReplyTo(temp);
        producer.send(m);
        ready.acquireUninterruptibly();
    }
}
