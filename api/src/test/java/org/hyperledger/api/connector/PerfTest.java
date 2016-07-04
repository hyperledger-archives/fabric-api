/**
 * Copyright 2016 Digital Asset Holdings, LLC
 *
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

package org.hyperledger.api.connector;

import com.google.common.base.Stopwatch;
import org.hyperledger.api.HLAPI;
import org.hyperledger.api.HLAPIException;
import org.hyperledger.api.TransactionListener;
import org.hyperledger.transaction.TID;
import org.hyperledger.transaction.Transaction;
import org.hyperledger.transaction.TransactionTest;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.*;

public class PerfTest {

    private static final int SIZE = 10000;
    private static final int NR_OF_GROUPS = 10;
    private static final int CHUNK_SIZE = SIZE / NR_OF_GROUPS;
    private static final int NR_OF_CONCURRENT_TRANSACTIONS = 500;

    private final HLAPI api = new GRPCClient("localhost", 30303, 31315);
    private final TransactionListener listener;
    private final List<MeasurableTransaction> txs;
    private final Map<TID, MeasurableTransaction> txMap;

    public PerfTest() {
        OpenTransactionLimiter limiter = new OpenTransactionLimiter(NR_OF_CONCURRENT_TRANSACTIONS);
        List<MeasurableTransaction> transactions = new ArrayList<>();
        Map<TID, MeasurableTransaction> transactionMap = new HashMap<>();
        for (int i = 0; i < SIZE; i++) {
            MeasurableTransaction t = new MeasurableTransaction(TransactionTest.randomTx(), limiter);
            transactions.add(t);
            transactionMap.put(t.tx.getID(), t);
        }
        txs = Collections.unmodifiableList(transactions);
        txMap = Collections.unmodifiableMap(transactionMap);

        listener = t -> txMap.get(t.getID()).complete();
    }

    @Before
    public void setUp() throws HLAPIException {
        api.registerTransactionListener(listener);
    }

    @After
    public void tearDown() {
        api.removeTransactionListener(listener);
    }

    @Test
    public void performance() throws HLAPIException, ExecutionException, InterruptedException {
        Stopwatch totalTime = Stopwatch.createStarted();

        for (MeasurableTransaction t : txs) {
            t.send(api);
            assertFalse(MeasurableTransaction.error.get());
        }
        List<Long> results = MeasurableTransaction.waitAll(txs).get();

        totalTime.stop();

        checkTransactionsAdded();
        printResults(totalTime, results);
    }

    private void checkTransactionsAdded() throws HLAPIException {
        for (MeasurableTransaction t : txs) {
            TID id = t.tx.getID();
            Transaction storedTx = api.getTransaction(id);
            assertNotNull("No transaction found with the id " + id, storedTx);
            assertEquals(t.tx, storedTx);
        }
    }

    private void printResults(Stopwatch totalTime, List<Long> results) {
        System.out.format("====== Test results ======\n");

        double totalSeconds = totalTime.elapsed(TimeUnit.MILLISECONDS) / 1000.0;
        System.out.format("Total: %d transactions in %.2f sec\n", SIZE, totalSeconds);
        System.out.format("Average transaction/s: %.2f\n", SIZE / totalSeconds);
        System.out.format("Average transaction process time: %.2f ms\n", avg(results));

        System.out.format("Distribution:\n");
        for (int i = 0; i < NR_OF_GROUPS; i++) {
            int lowerBound = i * CHUNK_SIZE;
            int upperBound = (i + 1) * CHUNK_SIZE;

            double timeDiffSec = (txs.get(upperBound - 1).completionTime - txs.get(lowerBound).completionTime) / 1000.0;
            double txsPerSec = (double) CHUNK_SIZE / timeDiffSec;
            double processTime = avg(results.subList(lowerBound, upperBound));

            System.out.format("%6d - %6d:\ttx/sec=%5.2f\tavg_tx_time=%5.2f\n", lowerBound, upperBound, txsPerSec, processTime);
        }
    }

    private double avg(List<Long> l) {
        return l.stream().mapToLong(Long::longValue).average().getAsDouble();
    }
}
