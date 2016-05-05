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
package org.hyperledger.account;

public class PaymentOptions {
    public enum Priority {
        LOW, NORMAL, HIGH
    }

    public enum FeeSource {
        SENDER, RECEIVER
    }

    public enum FeeCalculation {
        FIXED, CALCULATED
    }

    public enum OutputOrder {
        FIXED, SHUFFLED
    }

    private final long fee;
    private final Priority priority;
    private final FeeSource source;
    private final FeeCalculation calculation;
    private final OutputOrder outputOrder;
    private final int change;

    public static PaymentOptions common = create().feeCalculation(FeeCalculation.CALCULATED).build();
    public static PaymentOptions fixedOutputOrder = create().feeCalculation(FeeCalculation.CALCULATED).outputOrder(OutputOrder.FIXED).build();
    public static PaymentOptions receiverPaysFee = create().feeCalculation(FeeCalculation.CALCULATED).outputOrder(OutputOrder.FIXED).feeSource(FeeSource.RECEIVER).build();

    public static Builder create() {
        return new Builder();
    }

    public static class Builder {
        private long fee = TransactionFactory.MINIMUM_FEE;
        private Priority priority = Priority.NORMAL;
        private FeeSource feeSource = FeeSource.SENDER;
        private FeeCalculation calculation = FeeCalculation.CALCULATED;
        private OutputOrder outputOrder = OutputOrder.FIXED;
        private int change = 1;

        public Builder options(PaymentOptions o) {
            this.fee = o.fee;
            this.priority = o.priority;
            this.feeSource = o.source;
            this.calculation = o.calculation;
            this.outputOrder = o.outputOrder;
            this.change = o.change;
            return this;
        }

        public Builder fee(long fee) {
            this.fee = fee;
            return this;
        }

        public Builder priority(Priority priority) {
            this.priority = priority;
            return this;
        }

        public Builder feeSource(FeeSource feeSource) {
            this.feeSource = feeSource;
            return this;
        }

        public Builder feeCalculation(FeeCalculation feeCalculation) {
            this.calculation = feeCalculation;
            return this;
        }

        public Builder outputOrder(OutputOrder outputOrder) {
            this.outputOrder = outputOrder;
            return this;
        }

        public Builder numberOfChanges(int change) {
            this.change = change;
            return this;
        }

        public PaymentOptions build() {
            return new PaymentOptions(fee, calculation, feeSource, priority, outputOrder, change);
        }
    }

    public PaymentOptions(long fee, FeeCalculation calculation, FeeSource source, Priority priority, OutputOrder outputOrder, int change) {
        this.fee = fee;
        this.calculation = calculation;
        this.source = source;
        this.priority = priority;
        this.outputOrder = outputOrder;
        this.change = change;
    }

    public boolean isCalculated() {
        return calculation == FeeCalculation.CALCULATED;
    }

    public boolean isPaidBySender() {
        return source == FeeSource.SENDER;
    }

    public boolean isLowPriority() {
        return priority == Priority.LOW;
    }

    public boolean isNormalPriority() {
        return priority == Priority.NORMAL;
    }

    public boolean isHighPriority() {
        return priority == Priority.HIGH;
    }

    public boolean isShuffled() {
        return outputOrder == OutputOrder.SHUFFLED;
    }

    public int getChange() {
        return change;
    }

    public long getFee() {
        return fee;
    }

    public Priority getPriority() {
        return priority;
    }

    public FeeSource getSource() {
        return source;
    }

    public FeeCalculation getCalculation() {
        return calculation;
    }

    public OutputOrder getOutputOrder() {
        return outputOrder;
    }

    @Override
    public String toString() {
        return "PaymentOptions [fee=" + fee + ", calculation=" + calculation + ", feeSource=" + source + ", priority=" + priority + ", outputOrder=" + outputOrder
                + ", change=" + change + "]";
    }

}
