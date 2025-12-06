package com.zephyrcicd.demo.entity;

import com.zephyrcicd.tdengineorm.annotation.TdColumn;
import com.zephyrcicd.tdengineorm.annotation.TdTable;
import com.zephyrcicd.tdengineorm.annotation.TdTag;
import com.zephyrcicd.tdengineorm.enums.TdFieldTypeEnum;
import lombok.Getter;
import lombok.Setter;

/**
 * 用于压测的超表实体，包含两个 TAG 字段。
 */
@Setter
@Getter
@TdTable(value = "perf_metrics", comment = "压测超级表，包含两个TAG")
public class BenchmarkData {

    @TdColumn(value = "ts", type = TdFieldTypeEnum.TIMESTAMP)
    private Long ts;

    @TdTag
    @TdColumn(value = "device_id", length = 64)
    private String deviceId;

    @TdTag
    @TdColumn(value = "region", length = 64)
    private String region;

    @TdColumn(value = "temperature", type = TdFieldTypeEnum.DOUBLE)
    private Double temperature;

    @TdColumn(value = "humidity", type = TdFieldTypeEnum.DOUBLE)
    private Double humidity;

    public BenchmarkData() {
    }

    public BenchmarkData(String deviceId, String region, Long ts, Double temperature, Double humidity) {
        this.deviceId = deviceId;
        this.region = region;
        this.ts = ts;
        this.temperature = temperature;
        this.humidity = humidity;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private String deviceId;
        private String region;
        private Long ts;
        private Double temperature;
        private Double humidity;

        private Builder() {
        }

        public Builder deviceId(String deviceId) {
            this.deviceId = deviceId;
            return this;
        }

        public Builder region(String region) {
            this.region = region;
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

        public BenchmarkData build() {
            return new BenchmarkData(deviceId, region, ts, temperature, humidity);
        }
    }
}
