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
package org.hyperledger;

import com.typesafe.config.ConfigFactory;
import org.junit.Test;

import static org.junit.Assert.*;

public class HyperLedgerSettingsTest {

    @Test
    public void testTimeout() throws Exception {
        HyperLedgerSettings.initialize(ConfigFactory.load(ConfigFactory.parseString("hyperledger{client{timeout: 1m}}")));
        assertEquals(HyperLedgerSettings.getInstance().getTimeout(), Long.valueOf(60 * 1000).longValue());
        assertFalse(HyperLedgerSettings.getInstance().isCltvEnabled());
    }

    @Test
    public void testDefaultCryptoProviderSettings() throws Exception {
        assertTrue(HyperLedgerSettings.getInstance().isEnableNativeCryptoMasterPublicKeyGenerateKey());
        assertTrue(HyperLedgerSettings.getInstance().isEnableNativeCryptoPrivateKeyGetPublic());
        assertFalse(HyperLedgerSettings.getInstance().isEnableNativeCryptoPublicKeyOffset());
        assertTrue(HyperLedgerSettings.getInstance().isEnableNativeCryptoPrivateKeySign());
    }

    @Test
    public void testValidCryptoProviderSettings() throws Exception {
        HyperLedgerSettings.initialize(ConfigFactory.load(ConfigFactory.parseString("hyperledger{crypto{enableNativeCryptoPrivateKeyGetPublic: false}}")));
        assertFalse(HyperLedgerSettings.getInstance().isEnableNativeCryptoPrivateKeyGetPublic());
        HyperLedgerSettings.initialize(ConfigFactory.load(ConfigFactory.parseString("hyperledger{crypto{enableNativeCryptoPrivateKeyGetPublic: true}}")));
        assertTrue(HyperLedgerSettings.getInstance().isEnableNativeCryptoPrivateKeyGetPublic());
    }
}
