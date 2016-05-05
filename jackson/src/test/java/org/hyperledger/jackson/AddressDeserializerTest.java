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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.hyperledger.account.UIAddress;
import org.hyperledger.common.HyperLedgerException;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class AddressDeserializerTest {

    public static final String TEST_ADDRESS = "13woBeJcGWhnJ2xGETphgBmrrNLJPbYyzX";

    ObjectMapper mapper;

    @Before
    public void setUp() throws Exception {
        mapper = new ObjectMapper();
        mapper.registerModule(new SupernodeModule(() -> true));
    }

    private String quoteAddress() {
        return String.format("\"%s\"", TEST_ADDRESS);
    }

    @Test
    public void testDeserialize() throws Exception {
        mapper.readValue(quoteAddress(), UIAddress.class);
        assertNull(mapper.readValue("\"\"", UIAddress.class));
    }

    @Test
    public void testSerialize() throws HyperLedgerException, JsonProcessingException {
        UIAddress address = UIAddress.fromSatoshiStyle(TEST_ADDRESS);
        assertEquals(quoteAddress(), mapper.writeValueAsString(address));
    }
}
