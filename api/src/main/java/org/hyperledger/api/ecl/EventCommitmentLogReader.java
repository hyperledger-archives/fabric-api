package org.hyperledger.api.ecl;

/**
 * Reader interface for the Event Commitment Log (ECL) component
 */
public interface EventCommitmentLogReader {

    BlindedHash<SignedCoreTransaction> lookupCoreTransaction(Version version, EventId eventId) throws EclException;

    Hash<StateCommitment> viewStateCommitment(Version version) throws EclException;

    Version viewCurrentVersion() throws EclException;

    // this method is really part of the peer-to-peer interface: this is to propagate new blocks
    void onNewBlock(SignedBlock listener) throws EclException;

}
