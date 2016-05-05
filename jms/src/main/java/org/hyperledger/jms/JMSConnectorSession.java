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

import org.hyperledger.connector.*;

import javax.jms.JMSException;
import javax.jms.Session;

public class JMSConnectorSession implements ConnectorSession {
    private Session session;

    public JMSConnectorSession(Session session) {
        this.session = session;
    }

    public Session getJMSSession() {
        return session;
    }

    @Override
    public ConnectorMessage createMessage() throws ConnectorException {
        try {
            return new JMSConnectorMessage(session);
        } catch (JMSException e) {
            throw new ConnectorException(e);
        }
    }

    @Override
    public ConnectorProducer createProducer(ConnectorDestination destination) throws ConnectorException {
        try {
            if (destination instanceof JMSConnectorQueue) {
                return new JMSConnectorProducer(session.createProducer(((JMSConnectorQueue) destination).getQueue()));
            }
            if (destination instanceof JMSConnectorTopic) {
                return new JMSConnectorProducer(session.createProducer(((JMSConnectorTopic) destination).getTopic()));
            }
            return null;
        } catch (JMSException e) {
            throw new ConnectorException(e);
        }
    }

    @Override
    public ConnectorTemporaryQueue createTemporaryQueue() throws ConnectorException {
        try {
            return new JMSConnectorTemporaryQueue(session);
        } catch (JMSException e) {
            throw new ConnectorException(e);
        }
    }

    @Override
    public ConnectorQueue createQueue(String name) throws ConnectorException {
        try {
            return new JMSConnectorQueue(session, name);
        } catch (JMSException e) {
            throw new ConnectorException(e);
        }
    }

    @Override
    public ConnectorTopic createTopic(String name) throws ConnectorException {
        try {
            return new JMSConnectorTopic(session, name);
        } catch (JMSException e) {
            throw new ConnectorException(e);
        }
    }

    @Override
    public ConnectorConsumer createConsumer(ConnectorDestination destination) throws ConnectorException {
        try {
            if (destination instanceof JMSConnectorQueue) {
                return new JMSConnectorConsumer(session, session.createConsumer(((JMSConnectorQueue) destination).getDestination()));
            }
            if (destination instanceof JMSConnectorTopic) {
                return new JMSConnectorConsumer(session, session.createConsumer(((JMSConnectorTopic) destination).getDestination()));
            }
            if (destination instanceof JMSConnectorTemporaryQueue) {
                return new JMSConnectorConsumer(session, session.createConsumer(((JMSConnectorTemporaryQueue) destination).getDestination()));
            }
            return null;
        } catch (JMSException e) {
            throw new ConnectorException(e);
        }
    }

    @Override
    public void close() throws ConnectorException {
        try {
            session.close();
        } catch (JMSException e) {
            throw new ConnectorException(e);
        }
    }
}
