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

import org.hyperledger.connector.ConnectorException;
import org.hyperledger.connector.ConnectorMessage;
import org.hyperledger.connector.ConnectorProducer;

import javax.jms.JMSException;
import javax.jms.MessageProducer;

public class JMSConnectorProducer implements ConnectorProducer {
    private MessageProducer producer;

    public JMSConnectorProducer(MessageProducer producer) {
        this.producer = producer;
    }

    @Override
    public void send(ConnectorMessage message) throws ConnectorException {
        try {
            producer.send(((JMSConnectorMessage) message).getMessage());
        } catch (JMSException e) {
            throw new ConnectorException(e);
        }
    }

    @Override
    public void close() throws ConnectorException {
        try {
            producer.close();
        } catch (JMSException e) {
            throw new ConnectorException(e);
        }
    }
}
