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
import java.util.Collections;
import java.util.List;

public class TransactionProposal {
    private final List<Coin> sources;
    private final List<TransactionOutput> sinks;

    public TransactionProposal(List<Coin> sources, List<TransactionOutput> sinks) {
        this.sinks = sinks;
        this.sources = sources;
    }

    public TransactionProposal(CoinBucket coins, Transaction transaction) throws HyperLedgerException {
        sources = new ArrayList<>();
        sinks = new ArrayList<>();
        for (TransactionInput input : transaction.getInputs()) {
            Coin coin;

            if (input.getSource().isNull()) {
                coin = new Coin(input.getSource(), null); // Native asset issuance
            } else {
                coin = coins.getCoin(input.getSource());
            }

            // Check only if not native asset issuance
            if (coin == null) {
                throw new HyperLedgerException("The transaction refers to a coin not in first argument " + input.getSource());
            }
            if (!input.getScript().isEmpty()) {
                coin = new SignedCoin(coin, input.getScript());
            }
            sources.add(coin);
        }
        for (TransactionOutput output : transaction.getOutputs()) {
            sinks.add(output);
        }
    }

    public List<Coin> getSources() {
        return Collections.unmodifiableList(sources);
    }

    public CoinBucket getSourceCoins() {
        return new CoinBucket(sources);
    }

    public Coin getSource(int ix) {
        return sources.get(ix);
    }

    public List<TransactionOutput> getSinks() {
        return Collections.unmodifiableList(sinks);
    }

    public TransactionOutput getSink(int ix) {
        return sinks.get(ix);
    }

    public Transaction getTransaction() {
        Transaction.Builder builder = Transaction.create();
        builder.outputs(sinks);
        for (Coin c : sources) {
            if (c instanceof SignedCoin) {
                builder = builder.inputs(TransactionInput.create().source(c.getOutpoint())
                        .script(((SignedCoin) c).getSignature()).build());
            } else {
                builder = builder.inputs(TransactionInput.create().source(c.getOutpoint()).build());
            }
        }
        return builder.build();
    }

    public void addSignatures(TransactionProposal other) throws HyperLedgerException {
        addSignatures(other.getTransaction());
    }

    public void addSignatures(Transaction transaction) throws HyperLedgerException {
        if (!sinks.equals(transaction.getOutputs())) {
            throw new HyperLedgerException("Can not merge signatures of different transaction outputs");
        }
        if (sources.size() != transaction.getInputs().size()) {
            throw new HyperLedgerException("Can not merge signatures for different inputs");
        }
        for (int i = 0; i < sources.size(); ++i) {
            if (!sources.get(i).getOutpoint().equals(transaction.getSource(i))) {
                throw new HyperLedgerException("Can not merge signatures for different inputs");
            }
        }
        for (int i = 0; i < sources.size(); ++i) {
            Coin c = sources.get(i);
            Script ts = transaction.getInput(i).getScript();
            if (!ts.isEmpty()) {
                if (c.getOutput().getScript().isPayToScriptHash()) {
                    if (sources.get(i) instanceof SignedCoin) {
                        ScriptBuilder builder = new ScriptBuilder();
                        List<Script.Token> tst = ts.parse();
                        List<Script.Token> pst = ((SignedCoin) sources.get(i)).getSignature().parse();
                        if (tst.size() != pst.size()) {
                            throw new HyperLedgerException("Incompatible multi-sig signatures");
                        }
                        if (tst.size() < 1) {
                            throw new HyperLedgerException("Missing redemption script");
                        }
                        Script tt = new Script(tst.get(tst.size() - 1).data);
                        Script pt = new Script(pst.get(pst.size() - 1).data);
                        if (!tt.equals(pt) || !c.getOutput().getOutputAddress().equals(
                                new Address(Address.Type.P2SH, Hash.keyHash(tt.toByteArray())))) {
                            throw new HyperLedgerException("Incompatible multi-sigs");
                        }
                        for (int j = 0; j < tst.size() - 1; ++j) {
                            if (tst.get(j).data != null) {
                                builder.data(tst.get(j).data);
                            } else {
                                builder.data(pst.get(j).data);
                            }
                        }
                        builder.data(pt.toByteArray());
                        sources.set(i, new SignedCoin(c, builder.build()));
                    } else {
                        sources.set(i, new SignedCoin(c, ts));
                    }
                } else {
                    sources.set(i, new SignedCoin(c, ts));
                }
            }
        }
    }

    public Transaction sign(Account account) throws HyperLedgerException {
        return sign(account.getChain());
    }

    public Transaction sign(KeyChain signingChain) throws HyperLedgerException {
        return sign(signingChain.getSigner());
    }

    public void partialSign(KeyChain signingChain) throws HyperLedgerException {
        partialSign(signingChain.getSigner());
    }

    public void partialSign(Account account) throws HyperLedgerException {
        partialSign(account.getChain());
    }

    public synchronized void partialSign(Signer signer) throws HyperLedgerException {
        partialSign(signer, HyperLedgerSettings.getInstance().getSignatureOptions());
    }

    public synchronized void partialSign(Signer signer, SignatureOptions signatureOptions) throws HyperLedgerException {
        Transaction.Builder builder = Transaction.create().outputs(sinks);
        for (Coin s : sources) {
            builder.inputs(TransactionInput.create().source(s.getOutpoint()).build());
        }
        Transaction transaction = builder.build();
        for (int j = 0; j < sources.size(); ++j) {
            sources.set(j, signer.sign(sources.get(j), j, transaction, signatureOptions));
        }
    }

    public Transaction sign(Signer signer) throws HyperLedgerException {
        return sign(signer, HyperLedgerSettings.getInstance().getSignatureOptions());
    }

    public Transaction sign(Signer signer, SignatureOptions signatureOptions) throws HyperLedgerException {
        partialSign(signer, signatureOptions);
        return getTransaction();
    }

    public long getFee() {
        long fee = 0;
        for (Coin c : sources) {
            if (!c.getOutpoint().isNull())
                fee += c.getOutput().getValue();
        }
        for (TransactionOutput o : sinks) {
            fee -= o.getValue();
        }
        return fee;
    }

    @Override
    public String toString() {
        return "TransactionProposal{" +
                "sources=" + sources +
                ", sinks=" + sinks +
                ", fee= " + getFee() +
                '}';
    }
}
