# TDengine ORM Boot Starter Demo

[English](README_EN.md) | [中文](README.md)

这是 `tdengine-orm-boot-starter` 的示例项目，通过简洁的测试用例展示了 ORM 框架的所有核心功能。

## 项目简介

本项目是一个极简的测试示例，演示了如何使用 TdTemplate 操作 TDengine 时序数据库：

- **一个实体类**：SensorData（传感器数据，展示超级表和TAG的使用）
- **一个测试类**：15个测试用例，覆盖所有核心功能
- **直接使用 TdTemplate**：无Service层，代码更清晰
- **开箱即用**：运行测试类即可看到所有功能演示

> 💡 **本项目是 [tdengine-orm-boot-starter](https://github.com/zephyrcicd/tdengine-orm-boot-starter) 的官方示例项目**
>
> 如果您还不了解 TDengine ORM Boot Starter，请先访问主项目查看详细文档。

## 技术栈

- Spring Boot 2.4.2
- TDengine ORM Boot Starter 1.3.0
- JUnit 5

## 快速开始

### 1. 环境准备

确保已安装并启动 TDengine：

```bash
# 启动 TDengine
sudo systemctl start taosd

# 创建数据库
taos
> CREATE DATABASE IF NOT EXISTS demo;
> USE demo;
```

### 2. 配置数据库连接

编辑 `src/main/resources/application.yml`：

```yaml
td-orm:
  enabled: true
  url: jdbc:TAOS://localhost:6030/demo
  username: root
  password: taosdata
  driver-class-name: com.taosdata.jdbc.TSDBDriver
  log-level: INFO
```

### 3. 运行测试

**方式一：使用 Maven**

```bash
mvn clean test
```

**方式二：在 IDE 中运行**

打开 `SensorDataTest.java`，右键选择 "Run Tests"

## 项目结构

```
src/
├── main/java/com/zephyrcicd/demo/
│   ├── TdOrmDemoApplication.java     # Spring Boot 启动类
│   ├── entity/
│   │   └── SensorData.java          # 传感器数据实体（Super Table）
│   └── util/
│       └── TestDataGenerator.java    # 测试数据生成工具
│
├── test/java/com/zephyrcicd/demo/
│   └── SensorDataTest.java          # 完整功能测试（15个测试）
│
└── main/resources/
    └── application.yml               # 配置文件
```

## 测试用例说明

### SensorDataTest - 15个核心功能测试

| 测试 | 说明 | 展示功能 |
|------|------|----------|
| 1. 创建超级表 | 自动创建传感器数据超级表 | `createStableTableIfNotExist()` |
| 2. 插入单条数据 | USING 语法插入，自动创建子表 | `insertUsing()` + 动态表名策略 |
| 3. 批量插入数据 | 批量插入 100 条数据到单个子表 | `batchInsertUsing()` |
| 4. 批量插入多设备 | 3个设备（北京、上海、广州）各 50 条数据 | 多子表批量插入 |
| 5. 查询最新数据 | 查询设备最新 10 条数据 | `list()` + orderBy + limit |
| 6. 条件查询 | 多条件组合查询 | 动态条件构建（eq/ge/le/gt） |
| 7. 分页查询 | 分页返回数据 | `page()` 方法 |
| 8. 聚合统计 | AVG/MAX/MIN 统计 | 聚合函数使用 |
| 9. 分组统计 | 按位置分组统计 | `groupBy()` + orderBy |
| 10. 统计数量 | 统计数据条数 | `count()` |
| 11. 查询单条 | 查询单条最新数据 | `getOne()` |
| 12. 插入告警数据 | 插入不同状态的数据 | 状态字段使用 |
| 13. 查询告警数据 | 查询特定状态数据 | `in()` 条件 |
| 14. 时间窗口查询 | 按小时统计平均值 | `intervalWindow()` 窗口函数 |
| 15. 分区时间窗口查询 | 按位置分区的时间窗口统计 | `partitionBy()` + `intervalWindow()` |

## 核心代码示例

### 1. 实体类定义（Super Table）

```java
@TdTable(value = "sensors", comment = "传感器超级表")
public class SensorData {
    @TdTag  // TAG 字段，用于子表分组
    @TdColumn(value = "device_id", length = 50)
    private String deviceId;

    @TdTag
    @TdColumn(value = "location", length = 100)
    private String location;

    @TdColumn(value = "ts", type = TdFieldTypeEnum.TIMESTAMP)
    private Long ts;  // 时间戳

    @TdColumn(value = "temperature", type = TdFieldTypeEnum.DOUBLE)
    private Double temperature;  // 温度

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

### 2. 直接使用 TdTemplate

```java
@SpringBootTest
class SensorDataTest {
    @Autowired
    private TdTemplate tdTemplate;

    // 定义动态表名策略
    private final DynamicNameStrategy<SensorData> strategy = entity ->
            "sensor_" + entity.getDeviceId();

    @Test
    void testInsert() {
        // 1. 创建超级表
        tdTemplate.createStableTableIfNotExist(SensorData.class);

        // 2. 插入数据（自动创建子表）
        SensorData data = SensorData.builder()
                .deviceId("device001")
                .ts(System.currentTimeMillis())
                .temperature(25.5)
                .build();
        tdTemplate.insertUsing(data, strategy);

        // 3. 查询数据
        TdQueryWrapper<SensorData> wrapper = TdWrappers.queryWrapper(SensorData.class)
                .selectAll()
                .eq(SensorData::getDeviceId, "device001")
                .orderByDesc("ts")
                .limit(10);
        List<SensorData> result = tdTemplate.list(wrapper);
    }
}
```

### 3. 条件查询

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

### 4. 聚合统计

```java
TdQueryWrapper<SensorData> wrapper = TdWrappers.queryWrapper(SensorData.class)
    .select("COUNT(*) as count",
            "AVG(temperature) as avg_temp",
            "MAX(temperature) as max_temp")
    .eq(SensorData::getDeviceId, "device001")
    .ge(SensorData::getTs, startTime);

List<Map<String, Object>> result = tdTemplate.list(wrapper, Map.class);
```

### 5. 时间窗口查询

```java
TdQueryWrapper<SensorData> wrapper = TdWrappers.queryWrapper(SensorData.class)
    .select("_wstart as window_start",
            "AVG(temperature) as avg_temp")
    .eq(SensorData::getDeviceId, "device001")
    .interval("1h")  // 每小时窗口
    .orderBy("window_start");

List<Map<String, Object>> hourlyData = tdTemplate.list(wrapper, Map.class);
```

## TdTemplate 核心方法

### 表操作
- `createStableTableIfNotExist(Class)` - 创建超级表

### 插入操作
- `insert(entity)` - 插入单条数据
- `insert(strategy, entity)` - 使用动态表名策略插入
- `insertUsing(entity, strategy)` - USING 语法插入（自动建表）
- `batchInsertUsing(entityClass, list, strategy, batchSize)` - 批量 USING 插入

### 查询操作
- `list(wrapper)` - 查询列表
- `list(wrapper, resultClass)` - 查询并转换为指定类型
- `getOne(wrapper)` - 查询单条
- `page(pageNo, pageSize, wrapper)` - 分页查询
- `count(wrapper)` - 统计数量

## 测试输出示例

```
========== 测试1: 创建超级表 ==========
✓ 超级表创建成功

========== 测试2: 插入单条数据 ==========
✓ 插入成功: deviceId=device001

========== 测试5: 查询最新数据 ==========
✓ 查询成功: 10 条
  - 温度: 25.5°C, 湿度: 60.0%, 时间: 1699200000000
  - 温度: 26.2°C, 湿度: 62.5%, 时间: 1699200001000
  ...

========== 测试8: 聚合统计查询 ==========
✓ 统计分析成功:
  数据条数: 150
  平均温度: 26.8°C
  最高温度: 34.2°C
  最低温度: 20.5°C
```

## 注意事项

1. **数据库连接**：确保 TDengine 服务正在运行
2. **测试顺序**：测试用例按 `@Order` 注解顺序执行
3. **时间戳字段**：使用 `Long` 类型存储毫秒级时间戳
4. **动态表名策略**：`DynamicNameStrategy` 用于生成子表名

## 常见问题

### 1. 连接失败

检查 TDengine 是否启动：
```bash
sudo systemctl status taosd
```

### 2. 数据库不存在

创建数据库：
```bash
taos
> CREATE DATABASE IF NOT EXISTS demo;
```

## 学习路径

1. **阅读实体类** - 理解 `@TdTable`、`@TdTag`、`@TdColumn` 注解
2. **查看测试类** - 学习 TdTemplate 的各种使用方式
3. **运行测试** - 运行 `SensorDataTest`，观察输出
4. **修改测试** - 尝试修改查询条件，查看不同结果

## 相关链接

- [TDengine ORM Starter GitHub](https://github.com/zephyrcicd/tdengine-orm-boot-starter)
- [TDengine 官方文档](https://docs.taosdata.com/)
- [Maven Central](https://central.sonatype.com/artifact/io.github.zephyrcicd/tdengine-orm-boot-starter)

## 许可证

MIT License