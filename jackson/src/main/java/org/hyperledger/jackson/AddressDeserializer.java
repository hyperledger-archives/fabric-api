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
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.deser.std.StdScalarDeserializer;
import org.hyperledger.account.UIAddress;
import org.hyperledger.common.HyperLedgerException;

import java.io.IOException;

public class AddressDeserializer extends StdScalarDeserializer<UIAddress> {
    protected AddressDeserializer() {
        super(UIAddress.class);
    }

    @Override
    public UIAddress deserialize(JsonParser jp, DeserializationContext ctxt) throws IOException {
        JsonToken t = jp.getCurrentToken();
        if (t == JsonToken.VALUE_STRING) {
            try {
                String satoshiStyle = jp.getText().trim();
                if (satoshiStyle.length() == 0) {
                    return null;
                }

                return UIAddress.fromSatoshiStyle(satoshiStyle);
            } catch (HyperLedgerException e) {
                throw JsonMappingException.from(jp, "Error deserializing address", e);
            }
        }

        throw ctxt.mappingException(handledType());
    }
}
