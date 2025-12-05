# TDengine ORM Boot Starter Demo

[English](README_EN.md) | [中文](README.md)

This is a demo project for `tdengine-orm-boot-starter`, showcasing all core features through concise test cases.

## Project Overview

This is a minimalist test demo that demonstrates how to use TdTemplate to operate TDengine time-series database:

- **One Entity Class**: SensorData (sensor data, demonstrating super table and TAG usage)
- **One Test Class**: 15 test cases covering all core features
- **Direct TdTemplate Usage**: No service layer, clearer code
- **Ready to Use**: Run the test class to see all feature demonstrations

> 💡 **This project is the official demo for [tdengine-orm-boot-starter](https://github.com/zephyrcicd/tdengine-orm-boot-starter)**
>
> If you're not familiar with TDengine ORM Boot Starter yet, please visit the main project for detailed documentation.

## Tech Stack

- Spring Boot 2.4.2
- TDengine ORM Boot Starter 1.3.0
- JUnit 5

## Quick Start

### 1. Environment Setup

Ensure TDengine is installed and running:

```bash
# Start TDengine
sudo systemctl start taosd

# Create database
taos
> CREATE DATABASE IF NOT EXISTS demo;
> USE demo;
```

### 2. Configure Database Connection

Edit `src/main/resources/application.yml`:

```yaml
td-orm:
  enabled: true
  url: jdbc:TAOS://localhost:6030/demo
  username: root
  password: taosdata
  driver-class-name: com.taosdata.jdbc.TSDBDriver
  log-level: INFO
```

### 3. Run Tests

**Option 1: Using Maven**

```bash
mvn clean test
```

**Option 2: Run in IDE**

Open `SensorDataTest.java`, right-click and select "Run Tests"

## Project Structure

```
src/
├── main/java/com/zephyrcicd/demo/
│   ├── TdOrmDemoApplication.java     # Spring Boot application
│   ├── entity/
│   │   ├── SensorData.java          # Sensor data entity (Super Table)
│   │   └── BenchmarkData.java       # Performance entity (two TAGs)
│   └── util/
│       ├── TestDataGenerator.java    # Functional data generator
│       └── BenchmarkDataGenerator.java # Performance data generator
│
├── test/java/com/zephyrcicd/demo/
│   ├── SensorDataTest.java          # Complete feature tests (15 tests)
│   └── SensorDataPerformanceTest.java # Insert/query performance tests
│
└── main/resources/
    └── application.yml               # Configuration file
```

## Test Case Description

### SensorDataTest - 15 Core Feature Tests

| Test | Description | Featured Function |
|------|-------------|-------------------|
| 1. Create Super Table | Auto-create sensor data super table | `createStableTableIfNotExist()` |
| 2. Insert Single Record | USING syntax insert, auto-create sub-table | `insertUsing()` + dynamic table name strategy |
| 3. Batch Insert Data | Batch insert 100 records to single sub-table | `batchInsertUsing()` |
| 4. Batch Insert Multi-Device | 3 devices (Beijing, Shanghai, Guangzhou), 50 records each | Multi sub-table batch insert |
| 5. Query Latest Data | Query device's latest 10 records | `list()` + orderBy + limit |
| 6. Conditional Query | Multi-condition combined query | Dynamic condition building (eq/ge/le/gt) |
| 7. Paginated Query | Paginated data return | `page()` method |
| 8. Aggregation Statistics | AVG/MAX/MIN statistics | Aggregation functions |
| 9. Grouped Statistics | Group by location statistics | `groupBy()` + orderBy |
| 10. Count Statistics | Count data records | `count()` |
| 11. Query Single Record | Query single latest record | `getOne()` |
| 12. Insert Alert Data | Insert data with different statuses | Status field usage |
| 13. Query Alert Data | Query specific status data | `in()` condition |
| 14. Time Window Query | Hourly average statistics | `intervalWindow()` window function |
| 15. Partitioned Time Window Query | Time window statistics partitioned by location | `partitionBy()` + `intervalWindow()` |

## Performance Benchmarks

`SensorDataPerformanceTest` uses a dedicated super table `BenchmarkData` (two TAGs: `device_id` and `region`) to measure insert/query throughput. Tune workloads via JVM properties, e.g.:

```bash
mvn test -Dtest=SensorDataPerformanceTest \
  -Dtd.perf.deviceCount=5 \
  -Dtd.perf.rowsPerDevice=10000 \
  -Dtd.perf.partitionSize=500 \
  -Dtd.perf.queryLimit=1000 \
  -Dtd.perf.queryWindowMinutes=30 \
  -Dtd.perf.queryWindowMinutesLarge=360
```

### Batch Insert

| Mode | Rows | Time (ms) | Throughput (rows/s) |
|------|------|-----------|---------------------|
| Direct sub-table (`batchInsert`) | 50,000 | 699.26 | 71,504 |
| Super table USING (`batchInsertUsing`) | 50,000 | 552.87 | 90,438 |

USING inserts were about 21% faster thanks to auto table creation and fewer round trips.

### Query

| Scenario | Query Type | Rows Returned | Time (ms) | QPS |
|----------|------------|---------------|-----------|-----|
| Short window (30 min) | Sub-table SQL | 5,000 | 174.71 | 28.62 |
|  | Super table + TAG SQL | 5,000 | 70.70 | 70.72 |
|  | TdWrapper + TAG | 5,000 | 181.28 | 27.58 |
|  | TdWrapper without TAG | 5,000 | 155.66 | 32.12 |
| Long window (6 h) | Sub-table SQL | 5,000 | 83.24 | 60.06 |
|  | Super table + TAG SQL | 5,000 | 86.56 | 57.77 |
|  | TdWrapper + TAG | 5,000 | 64.80 | 77.16 |
|  | TdWrapper without TAG | 5,000 | 74.75 | 66.89 |

Notes:

- Each device is truncated by `td.perf.queryLimit` (example above: 1,000 rows/device → 5,000 total). Increase the property to fetch more data.
- In smaller windows, super table + TAG queries clearly outperform sub-table SQL; for longer windows both stay within the same magnitude.
- When using `TdWrapper`, always pass all tag filters. Missing tags force a table scan and reduce throughput.

Benchmark logs also break down per-device SQL build vs JDBC execution time to help locate bottlenecks.

## Core Code Examples

### 1. Entity Class Definition (Super Table)

```java
@TdTable(value = "sensors", comment = "Sensor super table")
public class SensorData {
    @TdTag  // TAG field for sub-table grouping
    @TdColumn(value = "device_id", length = 50)
    private String deviceId;

    @TdTag
    @TdColumn(value = "location", length = 100)
    private String location;

    @TdColumn(value = "ts", type = TdFieldTypeEnum.TIMESTAMP)
    private Long ts;  // Timestamp

    @TdColumn(value = "temperature", type = TdFieldTypeEnum.DOUBLE)
    private Double temperature;  // Temperature

    // getter/setter methods...

    // Builder pattern
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        // Builder implementation...
    }
}
```

### 2. Direct TdTemplate Usage

```java
@SpringBootTest
class SensorDataTest {
    @Autowired
    private TdTemplate tdTemplate;

    // Define dynamic table name strategy
    private final DynamicNameStrategy<SensorData> strategy = entity ->
            "sensor_" + entity.getDeviceId();

    @Test
    void testInsert() {
        // 1. Create super table
        tdTemplate.createStableTableIfNotExist(SensorData.class);

        // 2. Insert data (auto-create sub-table)
        SensorData data = SensorData.builder()
                .deviceId("device001")
                .ts(System.currentTimeMillis())
                .temperature(25.5)
                .build();
        tdTemplate.insertUsing(data, strategy);

        // 3. Query data
        TdQueryWrapper<SensorData> wrapper = TdWrappers.queryWrapper(SensorData.class)
                .selectAll()
                .eq(SensorData::getDeviceId, "device001")
                .orderByDesc("ts")
                .limit(10);
        List<SensorData> result = tdTemplate.list(wrapper);
    }
}
```

### 3. Conditional Query

```java
TdQueryWrapper<SensorData> wrapper = TdWrappers.queryWrapper(SensorData.class)
    .selectAll()
    .eq(SensorData::getDeviceId, "device001")
    .ge(SensorData::getTs, startTime)
    .le(SensorData::getTs, endTime)
    .gt(SensorData::getTemperature, 25.0)
    .orderByDesc("ts");

List<SensorData> results = tdTemplate.list(wrapper);
```

### 4. Aggregation Statistics

```java
TdQueryWrapper<SensorData> wrapper = TdWrappers.queryWrapper(SensorData.class)
    .select("COUNT(*) as count",
            "AVG(temperature) as avg_temp",
            "MAX(temperature) as max_temp")
    .eq(SensorData::getDeviceId, "device001")
    .ge(SensorData::getTs, startTime);

List<Map<String, Object>> result = tdTemplate.list(wrapper, Map.class);
```

### 5. Time Window Query

```java
TdQueryWrapper<SensorData> wrapper = TdWrappers.queryWrapper(SensorData.class)
    .select("_wstart as window_start",
            "AVG(temperature) as avg_temp")
    .eq(SensorData::getDeviceId, "device001")
    .interval("1h")  // 1-hour window
    .orderBy("window_start");

List<Map<String, Object>> hourlyData = tdTemplate.list(wrapper, Map.class);
```

## TdTemplate Core Methods

### Table Operations
- `createStableTableIfNotExist(Class)` - Create super table

### Insert Operations
- `insert(entity)` - Insert single record
- `insert(strategy, entity)` - Insert with dynamic table name strategy
- `insertUsing(entity, strategy)` - USING syntax insert (auto-create table)
- `batchInsertUsing(entityClass, list, strategy, batchSize)` - Batch USING insert

### Query Operations
- `list(wrapper)` - Query list
- `list(wrapper, resultClass)` - Query and convert to specified type
- `getOne(wrapper)` - Query single record
- `page(pageNo, pageSize, wrapper)` - Paginated query
- `count(wrapper)` - Count statistics

## Test Output Example

```
========== Test 1: Create Super Table ==========
✓ Super table created successfully

========== Test 2: Insert Single Record ==========
✓ Insert successful: deviceId=device001

========== Test 5: Query Latest Data ==========
✓ Query successful: 10 records
  - Temperature: 25.5°C, Humidity: 60.0%, Time: 1699200000000
  - Temperature: 26.2°C, Humidity: 62.5%, Time: 1699200001000
  ...

========== Test 8: Aggregation Statistics ==========
✓ Statistics successful:
  Record count: 150
  Average temperature: 26.8°C
  Max temperature: 34.2°C
  Min temperature: 20.5°C
```

## Notes

1. **Database Connection**: Ensure TDengine service is running
2. **Test Order**: Test cases execute in order specified by `@Order` annotation
3. **Timestamp Field**: Use `Long` type for millisecond timestamps
4. **Dynamic Table Name Strategy**: `DynamicNameStrategy` generates sub-table names

## Common Issues

### 1. Connection Failure

Check if TDengine is running:
```bash
sudo systemctl status taosd
```

### 2. Database Not Exists

Create database:
```bash
taos
> CREATE DATABASE IF NOT EXISTS demo;
```

## Learning Path

1. **Read Entity Class** - Understand `@TdTable`, `@TdTag`, `@TdColumn` annotations
2. **View Test Class** - Learn various TdTemplate usage patterns
3. **Run Tests** - Run `SensorDataTest`, observe output
4. **Modify Tests** - Try modifying query conditions, observe different results

## Related Links

- [TDengine ORM Starter GitHub](https://github.com/zephyrcicd/tdengine-orm-boot-starter)
- [TDengine Official Documentation](https://docs.taosdata.com/)
- [Maven Central](https://central.sonatype.com/artifact/io.github.zephyrcicd/tdengine-orm-boot-starter)

## License

MIT License
