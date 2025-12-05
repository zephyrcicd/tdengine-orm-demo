package com.zephyrcicd.demo.util;

import com.zephyrcicd.demo.entity.BenchmarkData;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * 压测数据生成器，支持带两个 TAG 的 BenchmarkData.
 */
public final class BenchmarkDataGenerator {

    private static final Random RANDOM = new Random();

    private BenchmarkDataGenerator() {
    }

    public static List<BenchmarkData> generate(String deviceId, String region, int count) {
        List<BenchmarkData> dataList = new ArrayList<>(count);
        long baseTs = System.currentTimeMillis();

        for (int i = 0; i < count; i++) {
            BenchmarkData data = BenchmarkData.builder()
                    .deviceId(deviceId)
                    .region(region)
                    .ts(baseTs - (count - i) * 1000L)
                    .temperature(15.0 + RANDOM.nextDouble() * 20.0)
                    .humidity(30.0 + RANDOM.nextDouble() * 50.0)
                    .build();
            dataList.add(data);
        }
        return dataList;
    }
}
