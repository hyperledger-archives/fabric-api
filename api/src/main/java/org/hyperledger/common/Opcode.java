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

/**
 * Bitcoin's opcodes in Script @Link https://en.bitcoin.it/wiki/Script
 *
 * @See Script
 */
public enum Opcode {
    OP_FALSE(0), OP_PUSH1(1), OP_PUSH2(2), OP_PUSH3(3), OP_PUSH4(4), OP_PUSH5(5), OP_PUSH6(6), OP_PUSH7(7), OP_PUSH8(8), OP_PUSH9(9), OP_PUSH10(
            10), OP_PUSH11(11), OP_PUSH12(12), OP_PUSH13(13), OP_PUSH14(14), OP_PUSH15(15), OP_PUSH16(16), OP_PUSH17(17), OP_PUSH18(18), OP_PUSH19(
            19), OP_PUSH20(20), OP_PUSH21(21), OP_PUSH22(22), OP_PUSH23(23), OP_PUSH24(24), OP_PUSH25(25), OP_PUSH26(26), OP_PUSH27(27), OP_PUSH28(
            28), OP_PUSH29(29), OP_PUSH30(30), OP_PUSH31(31), OP_PUSH32(32), OP_PUSH33(33), OP_PUSH34(34), OP_PUSH35(35), OP_PUSH36(36), OP_PUSH37(
            37), OP_PUSH38(38), OP_PUSH39(39), OP_PUSH40(40), OP_PUSH41(41), OP_PUSH42(42), OP_PUSH43(43), OP_PUSH44(44), OP_PUSH45(45), OP_PUSH46(
            46), OP_PUSH47(47), OP_PUSH48(48), OP_PUSH49(49), OP_PUSH50(50), OP_PUSH51(51), OP_PUSH52(52), OP_PUSH53(53), OP_PUSH54(54), OP_PUSH55(
            55), OP_PUSH56(56), OP_PUSH57(57), OP_PUSH58(58), OP_PUSH59(59), OP_PUSH60(60), OP_PUSH61(61), OP_PUSH62(62), OP_PUSH63(63), OP_PUSH64(
            64), OP_PUSH65(65), OP_PUSH66(66), OP_PUSH67(67), OP_PUSH68(68), OP_PUSH69(69), OP_PUSH70(70), OP_PUSH71(71), OP_PUSH72(72), OP_PUSH73(
            73), OP_PUSH74(74), OP_PUSH75(75),

    OP_PUSHDATA1(76), OP_PUSHDATA2(77), OP_PUSHDATA4(78), OP_1NEGATE(79),

    OP_RESERVED(80),

    OP_1(81), OP_2(82), OP_3(83), OP_4(84), OP_5(85), OP_6(86), OP_7(87), OP_8(88), OP_9(89), OP_10(90), OP_11(91), OP_12(92), OP_13(93),
    OP_14(94), OP_15(95), OP_16(96),

    OP_NOP(97), OP_VER(98), OP_IF(99), OP_NOTIF(100), OP_VERIF(101), OP_VERNOTIF(102),

    OP_ELSE(103), OP_ENDIF(104), OP_VERIFY(105), OP_RETURN(106),

    OP_TOALTSTACK(107), OP_FROMALTSTACK(108), OP_2DROP(109), OP_2DUP(110), OP_3DUP(111), OP_2OVER(112), OP_2ROT(113), OP_2SWAP(114),
    OP_IFDUP(115), OP_DEPTH(116), OP_DROP(117), OP_DUP(118), OP_NIP(119), OP_OVER(120), OP_PICK(121), OP_ROLL(122), OP_ROT(123), OP_SWAP(124),
    OP_TUCK(125),

    OP_CAT(126), OP_SUBSTR(127), OP_LEFT(128), OP_RIGHT(129), OP_SIZE(130), OP_INVERT(131), OP_AND(132), OP_OR(133), OP_XOR(134),

    OP_EQUAL(135), OP_EQUALVERIFY(136),

    OP_RESERVED1(137), OP_RESERVED2(138),

    OP_1ADD(139), // 0x8b in out 1 is added to the input.
    OP_1SUB(140), // 0x8c in out 1 is subtracted from the input.
    OP_2MUL(141), // 0x8d in out The input is multiplied by 2. Currently
    // disabled.
    OP_2DIV(142), // 0x8e in out The input is divided by 2. Currently
    // disabled.
    OP_NEGATE(143), // 0x8f in out The sign of the input is flipped.
    OP_ABS(144), // 0x90 in out The input is made positive.
    OP_NOT(145), // 0x91 in out If the input is 0 or 1, it is flipped.
    // Otherwise the output will be 0.
    OP_0NOTEQUAL(146), // 0x92 in out Returns 0 if the input is 0. 1
    // otherwise.
    OP_ADD(147), // 0x93 a b out a is added to b.
    OP_SUB(148), // 0x94 a b out b is subtracted from a.
    OP_MUL(149), // 0x95 a b out a is multiplied by b. Currently disabled.
    OP_DIV(150), // 0x96 a b out a is divided by b. Currently disabled.
    OP_MOD(151), // 0x97 a b out Returns the remainder after dividing a by
    // b. Currently disabled.
    OP_LSHIFT(152), // 0x98 a b out Shifts a left b bits, preserving sign.
    // Currently disabled.
    OP_RSHIFT(153), // 0x99 a b out Shifts a right b bits, preserving sign.
    // Currently disabled.
    OP_BOOLAND(154), // 0x9a a b out If both a and b are not 0, the output
    // is 1. Otherwise 0.
    OP_BOOLOR(155), // 0x9b a b out If a or b is not 0, the output is 1.
    // Otherwise 0.
    OP_NUMEQUAL(156), // 0x9c a b out Returns 1 if the numbers are equal, 0
    // otherwise.
    OP_NUMEQUALVERIFY(157), // 0x9d a b out Same as OP_NUMEQUAL, but runs
    // OP_VERIFY afterward.
    OP_NUMNOTEQUAL(158), // 0x9e a b out Returns 1 if the numbers are not
    // equal, 0 otherwise.
    OP_LESSTHAN(159), // 0x9f a b out Returns 1 if a is less than b, 0
    // otherwise.
    OP_GREATERTHAN(160), // 0xa0 a b out Returns 1 if a is greater than b,
    // 0
    // otherwise.
    OP_LESSTHANOREQUAL(161), // 0xa1 a b out Returns 1 if a is less than or
    // equal to b, 0 otherwise.
    OP_GREATERTHANOREQUAL(162), // 0xa2 a b out Returns 1 if a is greater
    // than or equal to b, 0 otherwise.
    OP_MIN(163), // 0xa3 a b out Returns the smaller of a and b.
    OP_MAX(164), // 0xa4 a b out Returns the larger of a and b.
    OP_WITHIN(165), // 0xa5 x min max out Returns 1 if x is within the
    // specified range (left-inclusive), 0 otherwise.

    OP_RIPEMD160(166), // 0xa6 in hash The input is hashed using
    // RIPEMD-160.
    OP_SHA1(167), // 0xa7 in hash The input is hashed using SHA-1.
    OP_SHA256(168), // 0xa8 in hash The input is hashed using SHA-256.
    OP_HASH160(169), // 0xa9 in hash The input is hashed twice: first with
    // SHA-256 and then with RIPEMD-160.
    OP_HASH256(170), // 0xaa in hash The input is hashed two times with
    // SHA-256.
    OP_CODESEPARATOR(171), // 0xab Nothing Nothing All of the signature
    // checking words will only match signatures to
    // the data after the most recently-executed
    // OP_CODESEPARATOR.
    OP_CHECKSIG(172), // 0xac sig pubkey True / false The entire
    // transaction's outputs, inputs, and script (from
    // the most recently-executed OP_CODESEPARATOR to
    // the end) are hashed. The signature used by
    // OP_CHECKSIG must be a valid signature for this
    // hash and public key. If it is, 1 is returned, 0
    // otherwise.
    OP_CHECKSIGVERIFY(173), // 0xad sig pubkey True / false Same as
    // OP_CHECKSIG, but OP_VERIFY is executed
    // afterward.
    OP_CHECKMULTISIG(174), // 0xae x sig1 sig2 ... <number of signatures>
    // pub1 pub2 <number of public keys> True /
    // False For each signature and public key pair,
    // OP_CHECKSIG is executed. If more public keys
    // than signatures are listed, some key/sig
    // pairs can fail. All signatures need to match
    // a public key. If all signatures are valid, 1
    // is returned, 0 otherwise. Due to a bug, one
    // extra unused value is removed from the stack.
    OP_CHECKMULTISIGVERIFY(175), // 0xaf x sig1 sig2 ... <number of
    // signatures> pub1 pub2 ... <number of
    // public keys> True / False Same as
    // OP_CHECKMULTISIG, but OP_VERIFY is
    // executed afterward.
    OP_NOP1(176), OP_NOP2(177), OP_NOP3(178), OP_NOP4(179), OP_NOP5(180), OP_NOP6(181), OP_NOP7(182), OP_NOP8(183), OP_NOP9(184), OP_NOP10(185),

    /**
     * Inputs:
     * - number of public keys and signatures
     * - public keys in growing order from the bottom
     * - hash of the data to be checked, which in our case is the hash of the original header
     * - the required minimum number of matching signature/public key pairs out of k
     * - signatures in the order that they correspond to the public key with the same index, or a zero byte placeholder if the given signature is not provided
     * <p>
     * Output:
     * <p>
     * OP_CHECKMULTISIGONSTACK: 1 on the stack if and only if there are r matching signatures out of
     * k for the public keys over the hash of the data, 0 otherwise.
     * <p>
     * OP_CHECKMULTISIGONSTACKVERIFY: 1 on the stack if and only if there are r matching signatures out of
     * k for the public keys over the hash of the data, aborts the evaluation otherwise
     */
    OP_CHECKMULTISIGONSTACK(186),
    OP_CHECKMULTISIGONSTACKVERIFY(187);

    public final int o;

    Opcode(int n) {
        this.o = n;
    }

    private static final Opcode codes[] = values();

    public static int count() {
        return codes.length;
    }

    public static Opcode fromIndex(int i) {
        return codes[i];
    }

    public static Opcode getNumberOp(final int n) {
        if (n > 0 && n < 17) {
            return fromIndex(OP_1.ordinal() + n - 1);
        } else {
            return OP_FALSE;
        }
    }

    public int getOpNumber() {
        if (isNumberOp()) {
            return o - OP_1.o + 1;
        } else {
            return -1;
        }
    }

    public boolean isNumberOp() {
        return o >= OP_1.o && o <= OP_16.o;
    }
}
