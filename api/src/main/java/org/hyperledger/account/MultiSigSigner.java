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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

public class MultiSigSigner extends ChainSigner {
    private final int signaturesNeeded;

    public MultiSigSigner(int signaturesNeeded, MultiSigKeyChain keyChain) {
        super(keyChain);
        this.signaturesNeeded = signaturesNeeded;
    }

    @Override
    public MultiSigKeyChain getKeyChain() {
        return (MultiSigKeyChain) super.getKeyChain();
    }

    public Script getSpendScript(List<Key> keys) {
        List<PublicKey> publicKeys = keys.stream()
                .map((k) -> (k instanceof PrivateKey ? ((PrivateKey) k).getPublic() : (PublicKey) k))
                .collect(Collectors.toList());
        return Script.create().multiSig(publicKeys, signaturesNeeded).build();
    }

    @Override
    public Script getSpendScript(Address address) throws HyperLedgerException {
        return getSpendScript(getKeyChain().getKeysForAddress(address));
    }

    @Override
    public Coin sign(Coin source, int ix, Transaction transaction) throws HyperLedgerException {
        return sign(source, ix, transaction, HyperLedgerSettings.getInstance().getSignatureOptions());
    }

    @Override
    public Coin sign(Coin source, int ix, Transaction transaction, SignatureOptions signatureOptions) throws HyperLedgerException {
        if (source.getOutput().getScript().isPayToScriptHash()) {
            Address address = source.getOutput().getOutputAddress();
            List<Key> keys = getKeyChain().getKeysForAddress(address);
            Iterator<Script.Token> signatures = getSourceSignatures(source);

            ScriptBuilder builder = Script.create();
            builder.multiSigSpend(calculateSignatures(keys, signatures, ix, transaction, address, source.getOutput(), signatureOptions));
            return new SignedCoin(source, builder.data(getSpendScript(address).toByteArray()).build());
        }
        return source;
    }

    private Iterator<Script.Token> getSourceSignatures(Coin source) throws HyperLedgerException {
        Iterator<Script.Token> signatures;
        if (source instanceof SignedCoin) {
            signatures = ((SignedCoin) source).getSignature().parse().iterator();
        } else {
            signatures = new ArrayList<Script.Token>().iterator();
        }
        if (signatures.hasNext()) {
            signatures.next();
        }
        return signatures;
    }

    private List<byte[]> calculateSignatures(List<Key> keys, Iterator<Script.Token> signatures, int ix,
                                             Transaction transaction, Address address, TransactionOutput source, SignatureOptions signatureOptions) throws HyperLedgerException {
        List<byte[]> byteArraySigs = new LinkedList<>();
        for (Key key : keys) {
            byte[] sig = new byte[0];
            Script.Token next = signatures.hasNext() ? signatures.next() : null;
            if (key instanceof PrivateKey) {
                sig = sign((PrivateKey) key, transaction, ix, getSpendScript(address), source, signatureOptions);
            } else if (signatures.hasNext()) {
                sig = next.data;
            }
            byteArraySigs.add(sig);
        }
        return byteArraySigs;
    }
}
