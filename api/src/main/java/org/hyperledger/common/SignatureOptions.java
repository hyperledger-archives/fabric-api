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
package org.hyperledger.common;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class SignatureOptions {

    private final Set<Option> options;

    public static final SignatureOptions COMMON = SignatureOptions.create().build();
    public static final SignatureOptions SCIV = SignatureOptions.create().sciv().build();
    public static final SignatureOptions SCIC = SignatureOptions.create().scic().build();

    public enum Option {
        SCIV, SCIC
    }

    public static class Builder {
        Set<Option> options = new HashSet<>();

        public Builder scic() {
            options.add(Option.SCIC);
            return this;
        }

        public Builder sciv() {
            options.add(Option.SCIV);
            return this;
        }

        public SignatureOptions build() {
            return new SignatureOptions(options);
        }
    }

    private SignatureOptions(Set<Option> options) {
        this.options = options;
    }

    public static Builder create() {
        return new Builder();
    }

    public boolean contains(Option o) {
        return options.contains(o);
    }

    @Override
    public String toString() {
        List<String> optionsList = new ArrayList<>();
        if (this.contains(Option.SCIV)) {
            optionsList.add("sciv");
        }
        if (this.contains(Option.SCIC)) {
            optionsList.add("scic");
        }
        if (optionsList.isEmpty()) {
            optionsList.add("normal");
        }
        return "SignatureOptions{" + String.join(",", optionsList) + "}";
    }
}
