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

import org.bouncycastle.asn1.sec.SECNamedCurves;
import org.bouncycastle.asn1.x9.X9ECParameters;
import org.bouncycastle.crypto.params.ECDomainParameters;

import java.math.BigInteger;

/**
 * An ECC public or private key
 */
public interface Key {
    X9ECParameters curve = SECNamedCurves.getByName("secp256k1");
    ECDomainParameters domain = new ECDomainParameters(curve.getCurve(), curve.getG(), curve.getN(), curve.getH());

    /**
     * The Address of a key is useful to link transactions. It is derived from the public key.
     *
     * @return the address accessible with the key
     */
    Address getAddress();

    /**
     * Erlier Bitcoin implementations used uncompressed representation of public key, that lead to
     * a different address for the same key.
     *
     * @return true if compressed, that is modern. Only use uncompressed keys for backward compatibility.
     */
    boolean isCompressed();

    /**
     * Safe access to the key's internal representation
     *
     * @return a copy of the key's internal representation
     */
    byte[] toByteArray();

    /**
     * Return a key computable for this key with an offset.
     * Due to a homomorphic property of EC one may compute the offset key of
     * both private and public keys independently such that they build a valid new pair.
     *
     * @param offset
     * @return a key derived of this with an offset.
     * @throws HyperLedgerException
     */
    Key offsetKey(BigInteger offset) throws HyperLedgerException;
}
