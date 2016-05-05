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
package org.hyperledger.jackson;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.hyperledger.common.HyperLedgerException;
import org.hyperledger.common.MasterPrivateKey;
import org.hyperledger.common.MasterPublicKey;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

/**
 *
 */
public class MasterKeySerializerTest {
    public static final String TEST_ADDRESS = "13woBeJcGWhnJ2xGETphgBmrrNLJPbYyzX";

    ObjectMapper mapper;
    private MasterPrivateKey KEY;

    @Before
    public void setUp() throws Exception {
        KEY = MasterPrivateKey.createNew();
        mapper = new ObjectMapper();
    }

    @Test
    public void testSerializeProduction() throws Exception {
        mapper.registerModule(new SupernodeModule(() -> true));
        assertThat(mapper.writeValueAsString(KEY)).startsWith("\"xprv");
        assertThat(mapper.writeValueAsString(KEY.getMasterPublic())).startsWith("\"xpub");
        assertThat(mapper.writeValueAsString(null)).isEqualTo("null");
    }

    @Test
    public void testSerializeTestnet() throws Exception {
        mapper.registerModule(new SupernodeModule(() -> false));
        assertThat(mapper.writeValueAsString(KEY)).startsWith("\"tprv");
        assertThat(mapper.writeValueAsString(KEY.getMasterPublic())).startsWith("\"tpub");
        assertThat(mapper.writeValueAsString(null)).isEqualTo("null");
    }

    @Test
    public void testDeserializeTestnet() throws IOException, HyperLedgerException {
        mapper.registerModule(new SupernodeModule(() -> false));
        assertExtendedKeyDeserialization();
    }

    @Test
    public void testDeserializeProduction() throws IOException, HyperLedgerException {
        mapper.registerModule(new SupernodeModule(() -> true));
        assertExtendedKeyDeserialization();
    }

    private void assertExtendedKeyDeserialization() throws IOException, HyperLedgerException {
        assertThat(mapper.readValue(
                "\"xprv9s21ZrQH143K2FfF639rx85pszkdDPgRCAY9uyQLntKW4g4NzxxwGfi48E58pjLBP6wjYqFnKdY1ttBkCN5gnAbHZAzmTjqW9Dso3qsTamX\"",
                MasterPrivateKey.class))
                .isNotNull()
                .isEqualTo(MasterPrivateKey.parse(
                        "xprv9s21ZrQH143K2FfF639rx85pszkdDPgRCAY9uyQLntKW4g4NzxxwGfi48E58pjLBP6wjYqFnKdY1ttBkCN5gnAbHZAzmTjqW9Dso3qsTamX"));

        assertThat(mapper.readValue(
                "\"xpub661MyMwAqRbcEjjiC4gsKG2ZS2b7crQGZPTkiMoxMDrUwUPXYWHBpU2XyXBM1moEKgStqy17WYfskQ53LrwhGBWqu5MU4EBRwWGq1QbgJhU\"",
                MasterPublicKey.class))
                .isNotNull()
                .isEqualTo(MasterPublicKey.parse(
                        "xpub661MyMwAqRbcEjjiC4gsKG2ZS2b7crQGZPTkiMoxMDrUwUPXYWHBpU2XyXBM1moEKgStqy17WYfskQ53LrwhGBWqu5MU4EBRwWGq1QbgJhU"));

        assertThat(mapper.readValue(
                "\"tprv8ZgxMBicQKsPd4tmkc1N7mhpC8AqSuiRXiTGnPpoGroyrGoTzLJgnR5W3QEnq6iVkYUWYvsYUz7pMjjVKaRdbDrt5pD586ZZ4KdDVZ9XgY7\"",
                MasterPrivateKey.class))
                .isNotNull()
                .isEqualTo(MasterPrivateKey.parse(
                        "tprv8ZgxMBicQKsPd4tmkc1N7mhpC8AqSuiRXiTGnPpoGroyrGoTzLJgnR5W3QEnq6iVkYUWYvsYUz7pMjjVKaRdbDrt5pD586ZZ4KdDVZ9XgY7"));

        assertThat(mapper.readValue(
                "\"tpubD6NzVbkrYhZ4WXvZeFfxXBMvm9gmcEuL72444us6h8cNgm4Ecj8GxuhNDZG8YGkU7ayoTYWz3eVvSaZejinw9cy8xg6mjkqfXTsEmM28gUR\"",
                MasterPublicKey.class))
                .isNotNull()
                .isEqualTo(MasterPublicKey.parse(
                        "tpubD6NzVbkrYhZ4WXvZeFfxXBMvm9gmcEuL72444us6h8cNgm4Ecj8GxuhNDZG8YGkU7ayoTYWz3eVvSaZejinw9cy8xg6mjkqfXTsEmM28gUR"));
    }

}
