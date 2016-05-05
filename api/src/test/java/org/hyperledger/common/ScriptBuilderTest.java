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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.CoreMatchers.containsString;
import static org.junit.Assert.*;

public class ScriptBuilderTest {

    private final ScriptBuilder scriptBuilder = new ScriptBuilder();
    private final ByteArrayOutputStream expected = new ByteArrayOutputStream();


    private static final int PLACEHOLDER_SATOSHI_BUG = 0;
    private static final byte[] DUMMY_KEY = {1, 2, 3, 4, 5, 6, 7, 8, 9};
    private static final byte[] DUMMY_KEY_2 = {0xA, 0xB, 0xC};
    private static final byte[] DUMMY_ADDRESS = {20, 19, 18, 17, 16, 15, 14, 13, 13, 11, 10, 9, 8, 7, 6, 5, 4, 3, 2, 1};

    @Test
    public void payToPublicKeyGeneratesCorrectByteArray() throws IOException {
        scriptBuilder.payToPublicKey(new PublicKey(DUMMY_KEY, false));
        final byte[] result = scriptBuilder.build().toByteArray();

        expected.write(DUMMY_KEY.length);
        expected.write(DUMMY_KEY);
        expected.write(Opcode.OP_CHECKSIG.o);

        assertArrayEquals(expected.toByteArray(), result);
    }

    @Test
    public void payToAddressWillNotAcceptPayToKeyAddress() {
        try {
            scriptBuilder.payTo(new Address(Address.Type.P2KEY, DUMMY_ADDRESS));
            fail("Exception not thrown");
        } catch (HyperLedgerException e) {
            assertThat(e.getMessage(), containsString("pay-to-key address"));
        }
    }

    @Test
    public void multiSigGeneratesCorrectByteArray() throws IOException {
        final List<PublicKey> keys = new ArrayList<>();
        keys.add(new PublicKey(DUMMY_KEY, false));
        keys.add(new PublicKey(DUMMY_KEY_2, false));
        final int signaturesNeeded = 1;

        scriptBuilder.multiSig(keys, signaturesNeeded);
        final byte[] result = scriptBuilder.build().toByteArray();

        expected.write(Opcode.OP_1.o);
        expected.write(DUMMY_KEY.length);
        expected.write(DUMMY_KEY);
        expected.write(DUMMY_KEY_2.length);
        expected.write(DUMMY_KEY_2);
        expected.write(Opcode.OP_2.o);
        expected.write(Opcode.OP_CHECKMULTISIG.o);

        assertArrayEquals(expected.toByteArray(), result);
    }

    @Test
    public void multiSigSpendGeneratesCorrectByteArray() throws IOException {
        final byte[] DUMMY_SIGNATURE = DUMMY_KEY;
        final byte[] DUMMY_SIGNATURE_2 = DUMMY_KEY_2;
        final List<byte[]> signatures = new ArrayList<>();
        signatures.add(DUMMY_SIGNATURE);
        signatures.add(DUMMY_SIGNATURE_2);

        scriptBuilder.multiSigSpend(signatures);
        final byte[] result = scriptBuilder.build().toByteArray();

        expected.write(PLACEHOLDER_SATOSHI_BUG);
        expected.write(DUMMY_SIGNATURE.length);
        expected.write(DUMMY_SIGNATURE);
        expected.write(DUMMY_SIGNATURE_2.length);
        expected.write(DUMMY_SIGNATURE_2);

        assertArrayEquals(expected.toByteArray(), result);
    }

    @Test
    public void multiSigSpendGeneratesCorrectByteArrayForEmptySignatureList() {
        final List<byte[]> signatures = new ArrayList<>();
        scriptBuilder.multiSigSpend(signatures);
        final byte[] result = scriptBuilder.build().toByteArray();

        expected.write(PLACEHOLDER_SATOSHI_BUG);
        expected.write(signatures.size());

        assertArrayEquals(expected.toByteArray(), result);
    }

}
