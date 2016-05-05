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

import com.fasterxml.jackson.databind.BeanDescription;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializationConfig;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.BeanSerializerModifier;
import org.hyperledger.HyperLedgerSettings;
import org.hyperledger.account.UIAddress;
import org.hyperledger.common.*;

import java.util.function.Supplier;

public class SupernodeModule extends SimpleModule {
    private static boolean isProduction = false;
    private static Supplier<Boolean> defaultIsProduction = () -> isProduction;
    private final WireFormatter formatter;

    public static void setIsProduction(boolean s) {
        isProduction = s;
    }

    public SupernodeModule() {
        this(defaultIsProduction);
    }

    public SupernodeModule(Supplier<Boolean> production) {
        super("Supernode");

        this.formatter = HyperLedgerSettings.getInstance().getTxWireFormatter();

        addDeserializer(MasterPrivateKey.class, new MasterPrivateKeyDeserializer());
        addDeserializer(MasterPublicKey.class, new MasterPublicKeyDeserializer());
        addDeserializer(Script.class, new ScriptDeserializer());
        addDeserializer(UIAddress.class, new AddressDeserializer());
        addDeserializer(Transaction.class, new TransactionDeserializer(formatter));
        addDeserializer(Hash.class, new HashDeserializer());
        addDeserializer(TID.class, new TIDDeserializer());
        addDeserializer(BID.class, new BIDDeserializer());

        addSerializer(MasterPrivateKey.class, new MasterPrivateKeySerializer());
        addSerializer(MasterPublicKey.class, new MasterPublicKeySerializer());
        addSerializer(Script.class, new ScriptSerializer());
        addSerializer(UIAddress.class, new AddressSerializer());
        addSerializer(Transaction.class, new TransactionSerializer());
        addSerializer(Outpoint.class, new OutpointSerializer());
        addSerializer(Hash.class, new HashSerializer());
        addSerializer(TID.class, new TIDSerializer());
        addSerializer(BID.class, new BIDSerializer());

        this.setSerializerModifier(new BeanSerializerModifier() {
            @Override
            public JsonSerializer<?> modifySerializer(SerializationConfig config,
                                                      BeanDescription beanDesc,
                                                      JsonSerializer<?> serializer) {
                if (serializer instanceof MasterPublicKeySerializer) {
                    MasterPublicKeySerializer.class.cast(serializer).setProduction(production.get());
                }
                if (serializer instanceof MasterPrivateKeySerializer) {
                    MasterPrivateKeySerializer.class.cast(serializer).setProduction(production.get());
                }

                return serializer;
            }
        });
    }
}
