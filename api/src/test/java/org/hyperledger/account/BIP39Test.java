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
package org.hyperledger.account;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.hyperledger.common.*;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;
import java.security.SecureRandom;
import java.security.Security;
import java.util.Arrays;

import static org.junit.Assert.assertTrue;

public class BIP39Test {
    @BeforeClass
    public static void init() {
        Security.addProvider(new BouncyCastleProvider());
    }

    private static final String TESTS = "BIP39.json";

    private JSONObject readObject(String resource) throws IOException, JSONException {
        InputStream input = this.getClass().getResource("/" + resource).openStream();
        StringBuilder content = new StringBuilder();
        byte[] buffer = new byte[1024];
        int len;
        while ((len = input.read(buffer)) > 0) {
            byte[] s = new byte[len];
            System.arraycopy(buffer, 0, s, 0, len);
            content.append(new String(s, "UTF-8"));
        }
        return new JSONObject(content.toString());
    }

    @Test
    public void bip39TrezorTest() throws IOException, JSONException, HyperLedgerException {
        JSONObject testData = readObject(TESTS);
        JSONArray english = testData.getJSONArray("english");
        for (int i = 0; i < testData.length(); ++i) {
            JSONArray test = english.getJSONArray(i);
            String m = Mnemonic.getMnemonic(ByteUtils.fromHex(test.getString(i)));
            assertTrue(m.equals(test.getString(i + 1)));
        }
    }

    @Test
    public void bip39EncodeDecodeTest() throws IOException, JSONException, HyperLedgerException {
        JSONObject testData = readObject(TESTS);
        JSONArray english = testData.getJSONArray("english");
        for (int i = 0; i < testData.length(); ++i) {
            JSONArray test = english.getJSONArray(i);
            byte[] m = Mnemonic.decode(test.getString(1), "HyperLedger");
            assertTrue(test.getString(1).equals(Mnemonic.encode(m, "HyperLedger")));
        }
        SecureRandom random = new SecureRandom();
        for (int i = 0; i < 100; ++i) {
            byte[] secret = new byte[32];
            random.nextBytes(secret);
            String e = Mnemonic.encode(secret, "HyperLedger");
            assertTrue(Arrays.equals(Mnemonic.decode(e, "HyperLedger"), secret));
        }
    }

    @Test
    public void seedTest() throws IOException, JSONException, HyperLedgerException {
        JSONObject testData = readObject(TESTS);
        JSONArray english = testData.getJSONArray("english");
        for (int i = 0; i < testData.length(); ++i) {
            JSONArray test = english.getJSONArray(i);
            String s = ByteUtils.toHex(Mnemonic.getMasterKeySeed(test.getString(1), "TREZOR"));
            String target = test.getString(2);
            assertTrue(target.equals(s));
        }
    }

    @Test
    public void addressTest() throws HyperLedgerException {
        // This is a random TREZOR backtest
        byte[] seed = Mnemonic.getMasterKeySeed("skull kite mother rebel rocket muffin luxury current alley teach write flower", "");
        MasterPrivateKey key = MasterPrivateKey.create(seed);
        // m/0/0'/0'/0/0
        Key k = key.getChild(0).getHardenedChild(0).getHardenedChild(0).getChild(0).getKey(0);
        Address a = k.getAddress();
        assertTrue("moRW5Q9GYArPg3cwpBmdwQeNDcqiwhC6ga".equals(new UIAddress(UIAddress.Network.TEST, a).toString()));
    }
}
