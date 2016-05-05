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

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.hyperledger.common.*;
import org.hyperledger.common.PrivateKey;
import org.hyperledger.common.PublicKey;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;
import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;
import java.security.*;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class ExtendedKeyTest {
    private final SecureRandom random = new SecureRandom();

    @BeforeClass
    public static void init() {
        Security.addProvider(new BouncyCastleProvider());
    }

    @Test
    public void testGenerator() throws HyperLedgerException {
        MasterPrivateKey ekprivate = MasterPrivateKey.createNew();
        MasterPublicKey ekpublic = ekprivate.getMasterPublic();

        for (int i = 0; i < 20; ++i) {
            PrivateKey fullControl = ekprivate.getKey(i);
            PublicKey readOnly = ekpublic.getKey(i);

            assertEquals(fullControl.getPublic(), readOnly);
            assertTrue(fullControl.getAddress().equals(readOnly.getAddress()));

            byte[] toSign = new byte[100];
            random.nextBytes(toSign);
            byte[] signature = fullControl.sign(toSign);

            assertTrue(readOnly.verify(toSign, signature));
        }
    }

    private static final ThreadMXBean mxb = ManagementFactory.getThreadMXBean();

    private JSONArray readObjectArray(String resource) throws IOException, JSONException {
        InputStream input = this.getClass().getResource("/" + resource).openStream();
        StringBuilder content = new StringBuilder();
        byte[] buffer = new byte[1024];
        int len;
        while ((len = input.read(buffer)) > 0) {
            byte[] s = new byte[len];
            System.arraycopy(buffer, 0, s, 0, len);
            content.append(new String(s, "UTF-8"));
        }
        return new JSONArray(content.toString());
    }

    @Test
    public void bip32Test() throws IOException, JSONException, NoSuchAlgorithmException, NoSuchProviderException, InvalidKeyException, HyperLedgerException {
        JSONArray tests = readObjectArray("BIP32.json");
        for (int i = 0; i < tests.length(); ++i) {
            JSONObject test = tests.getJSONObject(i);
            MasterPrivateKey ekprivate = MasterPrivateKey.create(ByteUtils.fromHex(test.getString("seed")));
            MasterPublicKey ekpublic = ekprivate.getMasterPublic();
            assertTrue(ekprivate.serialize(true).equals(test.get("private")));
            assertTrue(ekpublic.serialize(true).equals(test.get("public")));
            JSONArray derived = test.getJSONArray("derived");
            for (int j = 0; j < derived.length(); ++j) {
                JSONObject derivedTest = derived.getJSONObject(j);
                JSONArray locator = derivedTest.getJSONArray("locator");
                MasterPrivateKey ek = ekprivate;
                MasterPublicKey ep = ekpublic;
                for (int k = 0; k < locator.length(); ++k) {
                    JSONObject c = locator.getJSONObject(k);
                    if (!c.getBoolean("private")) {
                        ek = ek.getChild(c.getInt("sequence"));
                    } else {
                        ek = ek.getHardenedChild(c.getInt("sequence"));
                    }
                    ep = ek.getMasterPublic();
                }
                assertTrue(ek.serialize(true).equals(derivedTest.getString("private")));
                assertTrue(ep.serialize(true).equals(derivedTest.getString("public")));
            }
        }
    }

    @Test
    public void bip32PassphraseTest() throws HyperLedgerException, JSONException, IOException {
        JSONArray tests = readObjectArray("PassphraseKey.json");
        for (int i = 0; i < tests.length(); ++i) {
            JSONObject test = tests.getJSONObject(i);
            MasterKey key = MasterPrivateKey.createFromEncryptedSeed(test.getString("passphrase"), ByteUtils.fromHex(test.getString("seed")));
            assertTrue(key.serialize(true).equals(test.get("key")));
        }
    }

    @Test
    public void testECDSASpeed() throws HyperLedgerException {
        PrivateKey key = PrivateKey.createNew(true);
        byte[] data = new byte[32];
        random.nextBytes(data);
        byte[] signature = key.sign(data);
        long cpu = -mxb.getCurrentThreadUserTime();
        for (int i = 0; i < 100; ++i) {
            assertTrue(key.getPublic().verify(data, signature));
        }
        cpu += mxb.getCurrentThreadUserTime();
        double speed = 100.0 / (cpu / 10.0e9);
        System.out.println("ECDSA validation speed : " + speed + " signatures/second");
        assertTrue(speed > 100.0);
    }
}
