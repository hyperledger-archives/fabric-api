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
package org.hyperledger.api;

import com.typesafe.config.ConfigFactory;
import org.bitcoin.Secp256k1Context;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.hyperledger.HyperLedgerSettings;
import org.hyperledger.common.HyperLedgerException;
import org.hyperledger.common.PrivateKey;
import org.hyperledger.common.PublicKey;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigInteger;
import java.security.SecureRandom;
import java.security.Security;

import static org.junit.Assert.assertEquals;


public class HomomorphicTest {
    private static final Logger logger = LoggerFactory.getLogger(HomomorphicTest.class);

    private static SecureRandom random = new SecureRandom();

    static {
        boolean secpEnabled = Secp256k1Context.isEnabled();
        if (secpEnabled) {
            logger.info("Using native libsecp256k1");
        } else {
            Security.addProvider(new BouncyCastleProvider());
            logger.info("Using java crypto");
        }
        StringBuilder configStr = new StringBuilder("hyperledger{crypto{\n");
        for(String attr: java.util.Arrays.asList(
                "enableNativeCryptoPrivateKeyGetPublic",
                "enableNativeCryptoPrivateKeySign",
                "enableNativeCryptoMasterPublicKeyGenerateKey")) {
            configStr.append(attr).append(": ").append(secpEnabled).append("\n");
        }
        // PublicKey.offsetKey() does not work, so disabling it
        configStr.append("enableNativeCryptoPublicKeyOffset: false").append("}}");
        logger.info(configStr.toString());
        HyperLedgerSettings.initialize(ConfigFactory.load(ConfigFactory.parseString(configStr.toString())));
    }

    @Test
    public void test() throws HyperLedgerException {

        byte[] offset = new byte[32];
        for (int j = 0; j < 10; ++j) {
            PrivateKey key = PrivateKey.createNew();
            for (int i = 0; i < 10; ++i) {
                random.nextBytes(offset);
                BigInteger o = new BigInteger(offset);
                PrivateKey ok = key.offsetKey(o);
                PublicKey pk = key.getPublic().offsetKey(o);
                assertEquals(ok.getPublic(), pk);
                BigInteger no = o.negate();
                assertEquals(key, ok.offsetKey(no));
            }
        }
    }
}
