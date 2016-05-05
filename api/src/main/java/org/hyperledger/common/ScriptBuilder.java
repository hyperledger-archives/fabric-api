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

import java.util.List;

public class ScriptBuilder {
    private Script.Writer writer = new Script.Writer();

    public ScriptBuilder concat(Script other) {
        writer.writeBytes(other.toByteArray());
        return this;
    }

    public ScriptBuilder rawData(byte[] data) {
        writer.writeBytes(data);
        return this;
    }

    public ScriptBuilder data(byte[] data) {
        if (data.length <= 75) {
            writer.writeByte(data.length);
        } else if (data.length <= 0xff) {
            writer.writeByte(Opcode.OP_PUSHDATA1.o)
                    .writeByte(data.length);
        } else if (data.length <= 0xffff) {
            writer.writeByte(Opcode.OP_PUSHDATA2.o)
                    .writeInt16(data.length);
        } else {
            writer.writeByte(Opcode.OP_PUSHDATA4.o)
                    .writeInt32(data.length);
        }
        writer.writeBytes(data);
        return this;
    }

    public ScriptBuilder op(int index) {
        return op(Opcode.fromIndex(index));
    }

    public ScriptBuilder op(Opcode opcode) {
        return token(new Script.Token(opcode));
    }

    public ScriptBuilder token(Script.Token token) {
        writer.writeByte(token.op.o);
        if (token.data != null) {
            if (token.op.o == Opcode.OP_PUSHDATA1.o) {
                writer.writeByte(token.data.length);
            }
            if (token.op.o == Opcode.OP_PUSHDATA2.o) {
                writer.writeInt16(token.data.length);
            }
            if (token.op.o == Opcode.OP_PUSHDATA4.o) {
                writer.writeInt32(token.data.length);
            }
            writer.writeBytes(token.data);
        }
        return this;
    }

    public ScriptBuilder payTo(Address address) throws HyperLedgerException {
        if (address.getType() == Address.Type.COMMON) {
            op(Opcode.OP_DUP);
            op(Opcode.OP_HASH160);
            data(address.toByteArray());
            op(Opcode.OP_EQUALVERIFY);
            op(Opcode.OP_CHECKSIG);
        } else if (address.getType() == Address.Type.P2SH) {
            op(Opcode.OP_HASH160);
            data(address.toByteArray());
            op(Opcode.OP_EQUAL);
        } else if (address.getType() == Address.Type.P2KEY) {
            throw new HyperLedgerException("Use the LegacyAddressType for pay-to-key addresses");
        } else {
            throw new HyperLedgerException("unknown sink address type");
        }
        return this;
    }

    public ScriptBuilder payToPublicKey(PublicKey pub) {
        data(pub.toByteArray());
        op(Opcode.OP_CHECKSIG);
        return this;
    }

    public ScriptBuilder payToPublicKeyHashSpend(byte[] signature, PublicKey pub) {
        data(signature);
        data(pub.toByteArray());
        return this;
    }

    public ScriptBuilder payToOnlySigSpend(byte[] signature) {
        return data(signature);
    }

    public ScriptBuilder burn() {
        return op(Opcode.OP_RETURN);
    }

    public ScriptBuilder multiSig(List<PublicKey> keys, int signaturesNeeded) {
        op(Opcode.getNumberOp(signaturesNeeded));
        for (Key key : keys) {
            if (key instanceof PrivateKey) {
                key = ((PrivateKey) key).getPublic();
            }
            data(key.toByteArray());
        }
        op(Opcode.getNumberOp(keys.size()));
        op(Opcode.OP_CHECKMULTISIG);
        return this;
    }

    public ScriptBuilder multiSigSpend(List<byte[]> signatures) {
        satoshiBug();
        if (signatures.isEmpty()) {
            return data(new byte[0]);
        }
        signatures.forEach((sig) -> data(sig));
        return this;
    }

    private ScriptBuilder satoshiBug() {
        return data(new byte[0]);
    }

    public ScriptBuilder ifElseBlock(Script onTrue, Script onFalse) {
        op(Opcode.OP_IF);
        concat(onTrue);
        op(Opcode.OP_ELSE);
        concat(onFalse);
        op(Opcode.OP_ENDIF);
        return this;
    }

    public ScriptBuilder blockSignature(int signaturesNeeded, List<PublicKey> keys) {
        int zeroOffset = Opcode.OP_1.ordinal() - 1;

        ScriptBuilder sb = Script.create().op(Opcode.fromIndex(signaturesNeeded + zeroOffset)).op(Opcode.OP_DEPTH).op(Opcode.OP_1SUB).op(Opcode.OP_ROLL).op(Opcode.OP_HASH160);
        for (Key key : keys) {
            if (key instanceof PrivateKey) {
                key = ((PrivateKey) key).getPublic();
            }
            sb.data(key.toByteArray());
        }
        return sb.op(Opcode.fromIndex(keys.size() + zeroOffset)).op(Opcode.OP_CHECKMULTISIGONSTACKVERIFY);
    }

    public Script build() {
        return writer.toScript();
    }
}
