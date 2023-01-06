/*
 * Copyright 2022 VMware, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.micrometer.nativeimage.samples;

import java.io.File;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import io.micrometer.core.instrument.*;
import io.micrometer.core.instrument.binder.MeterBinder;
import io.micrometer.core.instrument.binder.jvm.*;
import io.micrometer.core.instrument.binder.system.FileDescriptorMetrics;
import io.micrometer.core.instrument.binder.system.ProcessorMetrics;
import io.micrometer.core.instrument.binder.system.UptimeMetrics;
import io.micrometer.core.instrument.binder.system.DiskSpaceMetrics;
import io.micrometer.core.instrument.observation.DefaultMeterObservationHandler;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationRegistry;

import static java.util.Collections.emptyList;

public class SimpleNativeImageSample {

    private final ObservationRegistry observationRegistry;

    private final SimpleMeterRegistry meterRegistry;

    private final ExecutorService executorService;

    private final List<MeterBinder> binders;

    public SimpleNativeImageSample() {
        this.observationRegistry = ObservationRegistry.create();
        this.meterRegistry = new SimpleMeterRegistry();
        this.executorService = Executors.newSingleThreadExecutor();
        this.binders = createBinders();

        observationRegistry.observationConfig().observationHandler(new DefaultMeterObservationHandler(meterRegistry));
        binders.forEach(binder -> binder.bindTo(meterRegistry));
        simulateTaskInExecutorService(); // for ExecutorServiceMetrics
    }

    public static void main(String[] args) {
        SimpleNativeImageSample sample = new SimpleNativeImageSample();
        sample.simulateRecordings();
        System.out.println(sample.getMeterRegistry().getMetersAsString());
        sample.shutdownNow();
    }

    public void simulateRecordings() {
        meterRegistry.counter("sample.counter").increment();
        DistributionSummary.builder("sample.distribution").register(meterRegistry).record(42);

        Timer.Sample sample = Timer.start(meterRegistry);
        sleep(100);
        sample.stop(Timer.builder("sample.timer").register(meterRegistry));

        Observation observation = Observation.start("sample.observation", observationRegistry);
        sleep(50);
        observation.event(Observation.Event.of("sample.event"));
        sleep(50);
        observation.stop();
    }

    public SimpleMeterRegistry getMeterRegistry() {
        return this.meterRegistry;
    }

    public void shutdownNow() {
        executorService.shutdownNow();
        for (MeterBinder meterBinder : binders) {
            if (meterBinder instanceof AutoCloseable binder) {
                try {
                    binder.close();
                }
                catch (Exception e) {
                    System.out.println("Unable to close " + meterBinder.getClass().getSimpleName());
                    e.printStackTrace();
                }
            }
        }

        meterRegistry.close();
    }

    private void sleep(long millis) {
        try {
            Thread.sleep(millis);
        }
        catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    private List<MeterBinder> createBinders() {
        // @formatter:off
        return List.of(
                new ClassLoaderMetrics(),
                new ExecutorServiceMetrics(executorService, "executor.sample", emptyList()),
                new JvmCompilationMetrics(),
                new JvmGcMetrics(),
                new JvmHeapPressureMetrics(),
                new JvmInfoMetrics(),
                new JvmMemoryMetrics(),
                new JvmThreadMetrics(),
                new DiskSpaceMetrics(new File(".")),
                new FileDescriptorMetrics(),
                new ProcessorMetrics(),
                new UptimeMetrics()
        );
        // @formatter:on
    }

    private void simulateTaskInExecutorService() {
        CountDownLatch latch = new CountDownLatch(1);
        executorService.submit(latch::countDown);
        try {
            latch.await(100, TimeUnit.MILLISECONDS);
        }
        catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

}
