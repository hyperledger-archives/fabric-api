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
import org.hyperledger.connector.ConnectorQueue;

import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.Queue;
import javax.jms.Session;

public class JMSConnectorQueue implements ConnectorQueue, JMSDestination {
    private Queue queue;

    public JMSConnectorQueue(Session session, String name) throws JMSException {
        queue = session.createQueue(name);
    }

    public Queue getQueue() {
        return queue;
    }

    @Override
    public Destination getDestination() {
        return queue;
    }

    @Override
    public String getName() throws ConnectorException {
        try {
            return queue.getQueueName();
        } catch (JMSException e) {
            throw new ConnectorException(e);
        }
    }
}
