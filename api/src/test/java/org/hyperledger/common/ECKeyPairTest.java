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

import org.bouncycastle.asn1.ASN1Integer;
import org.bouncycastle.asn1.ASN1StreamParser;
import org.bouncycastle.asn1.DERSequence;
import org.junit.Test;

import java.math.BigInteger;

import static org.junit.Assert.assertTrue;

/**
 *
 */
public class ECKeyPairTest {
    public static final String MESSAGE = "hellow world";
    private static final BigInteger HALF_CURVE_ORDER = PrivateKey.curve.getN().shiftRight(1);

    public static boolean isCanonical(BigInteger s) {
        return s.compareTo(HALF_CURVE_ORDER) <= 0;
    }

    @Test
    public void testMalleableSignature() throws Exception {
        for (int i = 0; i < 1000; i++) {
            PrivateKey key = PrivateKey.createNew(true);

            byte[] signature = key.sign(MESSAGE.getBytes());

            ASN1StreamParser asn1 = new ASN1StreamParser(signature);

            DERSequence seq = (DERSequence) asn1.readObject().toASN1Primitive();
            BigInteger s = ((ASN1Integer) seq.getObjectAt(1)).getPositiveValue();

            assertTrue(key.getPublic().verify(MESSAGE.getBytes(), signature));
            assertTrue(String.format("Signature is not canonical for iteration %d key %s", i, key), isCanonical(s));
        }
    }
}
