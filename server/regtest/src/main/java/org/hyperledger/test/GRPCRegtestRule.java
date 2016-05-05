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
package org.hyperledger.test;

import akka.actor.ActorSystem;
import com.typesafe.config.Config;
import org.hyperledger.api.BCSAPI;
import org.hyperledger.common.HyperLedgerException;
import org.hyperledger.connector.GRPCClient;
import org.hyperledger.core.conf.CoreAssembly;
import org.hyperledger.core.conf.CoreAssemblyFactory;
import org.hyperledger.main.BCSAPIServer;
import org.hyperledger.main.Main;
import org.hyperledger.network.HyperLedgerExtension;
import org.junit.rules.ExternalResource;

public class GRPCRegtestRule extends ExternalResource {
    private GRPCClient bcsapi;
    private ActorSystem system;
    private BCSAPIServer server;
    private final Config config;

    public GRPCRegtestRule(Config config) {
        this.config = config;
    }

    public void start() throws HyperLedgerException {
        system = ActorSystem.create("GRPCConnectedHyperLedger", config);
        server = Main.createAndStartServer(system);
        String host = config.getString("gRPCConnectedHyperLedger.grpc.host");
        int port = config.getInt("gRPCConnectedHyperLedger.grpc.port");
        int observerPort = config.getInt("gRPCConnectedHyperLedger.grpc.observerPort");

        bcsapi = new GRPCClient(host, port, observerPort);
        CoreAssembly coreAssembly = HyperLedgerExtension.get(system).coreAssembly();
    }

    public BCSAPI getBCSAPI() {
        return bcsapi;
    }

    public void stop() {
        server.destroy();
        system.shutdown();
        system.awaitTermination();
        CoreAssemblyFactory.reset();
    }

    @Override
    protected void after() {
        stop();
    }

    @Override
    protected void before() throws Throwable {
        start();
    }
}
