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

import org.hyperledger.HyperLedgerSettings;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Helper class implementing Bitcoin's hashing algorithm for transaction signatures.
 */
public class TransactionHasher {
    public static byte[] hashTransaction(Transaction t, int inr, int hashType, Script script, SignatureOptions signatureOptions, TransactionOutput source) throws HyperLedgerException {

        Transaction.Builder builder = new Transaction.Builder().version(t.getVersion());

        List<TransactionInput> inputs = new ArrayList<>();
        List<TransactionOutput> outputs = new ArrayList<>();
        for (TransactionOutput o : t.getOutputs())
            outputs.add(o);

        // implicit SIGHASH_ALL
        int i = 0;
        for (TransactionInput in : t.getInputs()) {
            TransactionInput.Builder tbuilder = TransactionInput.create()
                    .source(new Outpoint(in.getSourceTransactionID(), in.getOutputIndex()));
            tbuilder.script(i == inr ? script : new Script());
            tbuilder.sequence(in.getSequence());
            inputs.add(tbuilder.build());
            ++i;
        }

        if ((hashType & 0x1f) == Script.SIGHASH_NONE) {
            outputs.clear();
            i = 0;
            List<TransactionInput> modifiedInputs = new ArrayList<>();
            for (TransactionInput in : inputs) {
                if (i == inr)
                    modifiedInputs.add(inputs.get(i));
                else
                    modifiedInputs.add(
                            TransactionInput.create()
                                    .source(new Outpoint(in.getSourceTransactionID(), in.getOutputIndex()))
                                    .script(in.getScript())
                                    .sequence(0).build()
                    );
                ++i;
            }
            inputs = modifiedInputs;
        } else if ((hashType & 0x1f) == Script.SIGHASH_SINGLE) {
            if (inr >= outputs.size()) {
                // this is a Satoshi client bug.
                // This case should throw an error but it instead retuns 1 that is not checked and interpreted as below
                return ByteUtils.fromHex("0100000000000000000000000000000000000000000000000000000000000000");
            }
            List<TransactionOutput> modifiedOutputs = new ArrayList<>();
            for (i = 0; i < inr; ++i) {
                modifiedOutputs.add(
                        TransactionOutput.create().script(new Script()).value(-1).build()
                );
            }
            modifiedOutputs.add(outputs.get(inr));
            List<TransactionInput> modifiedInputs = new ArrayList<>();
            i = 0;
            for (TransactionInput in : inputs) {
                if (i == inr)
                    modifiedInputs.add(in);
                else
                    modifiedInputs.add(
                            TransactionInput.create()
                                    .source(new Outpoint(in.getSourceTransactionID(), in.getOutputIndex()))
                                    .script(in.getScript())
                                    .sequence(0).build()
                    );
                ++i;
            }
            inputs = modifiedInputs;
            outputs = modifiedOutputs;
        }
        if ((hashType & Script.SIGHASH_ANYONECANPAY) != 0) {
            TransactionInput oneInput = inputs.get(inr);
            inputs.clear();
            inputs.add(oneInput);
        }

        Transaction copy = builder.inputs(inputs).outputs(outputs).build();

        try {
            WireFormat.HashWriter writer = new WireFormat.HashWriter();
            HyperLedgerSettings.getInstance().getTxWireFormatter().toSignature(copy, writer, signatureOptions, source);
            writer.writeBytes(new byte[]{(byte) (hashType & 0xff), 0, 0, 0});
            return writer.hash().unsafeGetArray();
        } catch (IOException e) {
        }
        return null;
    }
}
