package org.hyperledger.api.ecl;

import java.util.List;

public class Transaction {

    List<Event> events;

    BlindedHash<SignedCoreTransaction> reference;

}
