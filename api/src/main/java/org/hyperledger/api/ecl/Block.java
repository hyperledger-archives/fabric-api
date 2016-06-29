package org.hyperledger.api.ecl;

import java.util.List;
import java.util.Optional;

public class Block {

    List<Transaction> transactions;

    Time time;

    Hash<StateCommitment> stateCommitmentHash;

    Optional<Hash<SignedBlock>> predecessor;

    Version version;

}
