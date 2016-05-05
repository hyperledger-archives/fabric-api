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
package org.hyperledger.common.color;

import org.hyperledger.common.*;
import org.junit.Before;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import static org.hyperledger.common.color.Color.BITCOIN;
import static org.junit.Assert.*;

public class ColorTest {
    private NativeAsset nativeAsset;
    private ForeignAsset foreignAsset;
    private Address address;
    private NativeAsset newNativeAsset;
    private Transaction tx;

    @Before
    public void setUp() throws Exception {
        tx = Transaction.create().build();
        address = PrivateKey.createNew().getAddress();
        nativeAsset = new NativeAsset(tx.getID(), 123);
        newNativeAsset = new NativeAsset(123);
        foreignAsset = new ForeignAsset(address);
    }

    @Test
    public void testNativeGetEncoded() {
        assertEquals(nativeAsset, Color.fromEncoded(nativeAsset.getEncoded()));
    }

    @Test
    public void testForeignGetEncoded() {
        assertEquals(foreignAsset, Color.fromEncoded(foreignAsset.getEncoded()));
    }

    @Test
    public void testBitcoinGetEncoded() {
        assertEquals(BITCOIN, Color.fromEncoded(BITCOIN.getEncoded()));
    }

    @Test
    public void tstNewNativeGetEncoded() {
        assertEquals(newNativeAsset, Color.fromEncoded(newNativeAsset.getEncoded()));
        assertEquals(newNativeAsset.toCanonical(tx), nativeAsset);
        assertEquals(nativeAsset.toCanonical(tx), nativeAsset);
        assertTrue(newNativeAsset.isBeingDefined());
        assertFalse(nativeAsset.isBeingDefined());
    }

    @Test
    public void testSerializeOutput() throws HyperLedgerException, IOException {
        ColoredTransactionOutput output = ColoredTransactionOutput.create().payTo(address).value(1234).build();
        ColoredTransactionOutput restore = roundTrip(output);
        assertEquals(restore.getColor(), BITCOIN);
        assertEquals(restore.getQuantity(), 0);
        assertEquals(restore.getValue(), 1234);
    }

    @Test
    public void testSerializeOutput1() throws HyperLedgerException, IOException {
        TransactionOutput output = TransactionOutput.create().payTo(address).value(1234).build();
        ColoredTransactionOutput restore = roundTrip(output);
        assertEquals(restore.getColor(), BITCOIN);
        assertEquals(restore.getQuantity(), 0);
        assertEquals(restore.getValue(), 1234);
    }

    @Test
    public void testSerializeColoredOutput() throws HyperLedgerException, IOException {
        ColoredTransactionOutput output = ColoredTransactionOutput.create().payTo(address).color(nativeAsset).quantity(1234).build();
        ColoredTransactionOutput restore = roundTrip(output);
        assertEquals(restore.getColor(), nativeAsset);
        assertEquals(restore.getQuantity(), 1234);
        assertEquals(restore.getValue(), 0);
    }

    public ColoredTransactionOutput roundTrip(TransactionOutput bitcoinOutput) throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        WireFormat.Writer writer = new WireFormat.Writer(bos);
        bitcoinOutput.toWireNativeAsset(writer);
        WireFormat.Reader reader = new WireFormat.Reader(bos.toByteArray());
        return ColoredTransactionOutput.fromWireNativeAsset(reader);
    }
}
