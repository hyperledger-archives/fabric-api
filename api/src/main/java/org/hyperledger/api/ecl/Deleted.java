package org.hyperledger.api.ecl;

import java.util.Set;

public class Deleted implements EventDescriptor {

    EventSelector eventSelector;

    Set<AParty> parties;

}
