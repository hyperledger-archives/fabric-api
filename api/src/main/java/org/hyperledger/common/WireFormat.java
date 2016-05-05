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

import java.io.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Wire format serializer and deserializer. Use to parse P2P messages.
 * <p>
 * Note that satoshi used unsigned integers for most fields while java's native integer is signed.
 * HyperLedger reads/writes and store unsigned with same size java integers as this saves memory
 * and gives better performance. The trade-off is that comparison of integers should not be done
 * via regular operators, but e.g. with Integer.compareUnsigned
 *
 * @see Integer#compareUnsigned(int, int)
 * @see Long#compareUnsigned(long, long)
 */
public class WireFormat {
    public static class InsufficientBytesException extends IOException {
        private final int needed;
        private final int have;

        public InsufficientBytesException(int needed, int have) {
            super("Attempted to read " + needed + " byte(s) but only " + have + " byte(s) could be read");
            this.needed = needed;
            this.have = have;
        }

        public int getNeeded() {
            return needed;
        }

        public int getHave() {
            return have;
        }
    }

    /**
     * Wire format writer
     */
    public static class Writer {
        protected final OutputStream bs;

        public Writer(OutputStream bs) {
            this.bs = bs;
        }

        public void writeByte(int n) throws IOException {
            bs.write(n);
        }

        public void writeUint16(int n) throws IOException {
            writeByte(n);
            writeByte(n >>> 8);
        }

        public void writeUint32(int n) throws IOException {
            writeByte(n);
            writeByte(n >>> 8);
            writeByte(n >>> 16);
            writeByte(n >>> 24);
        }

        public void writeUint64(long n) throws IOException {
            writeByte((int) n);
            writeByte((int) (n >>> 8));
            writeByte((int) (n >>> 16));
            writeByte((int) (n >>> 24));
            writeByte((int) (n >>> 32));
            writeByte((int) (n >>> 40));
            writeByte((int) (n >>> 48));
            writeByte((int) (n >>> 56));
        }

        public void writeVarInt(int n) throws IOException {
            if (Integer.compareUnsigned(n, 0xfd) < 0) {
                writeByte(n);
            } else if (Integer.compareUnsigned(n, 65536) < 0) {
                writeByte(0xfd);
                writeUint16(n);
            } else {
                writeByte(0xfe);
                writeUint32(n);
            }
        }

        public void writeVarInt(long n) throws IOException {
            if (Long.compareUnsigned(n, 0xfdl) < 0) {
                writeByte((int) n);
            } else if (Long.compareUnsigned(n, 65536l) < 0) {
                writeByte(0xfd);
                writeUint16((int) n);
            } else if (Long.compareUnsigned(n, 4294967295L) < 0) {
                writeByte(0xfe);
                writeUint32((int) n);
            } else {
                writeByte(0xff);
                writeUint32((int) n);
                writeUint32((int) (n >>> 32));
            }
        }

        public void writeBytes(byte[] b) throws IOException {
            bs.write(b);
        }

        public void writeHash(Hash h) throws IOException {
            writeBytes(h.unsafeGetArray());
        }

        public void writeVarBytes(byte[] b) throws IOException {
            writeVarInt(b.length);
            if (b.length > 0) {
                writeBytes(b);
            }
        }

        public void writeString(String s) throws IOException {
            if (s != null) {
                writeVarBytes(s.getBytes("UTF-8"));
            } else {
                writeVarBytes(new byte[0]);
            }
        }
    }

    /**
     * wire format reader
     */
    public static class Reader {
        protected final InputStream input;

        public Reader(byte[] bytes) throws IOException {
            this(new ByteArrayInputStream(bytes));
        }

        public Reader(InputStream input) throws IOException {
            this.input = input;
        }

        public boolean eof() throws IOException {
            return input.available() <= 0;
        }

        public int readByte() throws IOException {
            return input.read();
        }

        public void readBytes(byte[] bytes) throws IOException {
            int num = input.read(bytes);
            if (num < bytes.length)
                throw new InsufficientBytesException(bytes.length, num);
        }

        public byte[] readBytes(int length) throws IOException {
            byte[] b = new byte[length];
            if (length > 0) {
                readBytes(b);
            }
            return b;
        }

        public int readUint16() throws IOException {
            byte[] bytes = new byte[2];
            readBytes(bytes);
            return (bytes[0] & 0xFF) | ((bytes[1] & 0xFF) << 8);
        }

        public int readUint32() throws IOException {
            byte[] bytes = new byte[4];
            readBytes(bytes);
            return (bytes[0] & 0xFF) | ((bytes[1] & 0xFF) << 8) | ((bytes[2] & 0xFF) << 16)
                    | ((bytes[3] & 0xFF) << 24);
        }

        public long readUint64() throws IOException {
            byte[] bytes = new byte[8];
            readBytes(bytes);
            return (bytes[0] & 0xFFL)
                    | ((bytes[1] & 0xFFL) << 8)
                    | ((bytes[2] & 0xFFL) << 16)
                    | ((bytes[3] & 0xFFL) << 24
                    | ((bytes[4] & 0xFFL) << 32)
                    | ((bytes[5] & 0xFFL) << 40)
                    | ((bytes[6] & 0xFFL) << 48)
                    | ((bytes[7] & 0xFFL) << 56));
        }

        public long readVarInt() throws IOException {
            int flag = readByte();
            if (flag < 0xfd) {
                return flag & 0xFFL;
            } else if (flag == 0xfd) {
                return readUint16() & 0xFFFFL;
            } else if (flag == 0xfe) {
                return readUint32() & 0xFFFFFFFFL;
            } else {
                return readUint32() | ((long) readUint32()) << 32;
            }
        }

        public Hash readHash() throws IOException {
            return Hash.createFromSafeArray(readBytes(32));
        }

        public byte[] readVarBytes() throws IOException {
            int len = (int) (readVarInt() & 0x7FFFFFFFL);
            return readBytes(len);
        }

        public String readString() throws IOException {
            try {
                return new String(readVarBytes(), "UTF-8");
            } catch (UnsupportedEncodingException e) {
            }
            return null;
        }

    }


    /**
     * a wire format writer that computes
     * the double sha256 digest of the stream
     */
    public static class HashWriter extends Writer {
        private final MessageDigest digest;

        public HashWriter() throws IOException {
            super(null);
            try {
                digest = MessageDigest.getInstance("SHA-256");
            } catch (NoSuchAlgorithmException e) {
                throw new IOException(e);
            }
        }

        public void writeByte(int n) throws IOException {
            digest.update((byte) (n & 0xFF));
        }

        public void writeBytes(byte[] b) throws IOException {
            digest.update(b);
        }

        public Hash hash() {
            return Hash.createFromSafeArray(digest.digest(digest.digest()));
        }
    }

    /**
     * A writer into a byte array
     */
    public static class ArrayWriter extends Writer {
        public ArrayWriter() {
            super(new ByteArrayOutputStream());
        }

        public byte[] toByteArray() {
            return ((ByteArrayOutputStream) bs).toByteArray();
        }
    }

    /**
     * A writer that only computes size of written output.
     */
    public static class SizeWriter extends Writer {
        public SizeWriter() {
            super(null);
        }

        public int size = 0;

        @Override
        public void writeUint16(int n) throws IOException {
            size += 2;
        }

        @Override
        public void writeUint32(int n) throws IOException {
            size += 4;
        }

        @Override
        public void writeUint64(long n) throws IOException {
            size += 8;
        }

        @Override
        public void writeHash(Hash h) throws IOException {
            size += 32;
        }

        @Override
        public void writeBytes(byte b[]) throws IOException {
            size += b.length;
        }

        @Override
        public void writeByte(int b) throws IOException {
            ++size;
        }

        @Override
        public void writeVarInt(int n) throws IOException {
            if (Integer.compareUnsigned(n, 0xfd) < 0) {
                size += 1;
            } else if (Integer.compareUnsigned(n, 65536) < 0) {
                size += 3;
            } else {
                size += 5;
            }
        }

        @Override
        public void writeVarInt(long n) throws IOException {
            if (Long.compareUnsigned(n, 0xfdl) < 0) {
                size += 1;
            } else if (Long.compareUnsigned(n, 65536l) < 0) {
                size += 3;
            } else if (Long.compareUnsigned(n, 4294967295L) < 0) {
                size += 5;
            } else {
                size += 9;
            }
        }

        public int size() {
            return size;
        }
    }
}
