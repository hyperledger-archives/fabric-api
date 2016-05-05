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
package org.hyperledger.account.color;

import org.hyperledger.common.color.Color;

import java.util.Comparator;

/** A Color comparator that sorts BITCOIN last, and considers anything else equal */
public class BitcoinComparator implements Comparator<Color> {
    public static final BitcoinComparator INSTANCE = new BitcoinComparator();
    @Override
    public int compare(Color o1, Color o2) {
        if (o1 == Color.BITCOIN) {
            if (o2 == Color.BITCOIN)
                return 0;
            else
                return 1;
        }
        else if (o2 == Color.BITCOIN) {
            return -1;
        }
        return 0;
    }
}
