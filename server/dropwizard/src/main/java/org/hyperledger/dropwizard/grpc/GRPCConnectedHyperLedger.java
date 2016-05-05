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
package org.hyperledger.dropwizard.grpc;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.typesafe.config.Config;
import org.hyperledger.api.BCSAPI;
import org.hyperledger.connector.GRPCClient;
import org.hyperledger.dropwizard.HyperLedgerConfiguration;
import org.hyperledger.dropwizard.ManagedBCSAPI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GRPCConnectedHyperLedger implements HyperLedgerConfiguration {
    private static final Logger log = LoggerFactory.getLogger(GRPCConnectedHyperLedger.class);
    
    @JsonProperty("grpc")
	//TODO separate out the config
    private Config config;

    @JsonCreator
    public GRPCConnectedHyperLedger(@JsonProperty("grpc") Config config) {
        this.config = config;
    }

	@Override
	public ManagedBCSAPI createBCSAPI() {
		return new ManagedBCSAPI() {
			String host = config.getString("host");
			int port = Integer.parseInt(config.getString("port"));
			int observerPort = Integer.parseInt(config.getString("observerPort"));

            public GRPCClient client = new GRPCClient(host, port, observerPort);

			@Override
			public void start() throws Exception {
				log.info("Connecting to gRPC");
				log.info("host:port={}:{}", host, port);
			}

			@Override
			public void stop() throws Exception {
				log.info("Disconnecting from gRPC");
			}

			@Override
			public BCSAPI getBCSAPI() {
				return client;
			}
		};
	}

}
