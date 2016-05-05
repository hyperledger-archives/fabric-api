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
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.Security;

import static org.junit.Assert.assertTrue;

public class PrivateKeyTest {
    private static final Logger logger = LoggerFactory.getLogger(PrivateKeyTest.class);
    private PrivateKey key;
    private byte[] data = ByteUtils.fromHex("CF80CD8AED482D5D1527D7DC72FCEFF84E6326592848447D2DC0B0E87DFC9A90");
    private static boolean secpEnabled;

    static {
        secpEnabled = Secp256k1Context.isEnabled();
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

    @Before
    public void setUp() throws HyperLedgerException {
        this.key = PrivateKey.createNew();
    }

    @Test
    public void testSign() throws HyperLedgerException {
        logger.info("-> testSign()");
        byte[] sig = key.sign(data);
        assertTrue(sig != null);
        assertTrue(key.getPublic().verify(data, sig));
        if (secpEnabled) {
            // these are only relevant with native seclib
            testSignPos();
            testSignNeg();
        }
        logger.info("<- testSign()");
    }

    private void testSignPos() throws HyperLedgerException {
        logger.info("-> testSignPos()");
        PrivateKey posKey = new PrivateKey(ByteUtils.fromHex("67E56582298859DDAE725F972992A07C6C4FB9F62A8FFF58CE3CA926A1063530"), true);
        byte[] resultArr = posKey.sign(data);
        String sigString = javax.xml.bind.DatatypeConverter.printHexBinary(resultArr);
        assertTrue(sigString.equals("30440220182A108E1448DC8F1FB467D06A0F3BB8EA0533584CB954EF8DA112F1D60E39A202201C66F36DA211C087F3AF88B50EDF4F9BDAA6CF5FD6817E74DCA34DB12390C6E9"));
        logger.info("<- testSignPos()");
    }

    private void testSignNeg() throws HyperLedgerException {
        logger.info("-> testSignNeg()");
        PrivateKey negKey = new PrivateKey(ByteUtils.fromHex("FFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFF"), true);
        byte[] resultArr = negKey.sign(data);
        String sigString = javax.xml.bind.DatatypeConverter.printHexBinary(resultArr);
        assertTrue(sigString.equals("304402203D022D4E654706C7E70EE859B91732B1648FE718649E16E3EE57977C04166E4302205BFC742D9A6409245F94230D10924FC0CAAB3A446CD0E95B8EFAC06F551DB936"));
        logger.info("<- testSignNeg()");
    }

    @Test
    public void testGetPublic() {
        logger.info("-> testGetPublic()");
        PublicKey publicKey = key.getPublic();
        assertTrue(publicKey != null);
        byte[] sig = key.sign(data);
        assertTrue(sig != null);
        assertTrue(publicKey.verify(data, sig));
        logger.info("<- testGetPublic()");
    }

    @Ignore
    public void testMeasurePrivKey() throws Exception {
        MasterPrivateKey key = MasterPrivateKey.createNew();
        long start = System.currentTimeMillis();
        int iterations = 100000;
        for (int i = 0; i < iterations; i++) {
            key.getChild(i);
        }

        long elapsed = System.currentTimeMillis() - start;
        System.out.println("priv key generation takes " + elapsed / (double)iterations + " ms");
    }

}
