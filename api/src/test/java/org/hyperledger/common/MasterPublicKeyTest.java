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
package org.hyperledger.common;

import com.typesafe.config.ConfigFactory;
import org.bitcoin.Secp256k1Context;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.hyperledger.HyperLedgerSettings;
import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.Security;

import static org.junit.Assert.assertEquals;

public class MasterPublicKeyTest {
    private static final Logger logger = LoggerFactory.getLogger(MasterPublicKeyTest.class);

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
    public void testGetKey() throws HyperLedgerException {
        PublicKey otherKey = new PublicKey(ByteUtils.fromHex("3982F19BEF1615BCCFBB05E321C10E1D4CBA3DF0E841C2E41EEB6016347653C3"), true);
        PublicKey key = new PublicKey(ByteUtils.fromHex("040A629506E1B65CD9D2E0BA9C75DF9C4FED0DB16DC9625ED14397F0AFC836FAE595DC53F8B0EFE61E703075BD9B143BAC75EC0E19F82A2208CAEB32BE53414C40"), true);
        MasterPublicKey masterPublicKey = new MasterPublicKey(key, otherKey.toByteArray(), 0, 0, 0);
        byte[] resultArr = masterPublicKey.getKey(0).toByteArray();
        byte[] resultArr2 = masterPublicKey.getKey(1).toByteArray();
        String sigString = javax.xml.bind.DatatypeConverter.printHexBinary(resultArr);
        assertEquals(sigString, "0324C4464B92BE021979DF0B0BF8B3CDD6F08268100A50B376F9B3A474891BC425");
        sigString = javax.xml.bind.DatatypeConverter.printHexBinary(resultArr2);
        assertEquals(sigString, "031018294F21300B66FD7903266EDD613ABF1385B1264C50C242BFA884B702AA7C");
    }

    @Ignore
    public void testMeasureMasterPublicKey() throws Exception {
        MasterPublicKey key = MasterPrivateKey.createNew().getMasterPublic();
        long start = System.currentTimeMillis();
        int iterations = 100000;
        for (int i = 0; i < iterations; i++) {
            key.getChild(i);
        }

        long elapsed = System.currentTimeMillis() - start;
        System.out.println("public key generation takes " + elapsed / (double)iterations + " ms");
    }
}
