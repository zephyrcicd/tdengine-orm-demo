package com.zephyrcicd.demo;

import com.zephyrcicd.demo.entity.SensorData;
import com.zephyrcicd.demo.util.TestDataGenerator;
import com.zephyrcicd.tdengineorm.dto.Page;
import com.zephyrcicd.tdengineorm.enums.TdSelectFuncEnum;
import com.zephyrcicd.tdengineorm.strategy.DynamicNameStrategy;
import com.zephyrcicd.tdengineorm.template.TdTemplate;
import com.zephyrcicd.tdengineorm.wrapper.TdQueryWrapper;
import com.zephyrcicd.tdengineorm.wrapper.TdWrappers;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;
import java.util.Map;

/**
 * TDengine ORM 功能演示
 * 展示 TdTemplate 的各种使用方式
 *
 * @author zephyr
 */
@Slf4j
@SpringBootTest
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class SensorDataTest {

    @Autowired
    private TdTemplate tdTemplate;

    // 动态表名策略：根据设备ID生成子表名
    private final DynamicNameStrategy<SensorData> strategy = entity ->
            "sensor_" + entity.getDeviceId();

    @Test
    @Order(1)
    @DisplayName("1. 创建超级表")
    void testCreateSuperTable() {
        log.info("\n========== 测试1: 创建超级表 ==========");
        long startTime = System.nanoTime();

        tdTemplate.createStableTableIfNotExist(SensorData.class);

        long endTime = System.nanoTime();
        double elapsedMs = (endTime - startTime) / 1_000_000.0;
        log.info("✓ 超级表创建成功 [耗时: {}ms]", elapsedMs);
    }

    @Test
    @Order(2)
    @DisplayName("2. 插入单条数据（USING语法自动创建子表）")
    void testInsertUsing() {
        log.info("\n========== 测试2: 插入单条数据 ==========");

        SensorData data = SensorData.builder()
                .deviceId("device001")
                .location("北京机房")
                .deviceType("温湿度传感器")
                .ts(System.currentTimeMillis())
                .temperature(25.5)
                .humidity(60.0)
                .voltage(3.3f)
                .status(0)
                .remark("第一条数据")
                .build();

        long startTime = System.nanoTime();
        tdTemplate.insertUsing(data, strategy);
        long endTime = System.nanoTime();

        double elapsedMs = (endTime - startTime) / 1_000_000.0;
        log.info("✓ 插入成功: deviceId={} [耗时: {}ms]", data.getDeviceId(), elapsedMs);
    }

    @Test
    @Order(3)
    @DisplayName("3. 批量插入数据到单个子表")
    void testBatchInsertUsing() {
        log.info("\n========== 测试3: 批量插入数据 ==========");

        List<SensorData> dataList = TestDataGenerator.generateSensorData("device001", 100);

        long startTime = System.nanoTime();
        tdTemplate.batchInsertUsing(SensorData.class, dataList, strategy);
        long endTime = System.nanoTime();

        double elapsedMs = (endTime - startTime) / 1_000_000.0;
        double throughput = dataList.size() / (elapsedMs / 1000.0);
        log.info("✓ 批量插入成功: {} 条 [耗时: {}ms, 吞吐量: {} 条/秒]",
                dataList.size(), elapsedMs, throughput);
    }

    @Test
    @Order(4)
    @DisplayName("4. 批量插入多设备数据（不同位置）")
    void testBatchInsertMultiDevice() {
        log.info("\n========== 测试4: 批量插入多设备数据 ==========");

        // 使用新方法确保3个设备在3个不同位置（北京、上海、广州）
        List<SensorData> dataList = TestDataGenerator.generateMultiDeviceWithDifferentLocations(3, 50);

        long startTime = System.nanoTime();
        tdTemplate.batchInsertUsing(SensorData.class, dataList, strategy);
        long endTime = System.nanoTime();

        double elapsedMs = (endTime - startTime) / 1_000_000.0;
        double throughput = dataList.size() / (elapsedMs / 1000.0);
        log.info("✓ 批量插入成功: 3个设备（北京、上海、广州），每设备50条，共 {} 条 [耗时: {}ms, 吞吐量: {} 条/秒]",
                dataList.size(), elapsedMs, throughput);
    }

    @Test
    @Order(5)
    @DisplayName("5. 查询最新数据")
    void testQueryLatest() {
        log.info("\n========== 测试5: 查询最新数据 ==========");

        TdQueryWrapper<SensorData> wrapper = TdWrappers.queryWrapper(SensorData.class)
                .selectAll()
                .eq(SensorData::getDeviceId, "device001")
                .orderByDesc(SensorData::getTs)
                .limit(10);

        long startTime = System.nanoTime();
        List<SensorData> result = tdTemplate.list(wrapper);
        long endTime = System.nanoTime();

        double elapsedMs = (endTime - startTime) / 1_000_000.0;
        log.info("✓ 查询成功: {} 条 [耗时: {}ms]", result.size(), elapsedMs);
        result.stream().limit(3).forEach(data ->
                log.info("  - 温度: {}°C, 湿度: {}%, 时间: {}",
                        data.getTemperature(), data.getHumidity(), data.getTs()));
    }

    @Test
    @Order(6)
    @DisplayName("6. 条件查询")
    void testQueryByCondition() {
        log.info("\n========== 测试6: 条件查询 ==========");

        long endTime = System.currentTimeMillis();
        long startTime = endTime - 60 * 60 * 1000; // 最近1小时

        TdQueryWrapper<SensorData> wrapper = TdWrappers.queryWrapper(SensorData.class)
                .selectAll()
                .eq(SensorData::getDeviceId, "device001")
                .ge(SensorData::getTs, startTime)
                .le(SensorData::getTs, endTime)
                .gt(SensorData::getTemperature, 25.0)
                .orderByDesc(SensorData::getTs);

        long queryStartTime = System.nanoTime();
        List<SensorData> result = tdTemplate.list(wrapper);
        long queryEndTime = System.nanoTime();

        double elapsedMs = (queryEndTime - queryStartTime) / 1_000_000.0;
        log.info("✓ 条件查询成功: {} 条数据符合条件 [耗时: {}ms]", result.size(), elapsedMs);
        log.info("  查询条件: deviceId=device001, 温度>25°C, 时间范围=最近1小时");
    }

    @Test
    @Order(7)
    @DisplayName("7. 分页查询")
    void testQueryPage() {
        log.info("\n========== 测试7: 分页查询 ==========");

        TdQueryWrapper<SensorData> wrapper = TdWrappers.queryWrapper(SensorData.class)
                .selectAll()
                .eq(SensorData::getDeviceId, "device001")
                .orderByDesc(SensorData::getTs);

        long startTime = System.nanoTime();
        Page<SensorData> page = tdTemplate.page(1L, 20L, wrapper);
        long endTime = System.nanoTime();

        double elapsedMs = (endTime - startTime) / 1_000_000.0;
        log.info("✓ 分页查询成功 [耗时: {}ms]:", elapsedMs);
        log.info("  总记录数: {}", page.getTotal());
        log.info("  当前页: {}", page.getPageNo());
        log.info("  每页大小: {}", page.getPageSize());
        log.info("  当前页数据: {} 条", page.getDataList().size());
    }

    @Test
    @Order(8)
    @DisplayName("8. 聚合统计查询")
    void testAggregation() {
        log.info("\n========== 测试8: 聚合统计查询 ==========");

        long endTime = System.currentTimeMillis();
        long startTime = endTime - 24 * 60 * 60 * 1000; // 最近24小时

        TdQueryWrapper<SensorData> wrapper = TdWrappers.queryWrapper(SensorData.class)
                .selectFunc(TdSelectFuncEnum.AVG, SensorData::getTemperature, "avg_temp")
                .selectFunc(TdSelectFuncEnum.MAX, SensorData::getTemperature, "max_temp")
                .selectFunc(TdSelectFuncEnum.MIN, SensorData::getTemperature, "min_temp")
                .selectFunc(TdSelectFuncEnum.AVG, SensorData::getHumidity, "avg_humidity")
                .eq(SensorData::getDeviceId, "device001")
                .ge(SensorData::getTs, startTime)
                .le(SensorData::getTs, endTime);

        long queryStartTime = System.nanoTime();
        Map<String, Object> result = tdTemplate.getOneAsMap(wrapper);
        long queryEndTime = System.nanoTime();

        double elapsedMs = (queryEndTime - queryStartTime) / 1_000_000.0;
        if (!result.isEmpty()) {
            log.info("✓ 统计分析成功 [耗时: {}ms]:", elapsedMs);
            log.info("  平均温度: {}°C", result.get("avg_temp"));
            log.info("  最高温度: {}°C", result.get("max_temp"));
            log.info("  最低温度: {}°C", result.get("min_temp"));
            log.info("  平均湿度: {}%", result.get("avg_humidity"));
        }
    }

    @Test
    @Order(9)
    @DisplayName("9. 分组统计")
    void testGroupBy() {
        log.info("\n========== 测试9: 分组统计 ==========");

        long endTime = System.currentTimeMillis();
        long startTime = endTime - 24 * 60 * 60 * 1000;

        TdQueryWrapper<SensorData> wrapper = TdWrappers.queryWrapper(SensorData.class)
                .select(SensorData::getLocation)
                .selectFunc(TdSelectFuncEnum.AVG, SensorData::getTemperature, "avg_temp")
                .selectFunc(TdSelectFuncEnum.COUNT, "*", "c")
                .ge(SensorData::getTs, startTime)
                .le(SensorData::getTs, endTime)
                .groupBy(SensorData::getLocation)
                .orderByDesc("c");

        long queryStartTime = System.nanoTime();
        List<Map<String, Object>> result = tdTemplate.listAsMap(wrapper);
        long queryEndTime = System.nanoTime();

        double elapsedMs = (queryEndTime - queryStartTime) / 1_000_000.0;
        log.info("✓ 按位置分组统计成功: {} 个位置 [耗时: {}ms]", result.size(), elapsedMs);
        result.forEach(stat ->
                log.info("  位置: {}, 数据量: {}, 平均温度: {}°C",
                        stat.get("location"), stat.get("c"), stat.get("avg_temp")));
    }

    @Test
    @Order(10)
    @DisplayName("10. 统计数量")
    void testCount() {
        log.info("\n========== 测试10: 统计数量 ==========");

        long endTime = System.currentTimeMillis();
        long startTime = endTime - 24 * 60 * 60 * 1000;

        TdQueryWrapper<SensorData> wrapper = TdWrappers.queryWrapper(SensorData.class)
                .eq(SensorData::getDeviceId, "device001")
                .ge(SensorData::getTs, startTime)
                .le(SensorData::getTs, endTime);

        long queryStartTime = System.nanoTime();
        Long count = tdTemplate.count(wrapper);
        long queryEndTime = System.nanoTime();

        double elapsedMs = (queryEndTime - queryStartTime) / 1_000_000.0;
        log.info("✓ 统计成功: device001最近24小时共 {} 条数据 [耗时: {}ms]", count, elapsedMs);
    }

    @Test
    @Order(11)
    @DisplayName("11. 查询单条数据")
    void testQueryOne() {
        log.info("\n========== 测试11: 查询单条数据 ==========");

        TdQueryWrapper<SensorData> wrapper = TdWrappers.queryWrapper(SensorData.class)
                .selectAll()
                .eq(SensorData::getDeviceId, "device001")
                .orderByDesc(SensorData::getTs)
                .limit(1);

        long startTime = System.nanoTime();
        SensorData data = tdTemplate.getOne(wrapper);
        long endTime = System.nanoTime();

        double elapsedMs = (endTime - startTime) / 1_000_000.0;
        if (data != null) {
            log.info("✓ 查询成功: 最新数据 [耗时: {}ms]", elapsedMs);
            log.info("  设备ID: {}", data.getDeviceId());
            log.info("  温度: {}°C", data.getTemperature());
            log.info("  湿度: {}%", data.getHumidity());
            log.info("  时间: {}", data.getTs());
        }
    }

    @Test
    @Order(12)
    @DisplayName("12. 插入告警数据")
    void testInsertAlertData() {
        log.info("\n========== 测试12: 插入告警数据 ==========");

        List<SensorData> alertData = TestDataGenerator.generateAlertData("device001", 20);

        long startTime = System.nanoTime();
        tdTemplate.batchInsert(SensorData.class, alertData, strategy);
        long endTime = System.nanoTime();

        double elapsedMs = (endTime - startTime) / 1_000_000.0;
        log.info("✓ 插入告警数据成功: {} 条 [耗时: {}ms]", alertData.size(), elapsedMs);
    }

    @Test
    @Order(13)
    @DisplayName("13. 查询告警数据")
    void testQueryAlerts() {
        log.info("\n========== 测试13: 查询告警数据 ==========");

        long endTime = System.currentTimeMillis();
        long startTime = endTime - 24 * 60 * 60 * 1000;

        TdQueryWrapper<SensorData> wrapper = TdWrappers.queryWrapper(SensorData.class)
                .selectAll()
                .ge(SensorData::getTs, startTime)
                .le(SensorData::getTs, endTime)
                .in(SensorData::getStatus, 1, 2) // 1:告警, 2:故障
                .orderByDesc(SensorData::getTs);

        long queryStartTime = System.nanoTime();
        List<SensorData> alerts = tdTemplate.list(wrapper);
        long queryEndTime = System.nanoTime();

        double elapsedMs = (queryEndTime - queryStartTime) / 1_000_000.0;
        log.info("✓ 查询告警数据成功: {} 条 [耗时: {}ms]", alerts.size(), elapsedMs);
        alerts.stream().limit(5).forEach(data ->
                log.info("  - 设备: {}, 状态: {}, 温度: {}°C, 备注: {}",
                        data.getDeviceId(),
                        data.getStatus() == 1 ? "告警" : "故障",
                        data.getTemperature(),
                        data.getRemark()));
    }

    @Test
    @Order(14)
    @DisplayName("14. 时间窗口查询（每小时平均值）")
    void testTimeWindowQuery() {
        log.info("\n========== 测试14: 时间窗口查询 ==========");

        long endTime = System.currentTimeMillis();
        long startTime = endTime - 24 * 60 * 60 * 1000;

        TdQueryWrapper<SensorData> wrapper = TdWrappers.queryWrapper(SensorData.class)
                .select("_wstart as window_start",
                        "AVG(temperature) as avg_temp",
                        "AVG(humidity) as avg_humidity")
                .eq(SensorData::getDeviceId, "device001")
                .ge(SensorData::getTs, startTime)
                .le(SensorData::getTs, endTime)
                .intervalWindow("1h")
                .orderByAsc("window_start");

        long queryStartTime = System.nanoTime();
        List<Map<String, Object>> result = tdTemplate.listAsMap(wrapper);
        long queryEndTime = System.nanoTime();

        double elapsedMs = (queryEndTime - queryStartTime) / 1_000_000.0;
        log.info("✓ 时间窗口查询成功: {} 个时间窗口 [耗时: {}ms]", result.size(), elapsedMs);
        result.stream().limit(5).forEach(stat ->
                log.info("  窗口开始: {}, 平均温度: {}°C, 平均湿度: {}%",
                        stat.get("window_start"), stat.get("avg_temp"), stat.get("avg_humidity")));
    }

    @Test
    @Order(15)
    @DisplayName("15. 分区时间窗口查询（按位置分区）")
    void testPartitionByWithTimeWindow() {
        log.info("\n========== 测试15: 分区时间窗口查询 ==========");

        long endTime = System.currentTimeMillis();
        long startTime = endTime - 24 * 60 * 60 * 1000;

        TdQueryWrapper<SensorData> wrapper = TdWrappers.queryWrapper(SensorData.class)
                .select(SensorData::getLocation)
                .select("_wstart as window_start")
                .selectFunc(TdSelectFuncEnum.AVG, SensorData::getTemperature, "avg_temp")
                .selectFunc(TdSelectFuncEnum.AVG, SensorData::getHumidity, "avg_humidity")
                .selectFunc(TdSelectFuncEnum.COUNT, "*", "data_count")
                .ge(SensorData::getTs, startTime)
                .le(SensorData::getTs, endTime)
                .partitionBy(SensorData::getLocation)  // 按位置分区
                .intervalWindow("1h")                   // 每小时窗口
                .orderByAsc(SensorData::getLocation)
                .orderByAsc("window_start");

        long queryStartTime = System.nanoTime();
        List<Map<String, Object>> result = tdTemplate.listAsMap(wrapper);
        long queryEndTime = System.nanoTime();

        double elapsedMs = (queryEndTime - queryStartTime) / 1_000_000.0;
        log.info("✓ 分区时间窗口查询成功: {} 条记录 [耗时: {}ms]", result.size(), elapsedMs);
        log.info("  说明: 按位置分区，每个位置独立计算每小时的温湿度统计");

        result.stream().limit(10).forEach(stat ->
                log.info("  位置: {}, 窗口: {}, 平均温度: {}°C, 平均湿度: {}%, 数据量: {}",
                        stat.get("location"),
                        stat.get("window_start"),
                        stat.get("avg_temp"),
                        stat.get("avg_humidity"),
                        stat.get("data_count")));
    }

    @AfterAll
    static void afterAll() {
        log.info("\n========================================");
        log.info("TDengine ORM 功能演示完成！");
        log.info("========================================");
    }
}