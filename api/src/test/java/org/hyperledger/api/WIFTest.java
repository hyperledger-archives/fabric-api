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
import org.hyperledger.common.PrivateKey;
import org.json.JSONArray;
import org.json.JSONException;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;
import java.security.Security;

import static org.junit.Assert.assertTrue;

public class WIFTest {
    @BeforeClass
    public static void init() {
        Security.addProvider(new BouncyCastleProvider());
    }

    private static final String WIF = "WIF.json";

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
    public void wifTest() throws IOException, JSONException, HyperLedgerException {
        JSONArray testData = readObjectArray(WIF);
        for (int i = 0; i < testData.length(); ++i) {
            JSONArray test = testData.getJSONArray(i);
            PrivateKey kp = PrivateKey.parseWIF(test.getString(1));
            assertTrue(test.getString(0).equals(kp.getAddress().toString()));
            String serialized = PrivateKey.serializeWIF(kp);
            assertTrue(test.getString(1).equals(serialized));
        }
    }

}
