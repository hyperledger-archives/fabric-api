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
package org.hyperledger.account.color;

import junit.framework.TestCase;
import org.hyperledger.account.AddressChain;
import org.hyperledger.account.CoinBucket;
import org.hyperledger.account.PaymentOptions;
import org.hyperledger.account.ReadOnlyAccount;
import org.hyperledger.common.*;
import org.hyperledger.common.color.Color;
import org.hyperledger.common.color.ColoredTransactionOutput;
import org.hyperledger.common.color.ForeignAsset;
import org.hyperledger.common.color.NativeAsset;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.Arrays;
import java.util.Collections;

import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class ColoredBaseTransactionFactoryTest extends TestCase {
    @Mock
    ReadOnlyAccount readOnlyAccount;

    @Mock
    CoinBucket coinBucket;

    @Mock
    ColoredCoinBucket coloredCoinBucket;

    @Mock
    ColoredCoinBucket emptyColoredCoinBucket;

    @Mock
    AddressChain addressChain;

    @Test
    public void testGetSufficientSources() throws Exception {
        Color color = new ForeignAsset(PrivateKey.createNew().getAddress());
        Coin coin = new Coin(new Outpoint(TID.INVALID, 0), new ColoredTransactionOutput(1000, new Script(), color, 100));

        setUpAccount(color, coin);

        ColoredBaseTransactionFactory onTest = new ColoredBaseTransactionFactory(readOnlyAccount);
        onTest.getSufficientSources(color, 100L);
    }

    public void setUpAccount(Color color, Coin... coins) throws HyperLedgerException {
        when(coinBucket.getCoins()).thenReturn(Arrays.asList(coins));
        when(emptyColoredCoinBucket.getCoins(color)).thenReturn(emptyColoredCoinBucket);
        when(emptyColoredCoinBucket.getCoins()).thenReturn(Collections.emptyList());
        when(readOnlyAccount.getChangeCoins()).thenReturn(coinBucket);
        when(readOnlyAccount.getConfirmedCoins()).thenReturn(emptyColoredCoinBucket);
        when(readOnlyAccount.getReceivingCoins()).thenReturn(emptyColoredCoinBucket);
        when(readOnlyAccount.getChain()).thenReturn(addressChain);
        when(addressChain.getNextChangeAddress()).thenReturn(PrivateKey.createNew().getAddress());
    }

    public Color setUpNative() throws HyperLedgerException {
        Transaction issueTx = Transaction.create().build();
        Color color = new NativeAsset(issueTx.getID(), 0);
        Coin coin = new Coin(new Outpoint(TID.INVALID, 0), new ColoredTransactionOutput(0, new Script(), color, 100));
        Coin tokenCoin = new Coin(new Outpoint(TID.INVALID, 1), new ColoredTransactionOutput(100000, new Script(), Color.BITCOIN, 0));

        setUpAccount(color, coin, tokenCoin);
        return color;
    }

    @Test
    public void testNativeAssetPropose() throws HyperLedgerException {
        Color color = setUpNative();

        Address address = PrivateKey.createNew().getAddress();
        ColoredBaseTransactionFactory factory =
                new ColoredBaseTransactionFactory(readOnlyAccount, new WireFormatter(WireFormatter.WireFormatFlags.NATIVE_ASSET));
        ColoredTransactionProposal proposal = factory.proposeColored(PaymentOptions.common,
                ColoredTransactionOutput.create().payTo(address).color(new NativeAsset(0)).quantity(111).build(),
                ColoredTransactionOutput.create().payTo(address).color(color).quantity(100).build(),
                ColoredTransactionOutput.create().payTo(address).color(Color.BITCOIN).value(3000).build()
        );
        assertEquals(4, proposal.getSinks().size());
        assertTrue(proposal.getSource(0).getOutpoint().isNull()); // starts with marker for new asset
    }

    @Test
    public void testNativeAssetProposeWithChange() throws HyperLedgerException {
        Color color = setUpNative();

        Address address = PrivateKey.createNew().getAddress();
        ColoredBaseTransactionFactory factory =
                new ColoredBaseTransactionFactory(readOnlyAccount, new WireFormatter(WireFormatter.WireFormatFlags.NATIVE_ASSET));
        ColoredTransactionProposal proposal = factory.proposeColored(PaymentOptions.common,
                ColoredTransactionOutput.create().payTo(address).color(new NativeAsset(0)).quantity(111).build(),
                ColoredTransactionOutput.create().payTo(address).color(color).quantity(60).build(),
                ColoredTransactionOutput.create().payTo(address).color(Color.BITCOIN).value(3000).build()
        );
        assertEquals(5, proposal.getSinks().size());
        assertTrue(proposal.getSource(0).getOutpoint().isNull()); // starts with marker for new asset
    }
}
