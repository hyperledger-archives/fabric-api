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
package org.hyperledger;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import org.hyperledger.common.SignatureOptions;
import org.hyperledger.common.WireFormatter;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * HyperLedger client API settings.
 */
public class HyperLedgerSettings {

    private final long timeout;
    private final Config configuration;
    private final boolean scivEnabled;
    private final boolean cltvEnabled;
    private final boolean scicEnabled;
    private final boolean naEnabled;
    private final boolean ctEnabled;
    private final boolean siglessTxIdEnabled;
    private final boolean enableNativeCryptoPrivateKeyGetPublic;
    private final boolean enableNativeCryptoPrivateKeySign;
    private final boolean enableNativeCryptoPublicKeyOffset;
    private final boolean enableNativeCryptoMasterPublicKeyGenerateKey;


    static HyperLedgerSettings instance = new HyperLedgerSettings(ConfigFactory.defaultReference());

    public static void initialize(Config config) {
        instance = new HyperLedgerSettings(config);
    }

    public static HyperLedgerSettings getInstance() {
        if (instance == null)
            throw new IllegalStateException("HyperLedgerSettings is not initialized");
        return instance;
    }

    public HyperLedgerSettings(Config config) {
        configuration = config.getConfig("hyperledger");
        scivEnabled = configuration.getBoolean("feature.sciv");
        scicEnabled = configuration.getBoolean("feature.scic");
        cltvEnabled = configuration.getBoolean("feature.cltv");
        ctEnabled = configuration.getBoolean("feature.ct");
        naEnabled = configuration.getBoolean("feature.native-assets");
        siglessTxIdEnabled = configuration.getBoolean("feature.siglessTxId");
        timeout = configuration.getDuration("client.timeout", TimeUnit.MILLISECONDS);
        enableNativeCryptoPrivateKeyGetPublic = configuration.getBoolean("crypto.enableNativeCryptoPrivateKeyGetPublic");
        enableNativeCryptoPrivateKeySign = configuration.getBoolean("crypto.enableNativeCryptoPrivateKeySign");
        enableNativeCryptoPublicKeyOffset = configuration.getBoolean("crypto.enableNativeCryptoPublicKeyOffset");
        enableNativeCryptoMasterPublicKeyGenerateKey = configuration.getBoolean("crypto.enableNativeCryptoMasterPublicKeyGenerateKey");
    }

    public Config getConfiguration() {
        return configuration;
    }

    public boolean isCltvEnabled() {
        return cltvEnabled;
    }

    public boolean isCtEnabled() {
        return ctEnabled;
    }

    public boolean isNaEnabled() {
        return naEnabled;
    }

    public long getTimeout() {
        return timeout;
    }

    public boolean isSiglessTxIdEnabled() {
        return siglessTxIdEnabled;
    }

    public boolean isEnableNativeCryptoPrivateKeyGetPublic() {
        return enableNativeCryptoPrivateKeyGetPublic;
    }

    public boolean isEnableNativeCryptoPrivateKeySign() {
        return enableNativeCryptoPrivateKeySign;
    }

    public boolean isEnableNativeCryptoPublicKeyOffset() {
        return enableNativeCryptoPublicKeyOffset;
    }

    public boolean isEnableNativeCryptoMasterPublicKeyGenerateKey() {
        return enableNativeCryptoMasterPublicKeyGenerateKey;
    }

    public SignatureOptions getSignatureOptions() {
        SignatureOptions.Builder builder = SignatureOptions.create();
        if (scivEnabled)
            builder.sciv();
        if (scicEnabled)
            builder.scic();
        return builder.build();
    }

    public WireFormatter getTxWireFormatter() {
        Set<WireFormatter.WireFormatFlags> formatFlags = new HashSet<>();
        if (naEnabled)
            formatFlags.add(WireFormatter.WireFormatFlags.NATIVE_ASSET);
        if (siglessTxIdEnabled)
            formatFlags.add(WireFormatter.WireFormatFlags.SIGLESS_TID);
        return new WireFormatter(formatFlags);
    }

}
