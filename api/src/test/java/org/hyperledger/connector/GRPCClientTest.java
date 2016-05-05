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
package org.hyperledger.connector;

import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.junit.Assert.*;

import org.hyperledger.account.BaseAccount;
import org.hyperledger.account.BaseTransactionFactory;
import org.hyperledger.account.KeyListChain;
import org.hyperledger.account.TransactionFactory;
import org.hyperledger.api.BCSAPIException;
import org.hyperledger.common.Address;
import org.hyperledger.common.HyperLedgerException;
import org.hyperledger.common.PrivateKey;
import org.hyperledger.common.Transaction;

public class GRPCClientTest {
    private static final Logger log = LoggerFactory.getLogger(GRPCClientTest.class);
    
    private String host = "localhost";
    private int port = 30303;
	private int observerPort = 31315;
	
	@Test
	public void testGetBlockHeight() throws BCSAPIException {
		GRPCClient client = new GRPCClient(host, port, observerPort);
		
		int height = client.getChainHeight();
		
		log.debug("testGetBlockHeight height=" + height);
		
		assertTrue(height > 0);
	}
	
	@Test
	@Ignore
	public void testSendTransaction() throws BCSAPIException, HyperLedgerException {		
		KeyListChain senderKeyChain = new KeyListChain(PrivateKey.createNew(true));
		Address receiverAddress = PrivateKey.createNew().getAddress();
		BaseAccount senderAccount = new BaseAccount(senderKeyChain);
		TransactionFactory factory = new BaseTransactionFactory(senderAccount);
        Transaction tx = factory.propose(receiverAddress, 30000).sign(senderKeyChain);
		
		log.debug("testSendTransaction tx=" + tx);
		
        GRPCClient client = new GRPCClient(host, port, observerPort);
        
        client.sendTransaction(tx);
		
		log.debug("testSendTransaction");
	}

}
