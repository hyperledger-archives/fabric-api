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

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.hyperledger.account.color.ColoredBaseTransactionFactory;
import org.hyperledger.api.APITransaction;
import org.hyperledger.common.*;
import org.hyperledger.common.color.Color;
import org.hyperledger.common.color.ColoredTransactionOutput;
import org.hyperledger.common.color.ForeignAsset;
import org.junit.Test;

import java.security.Security;
import java.util.List;

import static org.junit.Assert.assertTrue;

public class TransactionSignatureTest {
    static {
        Security.addProvider(new BouncyCastleProvider());
    }

    @Test
    public void signatureTest() throws HyperLedgerException {
        PrivateKey k1 = PrivateKey.createNew();
        TransactionWithSource signed = getSignedTransaction(k1, SignatureOptions.COMMON);

        byte[] signedHash = TransactionHasher.hashTransaction(signed.transaction, 0, Script.SIGHASH_ALL, k1.getAddress().getAddressScript(),
                SignatureOptions.COMMON, signed.source);
        List<Script.Token> tokens = signed.transaction.getInput(0).getScript().parse();
        byte[] signature = tokens.get(0).data;
        assertTrue(k1.getPublic().verify(signedHash, signature));
    }

    @Test
    public void signatureTestWithSCIV() throws HyperLedgerException {
        PrivateKey k1 = PrivateKey.createNew();
        TransactionWithSource signed = getSignedTransaction(k1, SignatureOptions.SCIV);

        byte[] signedHash = TransactionHasher.hashTransaction(signed.transaction, 0, Script.SIGHASH_ALL, k1.getAddress().getAddressScript(),
                SignatureOptions.SCIV, signed.source);
        List<Script.Token> tokens = signed.transaction.getInput(0).getScript().parse();
        byte[] signature = tokens.get(0).data;
        assertTrue(k1.getPublic().verify(signedHash, signature));
    }

    @Test
    public void signatureTestWithSCIC() throws HyperLedgerException {
        PrivateKey k1 = PrivateKey.createNew();
        TransactionWithSource signed = getSignedTransaction(k1, SignatureOptions.SCIC);
        byte[] signedHash = TransactionHasher.hashTransaction(signed.transaction, 0, Script.SIGHASH_ALL, k1.getAddress().getAddressScript(),
                SignatureOptions.SCIC, signed.source);
        List<Script.Token> tokens = signed.transaction.getInput(0).getScript().parse();
        byte[] signature = tokens.get(0).data;
        assertTrue(k1.getPublic().verify(signedHash, signature));
    }

    private static class TransactionWithSource {
        public final Transaction transaction;
        public final int ix;
        public final TransactionOutput source;

        public TransactionWithSource(Transaction transaction, int ix, TransactionOutput source) {
            this.transaction = transaction;
            this.ix = ix;
            this.source = source;
        }
    }

    public TransactionWithSource getSignedTransaction(PrivateKey k1, SignatureOptions signatureOptions) throws HyperLedgerException {
        if (signatureOptions.contains(SignatureOptions.Option.SCIC)) {
            Account account = new BaseAccount(new KeyListChain(k1));
            Color color = new ForeignAsset(PrivateKey.createNew().getAddress());
            ColoredTransactionOutput coloredOutput = new ColoredTransactionOutput(1000, k1.getAddress().getAddressScript(), color, 100);
            Transaction source = Transaction.create()
                    .inputs(TransactionInput.create().source(TID.INVALID, 0).build())
                    .outputs(coloredOutput).build();

            account.process(new APITransaction(source, BID.INVALID));

            ColoredBaseTransactionFactory transactionFactory = new ColoredBaseTransactionFactory(account);
            Transaction signed = transactionFactory.proposeColored(PrivateKey.createNew().getAddress(), color, 100L)
                    .sign(account.getChain().getSigner(), signatureOptions);
            return new TransactionWithSource(signed, 0, coloredOutput);
        } else {
            Account account = new BaseAccount(new KeyListChain(k1));
            Transaction source = Transaction.create()
                    .inputs(TransactionInput.create().source(TID.INVALID, 0).build())
                    .outputs(TransactionOutput.create().payTo(k1.getAddress()).value(50000000L).build()).build();

            account.process(new APITransaction(source, BID.INVALID));

            BaseTransactionFactory transactionFactory = new BaseTransactionFactory(account);
            Transaction signed = transactionFactory.propose(PrivateKey.createNew().getAddress(), 50000000L,
                    PaymentOptions.create().fee(0).feeCalculation(PaymentOptions.FeeCalculation.FIXED).build())
                    .sign(account.getChain().getSigner(), signatureOptions);
            return new TransactionWithSource(signed, 0, source.getOutput(0));
        }
    }
}
