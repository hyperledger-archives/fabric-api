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
package org.hyperledger.jms;

import org.hyperledger.connector.ConnectorConsumer;
import org.hyperledger.connector.ConnectorException;
import org.hyperledger.connector.ConnectorListener;
import org.hyperledger.connector.ConnectorMessage;

import javax.jms.JMSException;
import javax.jms.MessageConsumer;
import javax.jms.Session;

public class JMSConnectorConsumer implements ConnectorConsumer {
    private final MessageConsumer consumer;
    private final Session session;

    public JMSConnectorConsumer(Session session, MessageConsumer consumer) {
        this.consumer = consumer;
        this.session = session;
    }

    @Override
    public void setMessageListener(ConnectorListener listener) throws ConnectorException {
        try {
            consumer.setMessageListener(new JMSConnectorListener(session, listener));
        } catch (JMSException e) {
            throw new ConnectorException(e);
        }
    }

    @Override
    public ConnectorMessage receive() throws ConnectorException {
        try {
            return new JMSConnectorMessage(session, consumer.receive());
        } catch (JMSException e) {
            throw new ConnectorException(e);
        }
    }

    @Override
    public ConnectorMessage receiveNoWait() throws ConnectorException {
        try {
            return new JMSConnectorMessage(session, consumer.receiveNoWait());
        } catch (JMSException e) {
            throw new ConnectorException(e);
        }
    }

    @Override
    public ConnectorMessage receive(long timeout) throws ConnectorException {
        try {
            return new JMSConnectorMessage(session, consumer.receive(timeout));
        } catch (JMSException e) {
            throw new ConnectorException(e);
        }
    }

    @Override
    public void close() throws ConnectorException {
        try {
            consumer.close();
        } catch (JMSException e) {
            throw new ConnectorException(e);
        }
    }
}
