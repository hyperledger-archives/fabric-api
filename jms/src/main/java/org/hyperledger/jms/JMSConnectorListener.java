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

import org.hyperledger.connector.ConnectorListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageListener;
import javax.jms.Session;

public class JMSConnectorListener implements MessageListener {
    private static final Logger log = LoggerFactory.getLogger(JMSConnectorListener.class);

    private final ConnectorListener listener;
    private final Session session;

    public JMSConnectorListener(Session session, ConnectorListener listener) {
        this.listener = listener;
        this.session = session;
    }

    @Override
    public void onMessage(Message message) {
        try {
            JMSConnectorMessage m = new JMSConnectorMessage(session, message);
            listener.onMessage(m);
        } catch (JMSException e) {
            log.error("Unable to parse message", e);
        }
    }
}
