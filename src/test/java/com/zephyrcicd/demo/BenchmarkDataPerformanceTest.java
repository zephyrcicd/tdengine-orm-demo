package com.zephyrcicd.demo;

import com.zephyrcicd.demo.entity.BenchmarkData;
import com.zephyrcicd.demo.util.BenchmarkDataGenerator;
import com.zephyrcicd.tdengineorm.annotation.TdTable;
import com.zephyrcicd.tdengineorm.strategy.DynamicNameStrategy;
import com.zephyrcicd.tdengineorm.template.TdTemplate;
import com.zephyrcicd.tdengineorm.wrapper.TdQueryWrapper;
import com.zephyrcicd.tdengineorm.wrapper.TdWrappers;
import org.junit.jupiter.api.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@SpringBootTest
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class BenchmarkDataPerformanceTest {

    private static final Logger log = LoggerFactory.getLogger(BenchmarkDataPerformanceTest.class);

    private static final String DIRECT_DEVICE_PREFIX = "direct";
    private static final String USING_DEVICE_PREFIX = "using";
    private static final String SUB_TABLE_PREFIX = "bench_";
    private static final String SUPER_TABLE_NAME = resolveSuperTableName();
    private static final String[] REGIONS = {"bj", "sh", "gz", "sz"};

    private static final int DEVICE_COUNT = getIntProperty("td.perf.deviceCount", 5);
    private static final int ROWS_PER_DEVICE = getIntProperty("td.perf.rowsPerDevice", 10_000);
    private static final int PARTITION_SIZE = getIntProperty("td.perf.partitionSize", 500);
    private static final int QUERY_LIMIT = getIntProperty("td.perf.queryLimit", 1000);
    private static final long QUERY_WINDOW_MS = TimeUnit.MINUTES.toMillis(getIntProperty("td.perf.queryWindowMinutes", 30));
    private static final long QUERY_WINDOW_LARGE_MS = TimeUnit.MINUTES.toMillis(getIntProperty("td.perf.queryWindowMinutesLarge", 6 * 60));
    private static final long WARM_UP_OFFSET_MS = TimeUnit.DAYS.toMillis(7);

    @Autowired
    private TdTemplate tdTemplate;

    private final DynamicNameStrategy<BenchmarkData> strategy =
            entity -> tableNameForDevice(entity.getDeviceId(), entity.getRegion());

    @Test
    @Order(1)
    @DisplayName("1. 指定子表插入 vs 超级表 USING 插入耗时对比")
    void compareInsertStrategies() {
        tdTemplate.createStableTableIfNotExist(BenchmarkData.class);

        List<BenchmarkData> directInsertData = generateBenchmarkData(DIRECT_DEVICE_PREFIX, DEVICE_COUNT, ROWS_PER_DEVICE);
        List<BenchmarkData> usingInsertData = generateBenchmarkData(USING_DEVICE_PREFIX, DEVICE_COUNT, ROWS_PER_DEVICE);

        warmUpSubTables(directInsertData);

        BenchmarkResult directResult = benchmark("指定子表批量插入", directInsertData,
                batch -> tdTemplate.batchInsert(batch, PARTITION_SIZE, strategy));

        BenchmarkResult usingResult = benchmark("超级表USING批量插入", usingInsertData,
                batch -> tdTemplate.batchInsertUsing(batch, PARTITION_SIZE, strategy));

        logSummary(directResult, usingResult);
    }

    @Test
    @Order(2)
    @DisplayName("2. 指定子表查询 vs 超级表 TAG 查询耗时对比")
    void compareQueryStrategies() {
        List<DeviceTag> targetDevices = buildDeviceTags(DIRECT_DEVICE_PREFIX);
        if (targetDevices.isEmpty()) {
            log.warn("未找到可用于查询压测的设备ID");
            return;
        }

        executeQuerySuite("短窗口", targetDevices, Math.max(QUERY_WINDOW_MS, TimeUnit.MINUTES.toMillis(1)), QUERY_LIMIT);
        executeQuerySuite("大窗口", targetDevices, Math.max(QUERY_WINDOW_LARGE_MS, TimeUnit.MINUTES.toMillis(60)), QUERY_LIMIT);
    }

    private void executeQuerySuite(String suiteName, List<DeviceTag> devices, long windowMs, int limit) {
        int effectiveLimit = Math.min(limit, ROWS_PER_DEVICE);
        long endTime = System.currentTimeMillis();
        long startTime = endTime - windowMs;

        QueryBenchmarkResult subTableResult = benchmarkQuery(suiteName + " - 指定子表名称查询", devices,
                tag -> querySubTable(tag, startTime, endTime, effectiveLimit));
        QueryBenchmarkResult superTableResult = benchmarkQuery(suiteName + " - 超级表 + TAG 条件查询", devices,
                tag -> querySuperTable(tag, startTime, endTime, effectiveLimit));

        QueryBenchmarkResult wrapperWithTag = benchmarkQuery(suiteName + " - TdWrapper + TAG 条件查询", devices,
                tag -> queryWithWrapper(tag, startTime, endTime, effectiveLimit, true));
        QueryBenchmarkResult wrapperWithoutTag = benchmarkQuery(suiteName + " - TdWrapper 不带TAG查询", devices,
                tag -> queryWithWrapper(tag, startTime, endTime, effectiveLimit, false));

        logQuerySummary(suiteName + "（子表 vs 超级表）", subTableResult, superTableResult);
        logQuerySummary(suiteName + "（TdWrapper）", wrapperWithTag, wrapperWithoutTag);
    }

    private List<BenchmarkData> generateBenchmarkData(String prefix, int deviceCount, int rowsPerDevice) {
        List<BenchmarkData> dataList = new ArrayList<>(deviceCount * rowsPerDevice);
        for (int i = 1; i <= deviceCount; i++) {
            DeviceTag tag = deviceTag(prefix, i);
            dataList.addAll(BenchmarkDataGenerator.generate(tag.getDeviceId(), tag.getRegion(), rowsPerDevice));
        }
        return dataList;
    }

    private List<DeviceTag> buildDeviceTags(String prefix) {
        List<DeviceTag> deviceIds = new ArrayList<>(DEVICE_COUNT);
        for (int i = 1; i <= DEVICE_COUNT; i++) {
            deviceIds.add(deviceTag(prefix, i));
        }
        return deviceIds;
    }

    private String formatDeviceId(String prefix, int index) {
        return String.format("%s_%03d", prefix, index);
    }

    private DeviceTag deviceTag(String prefix, int index) {
        String deviceId = formatDeviceId(prefix, index);
        String region = REGIONS[(index - 1) % REGIONS.length];
        return new DeviceTag(deviceId, region);
    }

    private String tableNameForDevice(String deviceId, String region) {
        return SUB_TABLE_PREFIX + deviceId + "_" + region;
    }

    private void validateTableName(String tableName) {
        if (tableName == null || !tableName.matches("[A-Za-z0-9_]+")) {
            throw new IllegalArgumentException("非法子表名称: " + tableName);
        }
    }

    private void warmUpSubTables(List<BenchmarkData> dataset) {
        Map<String, BenchmarkData> firstRecordPerDevice = dataset.stream()
                .collect(Collectors.toMap(BenchmarkData::getDeviceId, data -> data, (left, right) -> left, LinkedHashMap::new));

        firstRecordPerDevice.values().forEach(sample -> {
            long warmUpTs = (sample.getTs() != null ? sample.getTs() : System.currentTimeMillis()) - WARM_UP_OFFSET_MS;
            BenchmarkData warmUpRecord = BenchmarkData.builder()
                    .deviceId(sample.getDeviceId())
                    .region(sample.getRegion())
                    .ts(warmUpTs)
                    .temperature(sample.getTemperature())
                    .humidity(sample.getHumidity())
                    .build();
            tdTemplate.insertUsing(warmUpRecord, strategy);
        });
        log.info("子表预热完成：{} 个子表已通过 USING 语法创建。", firstRecordPerDevice.size());
    }

    private BenchmarkResult benchmark(String label, List<BenchmarkData> data, InsertExecutor executor) {
        log.info("\n--- [{}] 开始，数据量: {}，分批大小: {} ---", label, data.size(), PARTITION_SIZE);
        long start = System.nanoTime();
        int[] result = executor.execute(data);
        long end = System.nanoTime();

        double elapsedMs = (end - start) / 1_000_000.0;
        int affectedRows = Arrays.stream(result).sum();
        double throughput = elapsedMs > 0 ? affectedRows / (elapsedMs / 1000.0) : 0.0;
        BenchmarkResult metrics = new BenchmarkResult(label, affectedRows, elapsedMs, throughput, result.length);

        log.info("--- [{}] 完成：插入 {} 条，耗时 {} ms，吞吐量 {} 条/秒，SQL 批次数 {} ---",
                label,
                affectedRows,
                String.format("%.2f", metrics.getElapsedMs()),
                String.format("%.2f", metrics.getThroughput()),
                metrics.getSqlBatchCount());
        return metrics;
    }

    private QueryBenchmarkResult benchmarkQuery(String label, List<DeviceTag> devices, QueryExecutor executor) {
        log.info("\n--- [{}] 开始，设备数: {} ---", label, devices.size());
        long start = System.nanoTime();
        int totalRows = 0;
        for (DeviceTag device : devices) {
            totalRows += executor.query(device);
        }
        long end = System.nanoTime();

        double elapsedMs = (end - start) / 1_000_000.0;
        double qps = elapsedMs > 0 ? devices.size() / (elapsedMs / 1000.0) : 0.0;
        QueryBenchmarkResult result = new QueryBenchmarkResult(label, devices.size(), totalRows, elapsedMs, qps);
        log.info("--- [{}] 完成：查询 {} 次，总返回 {} 条，耗时 {} ms，QPS {} ---",
                label,
                result.getQueryCount(),
                result.getTotalRows(),
                String.format("%.2f", result.getElapsedMs()),
                String.format("%.2f", result.getQueriesPerSecond()));
        return result;
    }

    private int querySubTable(DeviceTag deviceTag, long startTime, long endTime, int limit) {
        String tableName = tableNameForDevice(deviceTag.getDeviceId(), deviceTag.getRegion());
        validateTableName(tableName);
        Map<String, Object> params = new HashMap<>();
        params.put("start", startTime);
        params.put("end", endTime);
        String sql = "SELECT ts, temperature, humidity FROM " + tableName
                + " WHERE ts BETWEEN :start AND :end ORDER BY ts DESC LIMIT " + limit;

        List<Map<String, Object>> result = tdTemplate.getNamedParameterJdbcTemplate().queryForList(sql, params);
        return result.size();
    }

    private int querySuperTable(DeviceTag deviceTag, long startTime, long endTime, int limit) {
        Map<String, Object> params = new HashMap<>();
        params.put("deviceId", deviceTag.getDeviceId());
        params.put("region", deviceTag.getRegion());
        params.put("start", startTime);
        params.put("end", endTime);
        String sql = "SELECT ts, temperature, humidity FROM " + SUPER_TABLE_NAME
                + " WHERE device_id = :deviceId AND region = :region AND ts BETWEEN :start AND :end ORDER BY ts DESC LIMIT " + limit;
        return tdTemplate.getNamedParameterJdbcTemplate().queryForList(sql, params).size();
    }

    private int queryWithWrapper(DeviceTag deviceTag, long startTime, long endTime, int limit, boolean includeTagFilter) {
        TdQueryWrapper<BenchmarkData> wrapper = TdWrappers.queryWrapper(BenchmarkData.class)
                .select(BenchmarkData::getTs)
                .select(BenchmarkData::getTemperature)
                .select(BenchmarkData::getHumidity)
                .between("ts", startTime, endTime)
                .orderByDesc(BenchmarkData::getTs)
                .limit(limit);
        if (includeTagFilter) {
            wrapper.eq(BenchmarkData::getDeviceId, deviceTag.getDeviceId())
                    .eq(BenchmarkData::getRegion, deviceTag.getRegion());
        }
        return tdTemplate.list(wrapper).size();
    }

    private void logSummary(BenchmarkResult direct, BenchmarkResult using) {
        log.info("\n========== 插入方式耗时对比 ==========");
        log.info("{} => {} 条, {} ms, {} 条/秒",
                direct.getLabel(),
                direct.getTotalRows(),
                String.format("%.2f", direct.getElapsedMs()),
                String.format("%.2f", direct.getThroughput()));
        log.info("{} => {} 条, {} ms, {} 条/秒",
                using.getLabel(),
                using.getTotalRows(),
                String.format("%.2f", using.getElapsedMs()),
                String.format("%.2f", using.getThroughput()));

        if (direct.getElapsedMs() > 0 && using.getElapsedMs() > 0) {
            double ratio = using.getElapsedMs() / direct.getElapsedMs();
            log.info("USING/子表 耗时比例: {}", String.format("%.2f", ratio));
        }
    }

    private void logQuerySummary(String title, QueryBenchmarkResult first, QueryBenchmarkResult second) {
        log.info("\n========== {} ==========", title);
        log.info("{} => 查询 {} 次, 返回 {} 条, {} ms, {} QPS",
                first.getLabel(),
                first.getQueryCount(),
                first.getTotalRows(),
                String.format("%.2f", first.getElapsedMs()),
                String.format("%.2f", first.getQueriesPerSecond()));
        log.info("{} => 查询 {} 次, 返回 {} 条, {} ms, {} QPS",
                second.getLabel(),
                second.getQueryCount(),
                second.getTotalRows(),
                String.format("%.2f", second.getElapsedMs()),
                String.format("%.2f", second.getQueriesPerSecond()));

        if (first.getElapsedMs() > 0 && second.getElapsedMs() > 0) {
            double ratio = second.getElapsedMs() / first.getElapsedMs();
            log.info("{} / {} 耗时比例: {}", second.getLabel(), first.getLabel(), String.format("%.2f", ratio));
        }
    }

    private static String resolveSuperTableName() {
        TdTable tdTable = BenchmarkData.class.getAnnotation(TdTable.class);
        if (tdTable != null && tdTable.value() != null && tdTable.value().trim().length() > 0) {
            return tdTable.value();
        }
        return BenchmarkData.class.getSimpleName().toLowerCase(Locale.ROOT);
    }

    private static int getIntProperty(String key, int defaultValue) {
        return Integer.getInteger(key, defaultValue);
    }

    @FunctionalInterface
    private interface InsertExecutor {
        int[] execute(List<BenchmarkData> data);
    }

    @FunctionalInterface
    private interface QueryExecutor {
        int query(DeviceTag deviceTag);
    }

    private static final class BenchmarkResult {
        private final String label;
        private final int totalRows;
        private final double elapsedMs;
        private final double throughput;
        private final int sqlBatchCount;

        private BenchmarkResult(String label, int totalRows, double elapsedMs, double throughput, int sqlBatchCount) {
            this.label = label;
            this.totalRows = totalRows;
            this.elapsedMs = elapsedMs;
            this.throughput = throughput;
            this.sqlBatchCount = sqlBatchCount;
        }

        private String getLabel() {
            return label;
        }

        private int getTotalRows() {
            return totalRows;
        }

        private double getElapsedMs() {
            return elapsedMs;
        }

        private double getThroughput() {
            return throughput;
        }

        private int getSqlBatchCount() {
            return sqlBatchCount;
        }
    }

    private static final class QueryBenchmarkResult {
        private final String label;
        private final int queryCount;
        private final int totalRows;
        private final double elapsedMs;
        private final double queriesPerSecond;

        private QueryBenchmarkResult(String label, int queryCount, int totalRows, double elapsedMs, double queriesPerSecond) {
            this.label = label;
            this.queryCount = queryCount;
            this.totalRows = totalRows;
            this.elapsedMs = elapsedMs;
            this.queriesPerSecond = queriesPerSecond;
        }

        private String getLabel() {
            return label;
        }

        private int getQueryCount() {
            return queryCount;
        }

        private int getTotalRows() {
            return totalRows;
        }

        private double getElapsedMs() {
            return elapsedMs;
        }

        private double getQueriesPerSecond() {
            return queriesPerSecond;
        }
    }

    private static final class DeviceTag {
        private final String deviceId;
        private final String region;

        private DeviceTag(String deviceId, String region) {
            this.deviceId = deviceId;
            this.region = region;
        }

        private String getDeviceId() {
            return deviceId;
        }

        private String getRegion() {
            return region;
        }
    }
}
