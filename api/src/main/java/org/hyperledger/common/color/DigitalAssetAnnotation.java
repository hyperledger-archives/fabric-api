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
package org.hyperledger.common.color;

import org.hyperledger.common.HyperLedgerException;
import org.hyperledger.common.Script;
import org.hyperledger.common.TransactionOutput;

import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Helper class to annotate transactions with color tracking
 */
public class DigitalAssetAnnotation {

    private static byte[] prefix = {0x44, 0x41, 0x01, 0x00};

    public static boolean isDigitalAsset(Script script) {
        byte[] data = script.getPrunableData();
        return data != null && data.length >= prefix.length && Arrays.equals(Arrays.copyOfRange(data, 0, prefix.length), prefix);
    }

    public static Script indicateColors(long... quantities) throws HyperLedgerException {
        if (quantities.length >= 0xfd)
            throw new HyperLedgerException("Do not support that many colored outputs");

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        out.write(prefix, 0, prefix.length);
        out.write(quantities.length);
        for (long q : quantities)
            try {
                Leb128.writeUnsignedLeb128(out, q);
            } catch (IOException e) {
                throw new HyperLedgerException(e);
            }
        return Script.data(out.toByteArray());
    }

    public static TransactionOutput indicateColors(List<Long> quantities) throws HyperLedgerException {
        if (quantities.size() >= 0xfd)
            throw new HyperLedgerException("Do not support that many colored outputs");

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        out.write(prefix, 0, prefix.length);
        out.write(quantities.size());
        for (Long q : quantities)
            try {
                Leb128.writeUnsignedLeb128(out, q);
            } catch (IOException e) {
                throw new HyperLedgerException(e);
            }
        return TransactionOutput.create().data(out.toByteArray()).build();
    }

    public static List<Long> getColors(Script script) throws HyperLedgerException {
        ByteArrayInputStream in = new ByteArrayInputStream(script.getPrunableData());
        in.skip(prefix.length);
        int n = in.read() & 0xff;
        List<Long> colors = new ArrayList<>();
        for (int i = 0; i < n; ++i)
            try {
                colors.add(Leb128.readUnsignedLeb128(in));
            } catch (IOException e) {
                throw new HyperLedgerException(e);
            }
        return colors;
    }

/*
 * Copyright (C) 2008 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
/*
    Modified to use input and output streams and longs by Tamas Blummer
 */

    /**
     * Reads and writes DWARFv3 LEB 128 signed and unsigned integers. See DWARF v3
     * section 7.6.
     */
    private static class Leb128 {
        private Leb128() {
        }

        /**
         * Gets the number of bytes in the unsigned LEB128 encoding of the
         * given value.
         *
         * @param value the value in question
         * @return its write size, in bytes
         */
        public static int unsignedLeb128Size(long value) {
            // TODO: This could be much cleverer.

            long remaining = value >> 7;
            int count = 0;

            while (remaining != 0) {
                remaining >>= 7;
                count++;
            }

            return count + 1;
        }

        /**
         * Gets the number of bytes in the signed LEB128 encoding of the
         * given value.
         *
         * @param value the value in question
         * @return its write size, in bytes
         */
        public static int signedLeb128Size(long value) {
            // TODO: This could be much cleverer.

            long remaining = value >> 7;
            int count = 0;
            boolean hasMore = true;
            int end = ((value & Integer.MIN_VALUE) == 0) ? 0 : -1;

            while (hasMore) {
                hasMore = (remaining != end)
                        || ((remaining & 1) != ((value >> 6) & 1));

                value = remaining;
                remaining >>= 7;
                count++;
            }

            return count;
        }

        /**
         * Reads an signed integer from {@code in}.
         */
        public static long readSignedLeb128(InputStream in) throws IOException {
            long result = 0;
            int cur;
            int count = 0;
            long signBits = -1;

            do {
                cur = in.read() & 0xff;
                result |= (cur & 0x7f) << (count * 7);
                signBits <<= 7;
                count++;
            } while (((cur & 0x80) == 0x80) && count < 5);

            if ((cur & 0x80) == 0x80) {
                throw new IOException("invalid LEB128 sequence");
            }

            // Sign extend if appropriate
            if (((signBits >> 1) & result) != 0) {
                result |= signBits;
            }

            return result;
        }

        /**
         * Reads an unsigned integer from {@code in}.
         */
        public static long readUnsignedLeb128(InputStream in) throws IOException {
            long result = 0;
            int cur;
            int count = 0;

            do {
                cur = in.read() & 0xff;
                result |= (cur & 0x7f) << (count * 7);
                count++;
            } while (((cur & 0x80) == 0x80) && count < 5);

            if ((cur & 0x80) == 0x80) {
                throw new IOException("invalid LEB128 sequence");
            }

            return result;
        }

        /**
         * Writes {@code value} as an unsigned integer to {@code out}, starting at
         * {@code offset}. Returns the number of bytes written.
         */
        public static void writeUnsignedLeb128(OutputStream out, long value) throws IOException {
            long remaining = value >>> 7;

            while (remaining != 0) {
                out.write((int) ((value & 0x7f) | 0x80));
                value = remaining;
                remaining >>>= 7;
            }

            out.write((int) (value & 0x7f));
        }

        /**
         * Writes {@code value} as a signed integer to {@code out}, starting at
         * {@code offset}. Returns the number of bytes written.
         */
        public static void writeSignedLeb128(OutputStream out, long value) throws IOException {
            long remaining = value >> 7;
            boolean hasMore = true;
            int end = ((value & Integer.MIN_VALUE) == 0) ? 0 : -1;

            while (hasMore) {
                hasMore = (remaining != end)
                        || ((remaining & 1) != ((value >> 6) & 1));

                out.write((int) (value & 0x7f) | (hasMore ? 0x80 : 0));
                value = remaining;
                remaining >>= 7;
            }
        }
    }
}
