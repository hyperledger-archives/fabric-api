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

import org.hyperledger.connector.ConnectorDestination;
import org.hyperledger.connector.ConnectorException;
import org.hyperledger.connector.ConnectorMessage;
import org.hyperledger.connector.ConnectorProducer;

import javax.jms.BytesMessage;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.Session;

public class JMSConnectorMessage implements ConnectorMessage {
    private Session session;
    private BytesMessage message;

    public JMSConnectorMessage(Session session) throws JMSException {
        message = session.createBytesMessage();
        this.session = session;
    }

    public JMSConnectorMessage(Session session, Message message) throws JMSException {
        this.message = (BytesMessage) message;
        this.session = session;
    }

    public BytesMessage getMessage() {
        return message;
    }

    @Override
    public void setPayload(byte[] payload) throws ConnectorException {
        try {
            message.writeBytes(payload);
        } catch (JMSException e) {
            throw new ConnectorException(e);
        }
    }

    @Override
    public byte[] getPayload() throws ConnectorException {
        byte[] body = null;
        try {
            if (message.getBodyLength() > 0) {
                body = new byte[(int) message.getBodyLength()];
                message.readBytes(body);
                message.reset();
            }
        } catch (JMSException e) {
            throw new ConnectorException(e);
        }
        return body;
    }

    @Override
    public ConnectorProducer getReplyProducer() throws ConnectorException {
        try {
            return new JMSConnectorProducer(session.createProducer(message.getJMSReplyTo()));
        } catch (JMSException e) {
            throw new ConnectorException(e);
        }
    }

    @Override
    public void setReplyTo(ConnectorDestination replyTo) throws ConnectorException {
        try {
            message.setJMSReplyTo(((JMSDestination) replyTo).getDestination());
        } catch (JMSException e) {
            throw new ConnectorException(e);
        }
    }
}
