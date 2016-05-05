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


import org.junit.Test;

import static org.junit.Assert.*;

public class OpcodeTest {

    @Test
    public void isNumberOpTest() {
        assertTrue(Opcode.OP_1.isNumberOp());
        assertTrue(Opcode.OP_16.isNumberOp());
        assertFalse(Opcode.OP_RESERVED.isNumberOp());
        assertFalse(Opcode.OP_NOP.isNumberOp());
        assertFalse(Opcode.OP_FALSE.isNumberOp());
        assertFalse(Opcode.OP_NOP10.isNumberOp());
    }

    @Test
    public void getNumberOpTest() {
        assertEquals(Opcode.getNumberOp(1), Opcode.OP_1);
        assertEquals(Opcode.getNumberOp(16), Opcode.OP_16);
        assertEquals(Opcode.getNumberOp(-1), Opcode.OP_FALSE);
    }

    @Test
    public void getOpNumberTest() {
        assertEquals(Opcode.OP_1.getOpNumber(), 1);
        assertEquals(Opcode.OP_16.getOpNumber(), 16);
        assertEquals(Opcode.OP_RESERVED.getOpNumber(), -1);
        assertEquals(Opcode.OP_NOP.getOpNumber(), -1);
    }

}
