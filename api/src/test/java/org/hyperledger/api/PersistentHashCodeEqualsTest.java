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

import org.hyperledger.common.*;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

public class PersistentHashCodeEqualsTest {
    @Test
    public void hashtest() {
        assertEquals(0x2163a0b5, new Hash("000000000000000025d9038bcc4ab09a2091981d88354c263bdcd39fb5a06321").hashCode());
        assertEquals(0xdfad5da4, new Hash("00000000000000000acf1be3636034df1fc93017127b2a061a4f6651a45daddf").hashCode());
        assertEquals(-1108838913, Script.create().op(Opcode.OP_RETURN).data(ByteUtils.fromHex("3017127b2a061a4e")).build().hashCode());
        assertEquals(121, Script.create().op(Opcode.OP_10).build().hashCode());
    }

    @Test
    public void equalsTest() {
        BID b1 = new BID("000000000000000025d9038bcc4ab09a2091981d88354c263bdcd39fb5a06321");
        BID b2 = new BID("00000000000000000acf1be3636034df1fc93017127b2a061a4f6651a45daddf");
        assertNotEquals(b1, b2);
        TID t1 = new TID("000000000000000025d9038bcc4ab09a2091981d88354c263bdcd39fb5a06321");
        assertNotEquals(t1, b1);
        TID t2 = new TID("000000000000000025d9038bcc4ab09a2091981d88354c263bdcd39fb5a06321");
        assertEquals(t1, t2);
        assertNotEquals(t1, new TID("00000000000000000acf1be3636034df1fc93017127b2a061a4f6651a45daddf"));
        BID b3 = new BID("00000000000000000acf1be3636034df1fc93017127b2a061a4f6651a45daddf");
        assertEquals(b2, b3);
    }
}
