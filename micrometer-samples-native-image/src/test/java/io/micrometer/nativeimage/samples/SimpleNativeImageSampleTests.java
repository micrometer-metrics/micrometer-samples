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

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledInNativeImage;
import org.junit.jupiter.api.condition.EnabledInNativeImage;

import static org.assertj.core.api.Assertions.assertThat;

class SimpleNativeImageSampleTests {

    private SimpleNativeImageSample sample;

    @BeforeEach
    void setUp() {
        sample = new SimpleNativeImageSample();
    }

    @AfterEach
    void tearDown() {
        sample.shutdownNow();
    }

    @Test
    @DisabledInNativeImage
    void shouldHaveMetersOnJvm() {
        sample.simulateRecordings();
        // @formatter:off
        assertThat(sample.getMeterRegistry().getMetersAsString()).contains(
                "disk.free(GAUGE)",
                "disk.total(GAUGE)",
                "jvm.buffer.count(GAUGE)",
                "jvm.buffer.memory.used(GAUGE)",
                "jvm.buffer.total.capacity(GAUGE)",
                "jvm.classes.loaded(GAUGE)",
                "jvm.classes.unloaded(COUNTER)",
                "jvm.compilation.time(COUNTER)",
                "jvm.gc.live.data.size(GAUGE)",
                "jvm.gc.max.data.size(GAUGE)",
                "jvm.gc.memory.allocated(COUNTER)",
                "jvm.gc.memory.promoted(COUNTER)",
                "jvm.gc.overhead(GAUGE)",
                "jvm.info(GAUGE)",
                "jvm.memory.committed(GAUGE)",
                "jvm.memory.max(GAUGE)",
                "jvm.memory.usage.after.gc(GAUGE)",
                "jvm.memory.used(GAUGE)",
                "jvm.threads.daemon(GAUGE)",
                "jvm.threads.live(GAUGE)",
                "jvm.threads.peak(GAUGE)",
                "jvm.threads.states(GAUGE)",
                "process.cpu.usage(GAUGE)",
                "process.files.max(GAUGE)",
                "process.files.open(GAUGE)",
                "process.start.time(GAUGE)",
                "process.uptime(GAUGE)",
                "sample.counter(COUNTER)",
                "sample.distribution(DISTRIBUTION_SUMMARY)",
                "sample.observation(TIMER)",
                "sample.observation.active(LONG_TASK_TIMER)",
                "sample.observation.sample.event(COUNTER)",
                "sample.timer(TIMER)",
                "system.cpu.count(GAUGE)",
                "system.cpu.usage(GAUGE)",
                "system.load.average.1m(GAUGE)"
        );
        // @formatter:on
    }

    @Test
    @EnabledInNativeImage
    void shouldHaveMetersOnSubstrateVm() {
        sample.simulateRecordings();
        // @formatter:off
        assertThat(sample.getMeterRegistry().getMetersAsString()).contains(
                "disk.free(GAUGE)",
                "disk.total(GAUGE)",
                "jvm.classes.loaded(GAUGE)",
                "jvm.classes.unloaded(COUNTER)",
                "jvm.gc.overhead(GAUGE)",
                "jvm.info(GAUGE)",
                "jvm.threads.daemon(GAUGE)",
                "jvm.threads.live(GAUGE)",
                "jvm.threads.peak(GAUGE)",
                "process.cpu.usage(GAUGE)",
                "process.files.max(GAUGE)",
                "process.files.open(GAUGE)",
                "process.start.time(GAUGE)",
                "process.uptime(GAUGE)",
                "sample.counter(COUNTER)",
                "sample.distribution(DISTRIBUTION_SUMMARY)",
                "sample.observation(TIMER)",
                "sample.observation.active(LONG_TASK_TIMER)",
                "sample.observation.sample.event(COUNTER)",
                "sample.timer(TIMER)",
                "system.cpu.count(GAUGE)",
                "system.cpu.usage(GAUGE)",
                "system.load.average.1m(GAUGE)"
        ).doesNotContain(
                "jvm.buffer.count(GAUGE)",
                "jvm.buffer.memory.used(GAUGE)",
                "jvm.buffer.total.capacity(GAUGE)",
                "jvm.compilation.time(COUNTER)",
                "jvm.gc.live.data.size(GAUGE)",
                "jvm.gc.max.data.size(GAUGE)",
                "jvm.gc.memory.allocated(COUNTER)",
                "jvm.gc.memory.promoted(COUNTER)",
                "jvm.memory.committed(GAUGE)",
                "jvm.memory.max(GAUGE)",
                "jvm.memory.usage.after.gc(GAUGE)",
                "jvm.memory.used(GAUGE)",
                "jvm.threads.states(GAUGE)"
        );
        // @formatter:on
    }

}
