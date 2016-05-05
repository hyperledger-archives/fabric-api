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

/**
 * Legacy address type, if you really need it...
 */
public class LegacyAddress extends Address {
    private final PublicKey publicKey;

    public LegacyAddress(PublicKey publicKey) throws HyperLedgerException {
        super(Type.P2KEY, Hash.keyHash(publicKey.toByteArray()));
        this.publicKey = publicKey;
    }

    @Override
    public Script getAddressScript() throws HyperLedgerException {
        return publicKey.getP2KeyScript();
    }

    public Address getCommonAddress() throws HyperLedgerException {
        return new Address(Type.COMMON, toByteArray());
    }
}
