package com.zephyrcicd.demo.util;

import com.zephyrcicd.demo.entity.SensorData;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * 测试数据生成工具
 *
 * @author zephyr
 */
public class TestDataGenerator {

    private static final Random RANDOM = new Random();
    private static final String[] LOCATIONS = {"北京机房", "上海机房", "广州机房", "深圳机房", "杭州机房"};
    private static final String[] DEVICE_TYPES = {"温湿度传感器", "压力传感器", "电流传感器", "烟雾传感器"};

    /**
     * 生成传感器数据
     *
     * @param deviceId 设备ID
     * @param count    数据条数
     * @return 传感器数据列表
     */
    public static List<SensorData> generateSensorData(String deviceId, int count) {
        List<SensorData> dataList = new ArrayList<>();
        long currentTime = System.currentTimeMillis();
        String location = LOCATIONS[RANDOM.nextInt(LOCATIONS.length)];
        String deviceType = DEVICE_TYPES[RANDOM.nextInt(DEVICE_TYPES.length)];

        for (int i = 0; i < count; i++) {
            SensorData data = SensorData.builder()
                    .deviceId(deviceId)
                    .location(location)
                    .deviceType(deviceType)
                    .ts(currentTime - (count - i) * 1000) // 每秒一条数据
                    .temperature(20.0 + RANDOM.nextDouble() * 15) // 20-35度
                    .humidity(40.0 + RANDOM.nextDouble() * 40) // 40-80%
                    .voltage(3.0f + RANDOM.nextFloat() * 0.5f) // 3.0-3.5V
                    .status(RANDOM.nextInt(10) > 8 ? 1 : 0) // 10%概率告警
                    .remark("测试数据")
                    .build();
            dataList.add(data);
        }

        return dataList;
    }

    /**
     * 生成多个设备的传感器数据
     *
     * @param deviceCount      设备数量
     * @param dataCountPerDevice 每个设备的数据条数
     * @return 传感器数据列表
     */
    public static List<SensorData> generateMultiDeviceSensorData(int deviceCount, int dataCountPerDevice) {
        List<SensorData> allData = new ArrayList<>();

        for (int i = 1; i <= deviceCount; i++) {
            String deviceId = String.format("device%03d", i);
            allData.addAll(generateSensorData(deviceId, dataCountPerDevice));
        }

        return allData;
    }

    /**
     * 生成告警数据
     *
     * @param deviceId 设备ID
     * @param count    数据条数
     * @return 传感器数据列表（包含告警）
     */
    public static List<SensorData> generateAlertData(String deviceId, int count) {
        List<SensorData> dataList = new ArrayList<>();
        long currentTime = System.currentTimeMillis();
        String location = LOCATIONS[RANDOM.nextInt(LOCATIONS.length)];
        String deviceType = DEVICE_TYPES[RANDOM.nextInt(DEVICE_TYPES.length)];

        for (int i = 0; i < count; i++) {
            int status = RANDOM.nextInt(3); // 0:正常, 1:告警, 2:故障
            SensorData data = SensorData.builder()
                    .deviceId(deviceId)
                    .location(location)
                    .deviceType(deviceType)
                    .ts(currentTime - (count - i) * 1000)
                    .temperature(status > 0 ? 35.0 + RANDOM.nextDouble() * 10 : 20.0 + RANDOM.nextDouble() * 15)
                    .humidity(40.0 + RANDOM.nextDouble() * 40)
                    .voltage(3.0f + RANDOM.nextFloat() * 0.5f)
                    .status(status)
                    .remark(status == 0 ? "正常" : status == 1 ? "温度告警" : "设备故障")
                    .build();
            dataList.add(data);
        }

        return dataList;
    }

    /**
     * 生成多个设备的传感器数据（确保每个设备在不同位置）
     * 适用于测试 PARTITION BY 按位置分区的场景
     *
     * @param deviceCount        设备数量（不超过LOCATIONS数组长度）
     * @param dataCountPerDevice 每个设备的数据条数
     * @return 传感器数据列表
     */
    public static List<SensorData> generateMultiDeviceWithDifferentLocations(int deviceCount, int dataCountPerDevice) {
        if (deviceCount > LOCATIONS.length) {
            throw new IllegalArgumentException("设备数量不能超过可用位置数量: " + LOCATIONS.length);
        }

        List<SensorData> allData = new ArrayList<>();
        long currentTime = System.currentTimeMillis();

        for (int i = 0; i < deviceCount; i++) {
            String deviceId = String.format("device%03d", i + 1);
            String location = LOCATIONS[i]; // 确保每个设备在不同位置
            String deviceType = DEVICE_TYPES[RANDOM.nextInt(DEVICE_TYPES.length)];

            for (int j = 0; j < dataCountPerDevice; j++) {
                SensorData data = SensorData.builder()
                        .deviceId(deviceId)
                        .location(location)
                        .deviceType(deviceType)
                        .ts(currentTime - (dataCountPerDevice - j) * 1000) // 每秒一条数据
                        .temperature(20.0 + RANDOM.nextDouble() * 15) // 20-35度
                        .humidity(40.0 + RANDOM.nextDouble() * 40) // 40-80%
                        .voltage(3.0f + RANDOM.nextFloat() * 0.5f) // 3.0-3.5V
                        .status(RANDOM.nextInt(10) > 8 ? 1 : 0) // 10%概率告警
                        .remark("测试数据")
                        .build();
                allData.add(data);
            }
        }

        return allData;
    }
}
