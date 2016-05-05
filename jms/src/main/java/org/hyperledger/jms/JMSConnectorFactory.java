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

import com.typesafe.config.Config;
import org.apache.activemq.ActiveMQConnectionFactory;
import org.hyperledger.connector.Connector;
import org.hyperledger.connector.ConnectorException;
import org.hyperledger.connector.ConnectorFactory;

import javax.jms.JMSException;

public class JMSConnectorFactory implements ConnectorFactory {
    public static final String CONFIG_PATH = "hyperledger.connector.jms";
    private String username;
    private String password;
    private String brokerUrl;
    private String clientId;

    public static boolean isEnabled(Config config) {
        return config.hasPath(CONFIG_PATH) &&
                !config.getConfig(CONFIG_PATH).isEmpty();
    }

    public static JMSConnectorFactory fromConfig(Config fullConfig) {
        Config config = fullConfig.getConfig(CONFIG_PATH);
        String clientId = null;
        if (config.hasPath("clientId"))
            clientId = config.getString("clientId");

        return new JMSConnectorFactory(
                config.getString("username"),
                config.getString("password"),
                config.getString("brokerUrl"),
                clientId
        );
    }

    public JMSConnectorFactory(String username, String password, String brokerUrl) {
        this.username = username;
        this.password = password;
        this.brokerUrl = brokerUrl;
    }

    public JMSConnectorFactory(String username, String password, String brokerUrl, String clientId) {
        this.username = username;
        this.password = password;
        this.brokerUrl = brokerUrl;
        this.clientId = clientId;
    }

    @Override
    public Connector getConnector() throws ConnectorException {
        ActiveMQConnectionFactory connectionFactory = new ActiveMQConnectionFactory(username, password, brokerUrl);
        if (clientId != null) {
            connectionFactory.setClientID(clientId);
        }
        try {
            return new JMSConnector(connectionFactory);
        } catch (JMSException e) {
            throw new ConnectorException(e);
        }
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getBrokerUrl() {
        return brokerUrl;
    }

    public void setBrokerUrl(String brokerUrl) {
        this.brokerUrl = brokerUrl;
    }

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }
}
