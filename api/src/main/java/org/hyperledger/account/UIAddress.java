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
package org.hyperledger.account;

import org.hyperledger.common.Address;
import org.hyperledger.common.HyperLedgerException;
import org.hyperledger.common.ByteUtils;
import org.hyperledger.common.Hash;

public class UIAddress {
    private final Address address;

    /**
     * Enumeration of networks. This is used to format addresses to a human readable (BASE58) format with a network specific prefix
     * <p>
     * <pre>
     * UNKNOWN - not defined network
     * PRODUCTION - Bitcoin main network
     * TEST - testnet3 Bitcoin test network
     * </pre>
     */
    public enum Network {
        UNKNOWN, PRODUCTION, TEST
    }

    private final Network network;

    public UIAddress(Network network, Address address) throws HyperLedgerException {
        this.address = address;
        this.network = network;
    }

    public UIAddress(Network network, Address.Type type, byte[] address) throws HyperLedgerException {
        this.address = new Address(type, address);
        this.network = network;
    }

    public Address getAddress() {
        return address;
    }

    /**
     * Convert a human readable address string to an address object
     * This conversion supports COMMON and P2SH address types only.
     *
     * @param address - the human readable address
     * @return an address object with type and network ad encoded in the readable string
     * @throws HyperLedgerException - for malformed address strings
     */
    public static UIAddress fromSatoshiStyle(String address) throws HyperLedgerException {
        try {
            Network network = Network.PRODUCTION;
            Address.Type type = Address.Type.COMMON;
            byte[] raw = ByteUtils.fromBase58(address);
            if ((raw[0] & 0xff) == 0x0) {
                network = Network.PRODUCTION;
                type = Address.Type.COMMON;
            }
            if ((raw[0] & 0xff) == 5) {
                network = Network.PRODUCTION;
                type = Address.Type.P2SH;
            }
            if ((raw[0] & 0xff) == 0x6f) {
                network = Network.TEST;
                type = Address.Type.COMMON;
            }
            if ((raw[0] & 0xff) == 196) {
                network = Network.TEST;
                type = Address.Type.P2SH;
            }
            byte[] check = Hash.hash(raw, 0, raw.length - 4);
            for (int i = 0; i < 4; ++i) {
                if (check[i] != raw[raw.length - 4 + i]) {
                    throw new HyperLedgerException("Address checksum mismatch");
                }
            }
            byte[] keyDigest = new byte[raw.length - 5];
            System.arraycopy(raw, 1, keyDigest, 0, raw.length - 5);
            return new UIAddress(network, type, keyDigest);
        } catch (Exception e) {
            throw new HyperLedgerException(e);
        }
    }

    /**
     * @return Human readable serialization of a COMMON or P2SH address that uses network conventions.
     */
    public String toString() {
        byte[] keyDigest = address.toByteArray();
        int addressFlag;
        if (network == Network.TEST) {
            if (address.getType() == Address.Type.COMMON) {
                addressFlag = 0x6f;
            } else if (address.getType() == Address.Type.P2SH) {
                addressFlag = 196;
            } else {
                return address.getType().name() + ":" + ByteUtils.toHex(address.toByteArray());
            }
        } else {
            if (address.getType() == Address.Type.COMMON) {
                addressFlag = 0x0;
            } else if (address.getType() == Address.Type.P2SH) {
                addressFlag = 0x5;
            } else {
                return address.getType().name() + ":" + ByteUtils.toHex(address.toByteArray());
            }
        }
        byte[] addressBytes = new byte[1 + keyDigest.length + 4];
        addressBytes[0] = (byte) (addressFlag & 0xff);
        System.arraycopy(keyDigest, 0, addressBytes, 1, keyDigest.length);
        byte[] check = Hash.hash(addressBytes, 0, keyDigest.length + 1);
        System.arraycopy(check, 0, addressBytes, keyDigest.length + 1, 4);
        return ByteUtils.toBase58(addressBytes);
    }
}
