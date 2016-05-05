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

import org.hyperledger.HyperLedgerSettings;
import org.hyperledger.common.*;

public class ChainSigner implements Signer {
    private final KeyChain keyChain;

    public ChainSigner(KeyChain keyChain) {
        this.keyChain = keyChain;
    }

    public KeyChain getKeyChain() {
        return keyChain;
    }

    public Script getSpendScript(Address addres) throws HyperLedgerException {
        return addres.getAddressScript();
    }

    @Override
    public Coin sign(Coin source, int ix, Transaction transaction) throws HyperLedgerException {
        return sign(source, ix, transaction, HyperLedgerSettings.getInstance().getSignatureOptions());
    }

    @Override
    public Coin sign(Coin source, int ix, Transaction transaction, SignatureOptions signatureOptions) throws HyperLedgerException {
        if (!(source instanceof SignedCoin) || ((SignedCoin) source).getSignature().isEmpty()) {
            // Don't sign native asset issuance marker
            if (source.getOutpoint().isNull())
                return source;
            if (source.getOutput().getScript().isPayToAddress()) {
                Address address = source.getOutput().getOutputAddress();
                PrivateKey key = keyChain.getKeyForAddress(address);
                if (key != null) {
                    return new SignedCoin(source, Script.create()
                            .payToPublicKeyHashSpend(sign(key, transaction, ix, address.getAddressScript(), source.getOutput(), signatureOptions), key.getPublic())
                            .build());
                }
            } else if (source.getOutput().getScript().isPayToKey()) {
                Address address = source.getOutput().getOutputAddress();
                PrivateKey key = keyChain.getKeyForAddress(address);
                if (key != null) {
                    return new SignedCoin(source, Script.create()
                            .payToOnlySigSpend(sign(key, transaction, ix, address.getAddressScript(), source.getOutput(), signatureOptions))
                            .build());
                }
            }
            // If we don't know the script type, we can't sign, assume it's for another signer
        }
        return source;
    }

    protected static byte[] sign(PrivateKey key, Transaction transaction, int ix, Script spendScript, TransactionOutput source, SignatureOptions signatureOptions) throws HyperLedgerException {
        byte[] sig = key.sign(TransactionHasher.hashTransaction(transaction, ix, Script.SIGHASH_ALL, spendScript, signatureOptions,
                source));
        byte[] sigPlusType = new byte[sig.length + 1];
        System.arraycopy(sig, 0, sigPlusType, 0, sig.length);
        sigPlusType[sigPlusType.length - 1] = (byte) (Script.SIGHASH_ALL & 0xff);
        return sigPlusType;
    }
}
