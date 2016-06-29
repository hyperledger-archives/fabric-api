package org.hyperledger.api.ecl;

/**
 * Writer interface for the Event Commitment Log (ECL) component
 */
public interface EventCommitmentLogWriter {

    interface Either<A, B> {}

    void commitTransaction(BlindedHash<CoreTransaction> transaction, Either<EventDescriptor,
            EventSelector> selectorOrDescriptor, Time time) throws EclException;

}
