/**
 *
 *    Copyright 2016-2017, Optimizely and contributors
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */
package com.optimizely.ab;

import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicLong;

/**
 * {@link ThreadFactory} for providing Optimizely use-case specific naming.
 */
public class NamedThreadFactory implements ThreadFactory {

    private final String nameFormat;
    private final boolean daemon;

    private final ThreadFactory backingThreadFactory = Executors.defaultThreadFactory();
    private final AtomicLong threadCount = new AtomicLong(0);

    /**
     * @param nameFormat the thread name format which should include a string placeholder for the thread number
     * @param daemon whether the threads created should be {@link Thread#daemon}s or not
     */
    public NamedThreadFactory(String nameFormat, boolean daemon) {
        this.nameFormat = nameFormat;
        this.daemon = daemon;
    }

    @Override
    public Thread newThread(Runnable r) {
        Thread thread = backingThreadFactory.newThread(r);
        long threadNumber = threadCount.incrementAndGet();

        thread.setName(String.format(nameFormat, threadNumber));
        thread.setDaemon(daemon);
        return thread;
    }
}
