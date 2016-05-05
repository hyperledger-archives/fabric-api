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
package org.hyperledger.jackson;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.deser.std.StdScalarDeserializer;
import org.hyperledger.common.Outpoint;
import org.hyperledger.common.TID;

import java.io.IOException;

public class OutpointDeserializer extends StdScalarDeserializer<Outpoint> {
    protected OutpointDeserializer() {
        super(Outpoint.class);
    }

    @Override
    public Outpoint deserialize(JsonParser jp, DeserializationContext ctxt) throws IOException {
        JsonNode node = jp.readValueAsTree();
        TID h = new TID(node.get(0).asText());
        int ix = node.get(1).asInt();
        return new Outpoint(h, ix);
    }
}
