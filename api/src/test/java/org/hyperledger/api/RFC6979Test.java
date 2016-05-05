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
import org.hyperledger.common.HyperLedgerException;
import org.hyperledger.common.ByteUtils;
import org.hyperledger.common.PrivateKey;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;
import java.security.Security;
import java.util.Arrays;

import static org.junit.Assert.assertTrue;

public class RFC6979Test {
    static BouncyCastleProvider provider;

    @BeforeClass
    public static void init() {
        Security.addProvider(provider = new BouncyCastleProvider());
    }

    private static final String TESTS = "RFC6979.json";

    private JSONArray readArray(String resource) throws IOException, JSONException {
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
    @Ignore
    public void rfc6979() throws IOException, JSONException, HyperLedgerException {
        JSONArray tests = readArray(TESTS);
        for (int i = 0; i < tests.length(); ++i) {
            JSONObject test = tests.getJSONObject(i);
            PrivateKey key = PrivateKey.parseWIF(test.getString("key"));
            byte[] message = test.getString("message").getBytes();
            byte[] expectedSignature = ByteUtils.fromHex(test.getString("expectedSignature"));
            byte[] signature = key.sign(message);
            assertTrue(Arrays.equals(expectedSignature, signature));
        }
    }
}
