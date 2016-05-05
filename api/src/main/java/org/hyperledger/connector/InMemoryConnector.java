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
package org.hyperledger.connector;

import java.security.SecureRandom;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

class InMemoryConnector implements Connector {

    @Override
    public ConnectorSession createSession() throws ConnectorException {
        return new InMemorySession();
    }

    private final ExecutorService sessionExecutor = Executors.newCachedThreadPool(r -> {
        Thread t = new Thread(r);
        t.setDaemon(true);
        t.setName("InMemoryConnector sessionExecutor");
        return t;
    });
    private final Map<String, Set<InMemoryConsumer>> destinationConsumer = new HashMap<>();

    private static final ConnectorMessage INVALID = new ConnectorMessage() {
        @Override
        public void setPayload(byte[] payload) throws ConnectorException {
        }

        @Override
        public byte[] getPayload() throws ConnectorException {
            return null;
        }

        @Override
        public ConnectorProducer getReplyProducer() throws ConnectorException {
            return null;
        }

        @Override
        public void setReplyTo(ConnectorDestination destination) throws ConnectorException {
        }
    };

    private class InMemoryConsumer implements ConnectorConsumer, Runnable {
        private volatile boolean run = true;
        private ConnectorListener listener;
        private ConnectorDestination destination;
        private LinkedBlockingQueue<ConnectorMessage> queue = new LinkedBlockingQueue<>();

        public InMemoryConsumer(ConnectorDestination destination) {
            synchronized (destinationConsumer) {
                this.destination = destination;
                try {
                    if (!destinationConsumer.containsKey(destination.getName())) {
                        Set<InMemoryConsumer> consumer = new HashSet<>();
                        consumer.add(this);
                        destinationConsumer.put(destination.getName(), consumer);
                    }
                } catch (ConnectorException e) {
                }
            }
        }

        public void putMessage(ConnectorMessage m) throws ConnectorException {
            queue.offer(m);
        }

        @Override
        public void setMessageListener(ConnectorListener listener) throws ConnectorException {
            this.listener = listener;
            sessionExecutor.execute(this);
        }

        @Override
        public ConnectorMessage receive(long timeout) throws ConnectorException {
            if (listener != null) {
                throw new ConnectorException("either listen or receive with consumer");
            }
            try {
                return queue.poll(timeout, TimeUnit.MILLISECONDS);
            } catch (InterruptedException e) {
                return null;
            }
        }

        @Override
        public ConnectorMessage receive() throws ConnectorException {
            if (listener != null) {
                throw new ConnectorException("either listen or receive with consumer");
            }
            try {
                return queue.take();
            } catch (InterruptedException e) {
                return null;
            }
        }

        @Override
        public ConnectorMessage receiveNoWait() throws ConnectorException {
            if (listener != null) {
                throw new ConnectorException("either listen or receive with consumer");
            }
            return queue.poll();
        }

        @Override
        public void close() throws ConnectorException {
            if (run) {
                run = false;
                putMessage(INVALID);
            }
            synchronized (destinationConsumer) {
                if (destinationConsumer.containsKey(destination.getName())) {
                    destinationConsumer.get(destination.getName()).remove(this);
                    if (destinationConsumer.get(destination.getName()).isEmpty()) {
                        destinationConsumer.remove(destination.getName());
                    }
                }
            }
        }

        @Override
        public void run() {
            try {
                while (run || !queue.isEmpty()) {
                    ConnectorMessage m = queue.take();
                    if (m != INVALID) {
                        listener.onMessage(m);
                    }
                }
            } catch (InterruptedException e) {
            }
        }
    }

    private class InMemoryProducer implements ConnectorProducer {
        private final ConnectorDestination destination;

        public InMemoryProducer(ConnectorDestination destination) {
            this.destination = destination;
        }

        @Override
        public void send(ConnectorMessage message) throws ConnectorException {
            synchronized (destinationConsumer) {
                if (destinationConsumer.containsKey(destination.getName())) {
                    for (InMemoryConsumer c : destinationConsumer.get(destination.getName())) {
                        c.putMessage(message);
                    }
                }
            }
        }

        @Override
        public void close() throws ConnectorException {
        }
    }

    private static class InMemoryQueue implements ConnectorQueue {
        private final String name;

        public InMemoryQueue(String name) {
            this.name = name;
        }

        @Override
        public String getName() {
            return name;
        }
    }

    private static class InMemoryTemporaryQueue extends InMemoryQueue implements ConnectorTemporaryQueue {
        public InMemoryTemporaryQueue() {
            super("temp" + String.valueOf(new SecureRandom().nextLong()));
        }

        @Override
        public void delete() {
        }
    }

    private static class InMemoryTopic implements ConnectorTopic {
        private final String name;

        public InMemoryTopic(String name) {
            this.name = name;
        }

        @Override
        public String getName() {
            return name;
        }
    }

    private class InMemorySession implements ConnectorSession {
        private Set<InMemoryConsumer> consumerSet = Collections.synchronizedSet(new HashSet<>());

        public class InMemoryConnectorMessage implements ConnectorMessage {
            private byte[] payload;
            private ConnectorDestination replyTo;

            @Override
            public void setPayload(byte[] payload) {
                this.payload = payload;
            }

            @Override
            public byte[] getPayload() {
                return payload;
            }

            @Override
            public ConnectorProducer getReplyProducer() throws ConnectorException {
                return createProducer(replyTo);
            }

            @Override
            public void setReplyTo(ConnectorDestination replyTo) {
                this.replyTo = replyTo;
            }
        }

        @Override
        public ConnectorMessage createMessage() throws ConnectorException {
            return new InMemoryConnectorMessage();
        }

        @Override
        public ConnectorProducer createProducer(ConnectorDestination destination) throws ConnectorException {
            return new InMemoryProducer(destination);
        }

        @Override
        public ConnectorConsumer createConsumer(ConnectorDestination destination) throws ConnectorException {
            InMemoryConsumer c = new InMemoryConsumer(destination);
            consumerSet.add(c);
            return c;
        }

        @Override
        public ConnectorTemporaryQueue createTemporaryQueue() throws ConnectorException {
            return new InMemoryTemporaryQueue();
        }

        @Override
        public ConnectorQueue createQueue(String name) throws ConnectorException {
            return new InMemoryQueue(name);
        }

        @Override
        public ConnectorTopic createTopic(String name) throws ConnectorException {
            return new InMemoryTopic(name);
        }

        @Override
        public void close() throws ConnectorException {
            for (InMemoryConsumer c : consumerSet) {
                c.close();
            }
        }
    }

    @Override
    public void start() {
    }

    @Override
    public void close() {
        sessionExecutor.shutdown();
    }

    @Override
    public void setClientID(String string) {
    }
}
