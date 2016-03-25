/* This file is part of VoltDB.
 * Copyright (C) 2008-2016 VoltDB Inc.
 *
 * Permission is hereby granted, free of charge, to any person obtaining
 * a copy of this software and associated documentation files (the
 * "Software"), to deal in the Software without restriction, including
 * without limitation the rights to use, copy, modify, merge, publish,
 * distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to
 * the following conditions:
 *
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.
 * IN NO EVENT SHALL THE AUTHORS BE LIABLE FOR ANY CLAIM, DAMAGES OR
 * OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE,
 * ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
 * OTHER DEALINGS IN THE SOFTWARE.
 */

package callcenter;

import java.util.NavigableMap;
import java.util.Random;
import java.util.TreeMap;

public class NetworkSadnessTransformer<T> {

    final CallCenterApp.CallCenterConfig config;
    final CallSimulator simulator;

    // random number generator with constant seed
    final Random rand = new Random(1);

    final NavigableMap<Long, T> queue = new TreeMap<>();

    // for zipf generation
    private final int size;
    private final double skew;
    private final double bottom;

    NetworkSadnessTransformer(CallCenterApp.CallCenterConfig config, CallSimulator simulator) {
        this.config = config;
        this.simulator = simulator;

        // zipf params and setup
        size = 1;
        skew = 1;
        double bottomtemp = 0;
        for (int i = 1; i < size; i++) {
            bottomtemp += (1 / Math.pow(i, this.skew));
        }
        bottom = bottomtemp; // because final
    }

    public int nextZipfDelay() {
        int value;
        double friquency = 0;
        double dice;

        value = rand.nextInt(size);
        friquency = (1.0d / Math.pow(value, this.skew)) / this.bottom;
        dice = rand.nextDouble();

        while(!(dice < friquency)) {
            value = rand.nextInt(size);
            friquency = (1.0d / Math.pow(value, this.skew)) / this.bottom;
            dice = rand.nextDouble();
        }

        return value;
    }

    void transformAndQueue(T event, long systemCurrentTimeMillis) {
        // if you're super unlucky, this blows up the stack
        if (rand.nextDouble() < 0.05) {
            // duplicate this message (note recursion means maybe more than duped)
            transformAndQueue(event, systemCurrentTimeMillis);
        }

        long delayms = nextZipfDelay();
        queue.put(systemCurrentTimeMillis + delayms, event);
    }

    @SuppressWarnings("unchecked")
    T next(long systemCurrentTimeMillis) {
        // drain all the waiting messages from the source (up to 10k)
        /*while (queue.size() < 10000) {
            T event = (T) simulator.next(systemCurrentTimeMillis);
            if (event == null) {
                break;
            }
            transformAndQueue(event, systemCurrentTimeMillis);
        }

        if ((queue.size() > 0) && (queue.firstKey() < systemCurrentTimeMillis)) {
            Entry<Long, T> eventEntry = queue.pollFirstEntry();
            return eventEntry.getValue();
        }

        return null;*/

        return (T) simulator.next(systemCurrentTimeMillis);
    }

}