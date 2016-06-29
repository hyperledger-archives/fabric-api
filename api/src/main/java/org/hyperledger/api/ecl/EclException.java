package org.hyperledger.api.ecl;

// extending RuntimeException so that catching is not mandatory
// it is still valid to use them in throws clauses
public class EclException extends RuntimeException {

    String errorMessage;

}
