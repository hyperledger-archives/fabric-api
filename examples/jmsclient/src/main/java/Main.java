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
import org.hyperledger.account.AddressListChain;
import org.hyperledger.account.BaseReadOnlyAccount;
import org.hyperledger.account.ReadOnlyAccount;
import org.hyperledger.api.BCSAPIException;
import org.hyperledger.common.HyperLedgerException;
import org.hyperledger.common.ByteUtils;
import org.hyperledger.common.LegacyAddress;
import org.hyperledger.common.PublicKey;
import org.hyperledger.connector.BCSAPIClient;
import org.hyperledger.jms.JMSConnectorFactory;

/**
 * Example how to connect to a HyperLedger JMS server.
 */
public class Main {

    public void run() {

        try {
            // change parameters matching your configuration
            JMSConnectorFactory factory = new JMSConnectorFactory("application", "somepassword", "tcp://hal:61616");
            BCSAPIClient api = new BCSAPIClient(factory);
            api.init();

            api.ping(100);

            PublicKey pk = new PublicKey(ByteUtils.fromHex("04678afdb0fe5548271967f1a67130b7105cd6a828e03909a67962e0ea1f61deb649f6bc3f4cef38c4f35504e51ec112de5c384df7ba0b8d578a4c702b6bf11d5f"), false);

            LegacyAddress satoshi = new LegacyAddress(pk);

            ReadOnlyAccount satoshistash = new BaseReadOnlyAccount(new AddressListChain(satoshi, satoshi.getCommonAddress()));
            satoshistash.sync(api);
            System.out.println("There are " + satoshistash.getConfirmedCoins().getTotalSatoshis() +
                    " satoshis on the genesis address (" + satoshi.getCommonAddress() + ").");

            System.exit(0);
        } catch (BCSAPIException e) {
            e.printStackTrace();
        } catch (HyperLedgerException e) {
            e.printStackTrace();
        }
    }


    public static void main(String[] args) {
        new Main().run();
    }
}
