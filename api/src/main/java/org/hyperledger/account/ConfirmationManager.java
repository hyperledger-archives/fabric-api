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

import org.hyperledger.api.*;
import org.hyperledger.common.BID;
import org.hyperledger.common.Block;
import org.hyperledger.common.Outpoint;
import org.hyperledger.common.TransactionInput;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

public class ConfirmationManager implements TrunkListener {
    private static final Logger log = LoggerFactory.getLogger(ConfirmationManager.class);

    private final LinkedList<BID> trunk = new LinkedList<>();

    private int height;

    private final Map<BID, Set<APITransaction>> monitored = new HashMap<>();
    private final Map<Outpoint, APITransaction> spending = new HashMap<>();

    private final Set<ConfirmationListener> confirmationListener = new HashSet<>();

    public synchronized void init(BCSAPI api, int trunkLength, List<BID> inventory) throws BCSAPIException {
        trunk.clear();
        if (inventory != null) {
            Collections.copy(trunk, inventory);
        }
        api.catchUp(trunk, trunkLength, true, this);
        height = api.getChainHeight();
        api.registerTrunkListener(this);
    }

    public synchronized void init(BCSAPI api, int trunkLength) throws BCSAPIException {
        init(api, trunkLength, null);
    }

    public synchronized int getHeight() {
        return height;
    }

    @Override
    public synchronized void trunkUpdate(List<APIBlock> added) {
        Set<APITransaction> unconfirmedTransactions = new HashSet<>();
        Block first = added.get(0);

        if (!trunk.isEmpty() && !trunk.getFirst().equals(first.getPreviousID())) {
            log.trace("Chain reorg through {}", first.getID());
            if (trunk.contains(first.getPreviousID())) {
                do {
                    BID removed = trunk.removeFirst();
                    log.trace("Removing block {}", removed);
                    if (monitored.containsKey(removed))
                        unconfirmedTransactions.addAll(monitored.get(removed));
                } while (!first.getPreviousID().equals(trunk.getFirst()));
            } else {
                log.trace("Removing all blocks");
                trunk.clear();
                for (Set<APITransaction> m : monitored.values())
                    unconfirmedTransactions.addAll(m);
                monitored.clear();
            }
        }
        for (APIBlock b : added) {
            trunk.addFirst(b.getID());
            log.trace("New highest block {}", trunk.getFirst());
            if (b.getTransactions() != null) {
                Map<Outpoint, APITransaction> tomonitor = new HashMap<>();
                for (APITransaction t : b.getTransactions()) {
                    unconfirmedTransactions.remove(t);
                    for (ConfirmationListener l : confirmationListener) {
                        l.confirmed(t);
                    }
                    Map<APITransaction, APITransaction> ds = new HashMap<>();
                    for (TransactionInput i : t.getInputs()) {
                        if (spending.containsKey(i.getSource()) &&
                                !spending.get(i.getSource()).equals(t)) {
                            ds.put(spending.get(i.getSource()), t);
                        }
                    }
                    for (Map.Entry<APITransaction, APITransaction> e : ds.entrySet()) {
                        for (ConfirmationListener l : confirmationListener) {
                            l.doubleSpent(e.getKey(), e.getValue());
                        }
                    }
                    for (ConfirmationListener l : confirmationListener) {
                        if (l.isKnownTransaction(t)) {
                            for (TransactionInput i : t.getInputs()) {
                                tomonitor.put(i.getSource(), t);
                            }
                        }
                    }

                }
                if (!tomonitor.isEmpty()) {
                    monitored.put(b.getID(), new HashSet<>(tomonitor.values()));
                    spending.putAll(tomonitor);
                }
            }
            height = b.getHeight();
        }

        for (APITransaction n : unconfirmedTransactions) {
            log.trace("un-confirmed {}", n.getID());
            for (ConfirmationListener l : confirmationListener) {
                l.unconfirmed(n);
            }
        }
    }

    public synchronized void addConfirmationListener(ConfirmationListener listener) {
        confirmationListener.add(listener);
    }

    public synchronized void removeConfirmationListener(ConfirmationListener listener) {
        confirmationListener.remove(listener);
    }

    /**
     * The height of the block, or -1 if we have not seen it
     */
    public synchronized int getBlockHeight(BID bid) {
        int i = trunk.indexOf(bid);
        if (i < 0) return -1;
        return height - i;
    }
}
