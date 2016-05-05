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

import org.hyperledger.common.color.DigitalAssetAnnotation;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.StringTokenizer;

/**
 * A script in the TransactionInput or TransactionOutput
 *
 * @see TransactionInput
 * @see TransactionOutput
 */
public class Script {
    private final byte[] bytes;

    public Script() {
        this.bytes = new byte[0];
    }

    public Script(byte[] bytes) {
        this.bytes = Arrays.copyOf(bytes, bytes.length);
    }

    public int size() {
        return bytes.length;
    }

    public void toWire(WireFormat.Writer writer) throws IOException {
        writer.writeVarBytes(bytes);
    }

    public static Script fromWire(WireFormat.Reader reader) throws IOException {
        return new Script(reader.readVarBytes());
    }

    public byte[] toByteArray() {
        return Arrays.copyOf(bytes, bytes.length);
    }

    public String toString() {
        String readable = toReadable();
        if (isPayToAddress()) {
            try {
                List<Token> tokens = parse();
                return readable + " (pay to " + new Address(Address.Type.COMMON, tokens.get(2).data) + ")";
            } catch (HyperLedgerException e) {
            }
        } else if (isPayToScriptHash()) {
            try {
                List<Token> tokens = parse();
                return readable + " (pay to " + new Address(Address.Type.P2SH, tokens.get(1).data) + ")";
            } catch (HyperLedgerException e) {
            }
        } else if (isPrunable()) {
            try {
                if (DigitalAssetAnnotation.isDigitalAsset(this)) {
                    List<Long> colors = DigitalAssetAnnotation.getColors(this);
                    StringBuilder b = new StringBuilder();
                    for (long c : colors) {
                        b.append(c);
                        b.append(" ");
                    }
                    return readable + " (colors " + b.toString() + ")";
                } else {
                    List<Token> tokens = parse();
                    return readable + " (data " + ByteUtils.toHex(tokens.get(1).data) + ")";
                }
            } catch (HyperLedgerException e) {
            }
        }
        return readable;
    }

    public boolean isEmpty() {
        return bytes.length == 0;
    }

    public Address toP2SHAddress() {
        try {
            return new Address(Address.Type.P2SH, Hash.keyHash(bytes));
        } catch (HyperLedgerException e) {
            // cant happen
            return null;
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Script script = (Script) o;

        return Arrays.equals(bytes, script.bytes);

    }

    @Override
    public int hashCode() {
        int result = 1;
        for (byte element : bytes)
            result = 31 * result + element;
        return result;
    }

    public static final int SIGHASH_ALL = 1;
    public static final int SIGHASH_NONE = 2;
    public static final int SIGHASH_SINGLE = 3;
    public static final int SIGHASH_ANYONECANPAY = 0x80;

    public static class Token {
        public Opcode op;
        public byte[] data;

        public Token() {
        }

        public Token(Opcode op) {
            this.op = op;
            data = null;
        }

        public String toString() {
            if (data == null) return op.toString();
            else return op.toString() + ": " + Arrays.toString(data);
        }
    }

    private static Writer writer() {
        return new Writer();
    }

    public static class Writer {
        private final ByteArrayOutputStream s;

        Writer() {
            s = new ByteArrayOutputStream();
        }

        Writer(ByteArrayOutputStream s) {
            this.s = s;
        }

        public Writer writeByte(int n) {
            s.write(n);
            return this;
        }

        public Writer writeBytes(byte[] b) {
            try {
                s.write(b);
            } catch (IOException e) {
            }
            return this;
        }

        public Writer writeInt16(long n) {
            s.write((int) (0xFFL & n));
            s.write((int) (0xFFL & (n >> 8)));

            return this;
        }

        public Writer writeInt32(long n) {
            s.write((int) (0xFF & n));
            s.write((int) (0xFF & (n >> 8)));
            s.write((int) (0xFF & (n >> 16)));
            s.write((int) (0xFF & (n >> 24)));

            return this;
        }

        public Script toScript() {
            return new Script(s.toByteArray());
        }
    }

    public static ScriptBuilder create() {
        return new ScriptBuilder();
    }

    public static Reader reader(Script s) {
        return new Reader(s);
    }

    private static class Reader {
        private final Script s;
        int cursor;

        public Reader(Script s) {
            this.s = s;
            this.cursor = 0;
        }

        public boolean eof() {
            return s.bytes == null || cursor >= s.bytes.length;
        }

        public byte[] readBytes(int n) {
            if (n < 0 || (cursor + n) > s.bytes.length) {
                throw new ArrayIndexOutOfBoundsException(cursor + n);
            }
            byte[] b = new byte[n];
            System.arraycopy(s.bytes, cursor, b, 0, n);
            cursor += n;
            return b;
        }

        public void skipBytes(int n) {
            cursor += n;
        }

        public int readByte() {
            return s.bytes[cursor++] & 0xff;
        }

        public long readInt16() {
            long value = ((s.bytes[cursor] & 0xFFL) << 0) | ((s.bytes[cursor + 1] & 0xFFL) << 8);
            cursor += 2;
            return value;
        }

        public long readInt32() {
            long value =
                    ((s.bytes[cursor] & 0xFFL) << 0) | ((s.bytes[cursor + 1] & 0xFFL) << 8) | ((s.bytes[cursor + 2] & 0xFFL) << 16)
                            | ((s.bytes[cursor + 3] & 0xFFL) << 24);
            cursor += 4;
            return value;

        }
    }

    public static class Tokenizer {
        private final Reader reader;
        private boolean enableCheckMultisigOnStackOps;

        public Tokenizer(Script script) {
            this(script, false);
        }

        public Tokenizer(Script script, boolean enableCheckMultisigOnStackOps) {
            this.enableCheckMultisigOnStackOps = enableCheckMultisigOnStackOps;
            reader = reader(script);
        }

        public boolean hashMoreElements() {
            return !reader.eof();
        }

        public int getCursor() {
            return reader.cursor;
        }

        @SuppressWarnings("incomplete-switch")
        public Token nextToken() throws HyperLedgerException {
            Token token = new Token();

            int ix = reader.readByte();
            if (!isValidOpcode(ix, enableCheckMultisigOnStackOps)) {
                throw new HyperLedgerException("Invalid script" + ix + " opcode at " + reader.cursor);
            }
            Opcode op = Opcode.fromIndex(ix);
            token.op = op;
            if (op.o <= 75) {
                token.data = reader.readBytes(op.o);
                return token;
            }
            switch (op) {
                case OP_PUSHDATA1: {
                    token.data = reader.readBytes(reader.readByte());
                    break;
                }
                case OP_PUSHDATA2: {
                    token.data = reader.readBytes((int) reader.readInt16());
                    break;
                }
                case OP_PUSHDATA4: {
                    token.data = reader.readBytes((int) reader.readInt32());
                    break;
                }
            }
            return token;
        }

        private boolean isValidOpcode(int ix, boolean enableCheckMultisigOnStackOps) {
            return ix < Opcode.count()
                    && (enableCheckMultisigOnStackOps == true
                    || (ix != Opcode.OP_CHECKMULTISIGONSTACK.o && ix != Opcode.OP_CHECKMULTISIGONSTACKVERIFY.o));
        }
    }

    public static class Number {
        byte[] w;

        public Number(byte[] b) {
            w = new byte[b.length];
            System.arraycopy(b, 0, w, 0, b.length);
        }

        public Number(long n) throws HyperLedgerException {
            if (n == 0) {
                w = new byte[0];
                return;
            }
            boolean negative = false;
            if (n < 0) {
                negative = true;
                n = -n;
            }
            if (n <= 0x7f) {
                w = new byte[]{(byte) (n & 0xff)};
                w[0] |= negative ? 0x80 : 0;
                return;
            }
            if (n <= 0x7fff) {
                w = new byte[]{(byte) (n & 0xff), (byte) ((n >> 8) & 0xff)};
                w[1] |= negative ? 0x80 : 0;
                return;
            }
            if (n <= 0x7fffff) {
                w = new byte[]{(byte) (n & 0xff), (byte) ((n >> 8) & 0xff), (byte) ((n >> 16) & 0xff)};
                w[2] |= negative ? 0x80 : 0;
                return;
            }
            w = new byte[]{(byte) (n & 0xff), (byte) ((n >> 8) & 0xff), (byte) ((n >> 16) & 0xff), (byte) ((n >> 24) & 0xff)};
            if (((n >> 24) & 0x80) != 0) {
                byte[] tmp = new byte[5];
                System.arraycopy(w, 0, tmp, 0, 4);
                w = tmp;
            }
            w[w.length - 1] |= negative ? 0x80 : 0;
        }

        public byte[] toByteArray() {
            byte[] tmp = new byte[w.length];
            System.arraycopy(w, 0, tmp, 0, w.length);
            return tmp;
        }

        public long intValue() throws HyperLedgerException {
            if (w.length == 0) {
                return 0;
            }
            boolean negative = false;
            if ((w[w.length - 1] & 0x80) != 0) {
                negative = true;
                w[w.length - 1] &= 0x7f;
            }
            int n = 0;
            if (w.length > 0) {
                n += w[0] & 0xff;
            }
            if (w.length > 1) {
                n += (w[1] & 0xff) << 8;
            }
            if (w.length > 2) {
                n += (w[2] & 0xff) << 16;
            }
            if (w.length > 3) {
                n += (w[3] & 0xff) << 24;
            }
            if (negative) {
                n = -n;
            }
            return n;
        }
    }

    public static int intValue(byte[] n) throws HyperLedgerException {
        return (int) new Script.Number(n).intValue();
    }

    public List<Script.Token> parse() throws HyperLedgerException {
        try {
            List<Script.Token> p = new ArrayList<>();
            Script.Tokenizer tokenizer = new Script.Tokenizer(this);
            while (tokenizer.hashMoreElements()) {
                p.add(tokenizer.nextToken());
            }
            return p;
        } catch (Exception e) {
            throw new HyperLedgerException("Corrupt script " + ByteUtils.toHex(bytes), e);
        }
    }

    public boolean isPushOnly() throws HyperLedgerException {
        for (Script.Token t : parse()) {
            if (t.op.o > 78) {
                return false;
            }
        }
        return true;
    }

    @SuppressWarnings("incomplete-switch")
    public int sigOpCount(boolean accurate) {
        int nsig = 0;
        try {
            Opcode last = Opcode.OP_FALSE;
            Script.Tokenizer tokenizer = new Script.Tokenizer(this);
            while (tokenizer.hashMoreElements()) {
                Script.Token token = tokenizer.nextToken();

                if (token.data == null) {
                    switch (token.op) {
                        case OP_CHECKSIG:
                        case OP_CHECKSIGVERIFY:
                            ++nsig;
                            break;
                        case OP_CHECKMULTISIG:
                        case OP_CHECKMULTISIGVERIFY:
                            // https://en.bitcoin.it/wiki/BIP_0016
                            if (accurate && last.isNumberOp()) {
                                nsig += last.getOpNumber();
                            } else {
                                nsig += 20;
                            }
                            break;
                    }
                    last = token.op;
                }
            }
        } catch (Exception e) {
            // count until possible.
        }
        return nsig;
    }

    public static Script fromReadable(String s) {
        ScriptBuilder builder = Script.create();
        StringTokenizer tokenizer = new StringTokenizer(s, " ");
        while (tokenizer.hasMoreElements()) {
            String token = tokenizer.nextToken();
            if (token.startsWith("0x")) {
                byte[] data = ByteUtils.fromHex(token.substring(2));
                builder.rawData(data);
            } else if (token.startsWith("'")) {
                String str = token.substring(1, token.length() - 1);
                try {
                    builder.data(str.getBytes("US-ASCII"));
                } catch (UnsupportedEncodingException e) {
                }
            } else if ((token.startsWith("-") || token.startsWith("0") || token.startsWith("1") || token.startsWith("2") || token.startsWith("3")
                    || token.startsWith("4") || token.startsWith("5") || token.startsWith("6") || token.startsWith("7") || token.startsWith("8") || token
                    .startsWith("9"))
                    && !token.equals("0NOTEQUAL")
                    && !token.equals("1NEGATE")
                    && !token.equals("2DROP")
                    && !token.equals("2DUP")
                    && !token.equals("3DUP")
                    && !token.equals("2OVER")
                    && !token.equals("2ROT")
                    && !token.equals("2SWAP")
                    && !token.equals("1ADD")
                    && !token.equals("1SUB") && !token.equals("2MUL") && !token.equals("2DIV") && !token.equals("2SWAP")) {
                try {
                    long n = Long.valueOf(token);
                    if (n >= 1 && n <= 16) {
                        builder.rawData(new byte[]{(byte) (Opcode.OP_1.o + (int) n - 1)});
                    } else {
                        builder.data(new Number(n).toByteArray());
                    }
                } catch (NumberFormatException | HyperLedgerException e) {
                }
            } else {
                Opcode op;
                if (token.startsWith("OP_")) {
                    op = Opcode.valueOf(token);
                } else {
                    op = Opcode.valueOf("OP_" + token);
                }
                builder.op(op.o);
            }
        }
        return builder.build();
    }

    public String toReadable() {
        List<Script.Token> tokens;
        try {
            tokens = parse();
        } catch (HyperLedgerException e) {
            return ByteUtils.toHex(bytes);
        }
        StringBuilder b = new StringBuilder();
        boolean first = true;
        for (Script.Token token : tokens) {
            if (!first) {
                b.append(" ");
            }
            first = false;
            if (token.data != null) {
                if (token.data.length > 0) {
                    if (token.data.length == 33) {
                        try {
                            Address address = new Address(Address.Type.COMMON, Hash.keyHash(token.data));
                            b.append("public key of " + address.toString());
                        } catch (HyperLedgerException e) {
                            b.append(ByteUtils.toHex(token.data));
                        }
                        //} else if ( token.data.length >= 70 || token.data.length <= 72 ) {
                        //    b.append("<sig>");    // commented out; show the token in this case too
                    } else {
                        b.append(ByteUtils.toHex(token.data));
                    }
                } else {
                    b.append("OP_FALSE");
                }
            } else {
                b.append(token.op.toString());
            }
        }
        return b.toString();
    }

    public boolean isPayToScriptHash() {
        try {
            List<Script.Token> parsed = parse();
            return parsed.size() == 3 && parsed.get(0).op == Opcode.OP_HASH160 && (parsed.get(1).data != null && parsed.get(1).op.o <= 75)
                    && parsed.get(1).data.length == 20 && parsed.get(2).op == Opcode.OP_EQUAL;
        } catch (HyperLedgerException e) {
            return false;
        }
    }

    public boolean isPayToKey() {
        try {
            List<Script.Token> parsed = parse();
            return parsed.size() == 2 && parsed.get(0).data != null && parsed.get(0).data.length >= 33 && parsed.get(0).data.length <= 120
                    && parsed.get(1).op == Opcode.OP_CHECKSIG;
        } catch (HyperLedgerException e) {
            return false;
        }
    }

    public boolean isPayToAddress() {
        try {
            List<Script.Token> parsed = parse();
            return parsed.size() == 5 && parsed.get(0).op == Opcode.OP_DUP && parsed.get(1).op == Opcode.OP_HASH160
                    && parsed.get(2).data != null && parsed.get(2).data.length == 20 && parsed.get(3).op == Opcode.OP_EQUALVERIFY
                    && parsed.get(4).op == Opcode.OP_CHECKSIG;
        } catch (HyperLedgerException e) {
            return false;
        }
    }

    public Address getAddress() {
        try {
            List<Script.Token> parsed = parse();
            if (parsed.size() == 5 && parsed.get(0).op == Opcode.OP_DUP && parsed.get(1).op == Opcode.OP_HASH160
                    && parsed.get(2).data != null && parsed.get(2).data.length == 20 && parsed.get(3).op == Opcode.OP_EQUALVERIFY
                    && parsed.get(4).op == Opcode.OP_CHECKSIG) {
                return new Address(Address.Type.COMMON, parsed.get(2).data);
            } else if (parsed.size() == 3 && parsed.get(0).op == Opcode.OP_HASH160 && (parsed.get(1).data != null && parsed.get(1).op.o <= 75)
                    && parsed.get(1).data.length == 20 && parsed.get(2).op == Opcode.OP_EQUAL) {
                return new Address(Address.Type.P2SH, parsed.get(1).data);
            } else if (parsed.size() == 2 && parsed.get(1).op == Opcode.OP_CHECKSIG && (parsed.get(0).data != null && parsed.get(0).op.o <= 75)) {
                return new Address(Address.Type.P2KEY, Hash.keyHash(parsed.get(0).data));
            }

        } catch (HyperLedgerException e) {
        }
        return null;
    }

    public boolean isMultiSig() {
        try {
            List<Script.Token> parsed = parse();
            int nkeys = -1;
            int nvotes = -1;
            for (int i = 0; i < parsed.size(); ++i) {
                if (parsed.get(i).op == Opcode.OP_CHECKMULTISIG || parsed.get(i).op == Opcode.OP_CHECKMULTISIGVERIFY) {
                    nkeys = parsed.get(i - 1).op.ordinal() - Opcode.OP_1.ordinal() + 1;
                    nvotes = parsed.get(i - nkeys - 2).op.ordinal() - Opcode.OP_1.ordinal() + 1;
                    break;
                }
            }
            if (nkeys <= 0 || nkeys > 3) {
                return false;
            }
            if (parsed.size() != nkeys + 3) {
                return false;
            }
            if (nvotes < 0 || nvotes > nkeys) {
                return false;
            }
        } catch (HyperLedgerException e) {
            return false;
        }
        return true;
    }

    public boolean isStandard() {
        return isPayToAddress() || isPayToKey() || isPayToScriptHash() || isMultiSig();
    }

    public boolean isPrunable() {
        return getPrunableData() != null;
    }

    public byte[] getPrunableData() {
        try {
            List<Token> parsed = parse();
            if (parsed.size() == 2 && parsed.get(0).op == Opcode.OP_RETURN) {
                return parsed.get(1).data;
            }
        } catch (Exception e) {
            return null;
        }
        return null;
    }

    public static Script data(byte[] data) throws HyperLedgerException {
        if (data.length > 80)
            throw new HyperLedgerException("Annotation data limit exceeded");
        ScriptBuilder builder = new ScriptBuilder();
        builder.op(Opcode.OP_RETURN);
        builder.data(data);
        return builder.build();
    }

    public static Script deleteSignatureFromScript(Script script, byte[] sig) throws HyperLedgerException {
        Script.Tokenizer tokenizer = new Script.Tokenizer(script);
        ScriptBuilder builder = new ScriptBuilder();
        while (tokenizer.hashMoreElements()) {
            Script.Token token = tokenizer.nextToken();
            if (token.data != null && token.op.o <= 75 && token.data.length == sig.length) {
                boolean found = true;
                for (int i = 0; i < sig.length; ++i) {
                    if (sig[i] != token.data[i]) {
                        found = false;
                        break;
                    }
                }
                if (!found) {
                    builder.token(token);
                }
            } else {
                builder.token(token);
            }
        }
        return builder.build();
    }

    public static void derSig(byte[] sig) throws HyperLedgerException {
        if (sig.length < 9) {
            throw new HyperLedgerException("Non-canonical signature: too short");
        }
        if (sig.length > 73) {
            throw new HyperLedgerException("Non-canonical signature: too long");
        }
        if (sig[0] != 0x30) {
            throw new HyperLedgerException("Non-canonical signature: wrong type");
        }
        if (sig[1] != sig.length - 3) {
            throw new HyperLedgerException("Non-canonical signature: wrong length marker");
        }
        int nLenR = sig[3];
        if (5 + nLenR - sig.length >= 0) {
            throw new HyperLedgerException("Non-canonical signature: S length misplaced");
        }
        int nLenS = sig[5 + nLenR];
        if ((nLenR + nLenS + 7) != sig.length) {
            throw new HyperLedgerException("Non-canonical signature: R+S length mismatch");
        }
        if (sig[2] != 0x02) {
            throw new HyperLedgerException("Non-canonical signature: R value type mismatch");
        }
        if (nLenR == 0) {
            throw new HyperLedgerException("Non-canonical signature: R length is zero");
        }
        if ((sig[4] & 0x80) != 0) {
            throw new HyperLedgerException("Non-canonical signature: R value negative");
        }
        if (nLenR > 1 && (sig[4] == 0x00) && (sig[5] & 0x80) == 0) {
            throw new HyperLedgerException("Non-canonical signature: R value excessively padded");
        }
        if (sig[nLenR + 6 - 2] != 0x02) {
            throw new HyperLedgerException("Non-canonical signature: S value type mismatch");
        }
        if (nLenS == 0) {
            throw new HyperLedgerException("Non-canonical signature: S length is zero");
        }
        if ((sig[6 + nLenR] & 0x80) != 0) {
            throw new HyperLedgerException("Non-canonical signature: S value negative");
        }
        if (nLenS > 1 && (sig[6 + nLenR] == 0x00) && (sig[6 + nLenR + 1] & 0x80) == 0) {
            throw new HyperLedgerException("Non-canonical signature: S value excessively padded");
        }
    }
}
