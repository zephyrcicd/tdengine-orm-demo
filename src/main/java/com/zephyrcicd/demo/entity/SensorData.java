package com.zephyrcicd.demo.entity;

import com.zephyrcicd.tdengineorm.annotation.TdColumn;
import com.zephyrcicd.tdengineorm.annotation.TdTable;
import com.zephyrcicd.tdengineorm.annotation.TdTag;
import com.zephyrcicd.tdengineorm.enums.TdFieldTypeEnum;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 传感器数据实体类
 * 展示 TDengine Super Table 的使用
 *
 * @author zephyr
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TdTable(value = "sensors", comment = "传感器超级表")
public class SensorData {

    /**
     * TAG 字段：设备ID
     * 用于区分不同的传感器设备，自动创建子表
     */
    @TdTag
    @TdColumn(value = "device_id", length = 50)
    private String deviceId;

    /**
     * TAG 字段：设备位置
     */
    @TdTag
    @TdColumn(value = "location", length = 100)
    private String location;

    /**
     * TAG 字段：设备类型
     */
    @TdTag
    @TdColumn(value = "device_type", length = 50)
    private String deviceType;

    /**
     * 时间戳（主键）
     */
    @TdColumn(value = "ts", type = TdFieldTypeEnum.TIMESTAMP)
    private Long ts;

    /**
     * 温度数据
     */
    @TdColumn(value = "temperature", type = TdFieldTypeEnum.DOUBLE)
    private Double temperature;

    /**
     * 湿度数据
     */
    @TdColumn(value = "humidity", type = TdFieldTypeEnum.DOUBLE)
    private Double humidity;

    /**
     * 电压
     */
    @TdColumn(value = "voltage", type = TdFieldTypeEnum.FLOAT)
    private Float voltage;

    /**
     * 设备状态（0:正常, 1:告警, 2:故障）
     */
    @TdColumn(value = "status", type = TdFieldTypeEnum.TINYINT)
    private Integer status;

    /**
     * 备注信息
     */
    @TdColumn(value = "remark", length = 200)
    private String remark;
}