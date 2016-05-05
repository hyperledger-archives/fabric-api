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

import org.hyperledger.connector.Connector;
import org.hyperledger.connector.ConnectorException;
import org.hyperledger.connector.ConnectorSession;

import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.JMSException;
import javax.jms.Session;

public class JMSConnector implements Connector {
    private Connection connection;

    public JMSConnector(ConnectionFactory connectionFactory) throws JMSException {
        connection = connectionFactory.createConnection();
    }

    @Override
    public ConnectorSession createSession() throws ConnectorException {
        try {
            return new JMSConnectorSession(connection.createSession(false, Session.AUTO_ACKNOWLEDGE));
        } catch (JMSException e) {
            throw new ConnectorException(e);
        }
    }

    @Override
    public void start() throws ConnectorException {
        try {
            connection.start();
        } catch (JMSException e) {
            throw new ConnectorException(e);
        }
    }

    @Override
    public void close() throws ConnectorException {
        try {
            connection.close();
        } catch (JMSException e) {
            throw new ConnectorException(e);
        }
    }

    @Override
    public void setClientID(String string) throws ConnectorException {
        try {
            connection.setClientID(string);
        } catch (JMSException e) {
            throw new ConnectorException(e);
        }
    }
}
