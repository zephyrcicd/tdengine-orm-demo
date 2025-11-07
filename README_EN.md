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
│   │   └── SensorData.java          # Sensor data entity (Super Table)
│   └── util/
│       └── TestDataGenerator.java    # Test data generator utility
│
├── test/java/com/zephyrcicd/demo/
│   └── SensorDataTest.java          # Complete feature tests (15 tests)
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