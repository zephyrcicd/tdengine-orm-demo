package com.zephyrcicd.demo.entity;

import com.zephyrcicd.tdengineorm.annotation.TdColumn;
import com.zephyrcicd.tdengineorm.annotation.TdTable;
import com.zephyrcicd.tdengineorm.annotation.TdTag;
import com.zephyrcicd.tdengineorm.enums.TdFieldTypeEnum;

/**
 * 传感器数据实体类
 * 展示 TDengine Super Table 的使用
 *
 * @author zephyr
 */
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

    // 无参构造函数
    public SensorData() {
    }

    // 全参构造函数
    public SensorData(String deviceId, String location, String deviceType, Long ts,
                      Double temperature, Double humidity, Float voltage, Integer status, String remark) {
        this.deviceId = deviceId;
        this.location = location;
        this.deviceType = deviceType;
        this.ts = ts;
        this.temperature = temperature;
        this.humidity = humidity;
        this.voltage = voltage;
        this.status = status;
        this.remark = remark;
    }

    // Getter and Setter methods
    public String getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getDeviceType() {
        return deviceType;
    }

    public void setDeviceType(String deviceType) {
        this.deviceType = deviceType;
    }

    public Long getTs() {
        return ts;
    }

    public void setTs(Long ts) {
        this.ts = ts;
    }

    public Double getTemperature() {
        return temperature;
    }

    public void setTemperature(Double temperature) {
        this.temperature = temperature;
    }

    public Double getHumidity() {
        return humidity;
    }

    public void setHumidity(Double humidity) {
        this.humidity = humidity;
    }

    public Float getVoltage() {
        return voltage;
    }

    public void setVoltage(Float voltage) {
        this.voltage = voltage;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }

    // Builder pattern
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String deviceId;
        private String location;
        private String deviceType;
        private Long ts;
        private Double temperature;
        private Double humidity;
        private Float voltage;
        private Integer status;
        private String remark;

        public Builder deviceId(String deviceId) {
            this.deviceId = deviceId;
            return this;
        }

        public Builder location(String location) {
            this.location = location;
            return this;
        }

        public Builder deviceType(String deviceType) {
            this.deviceType = deviceType;
            return this;
        }

        public Builder ts(Long ts) {
            this.ts = ts;
            return this;
        }

        public Builder temperature(Double temperature) {
            this.temperature = temperature;
            return this;
        }

        public Builder humidity(Double humidity) {
            this.humidity = humidity;
            return this;
        }

        public Builder voltage(Float voltage) {
            this.voltage = voltage;
            return this;
        }

        public Builder status(Integer status) {
            this.status = status;
            return this;
        }

        public Builder remark(String remark) {
            this.remark = remark;
            return this;
        }

        public SensorData build() {
            return new SensorData(deviceId, location, deviceType, ts,
                    temperature, humidity, voltage, status, remark);
        }
    }
}