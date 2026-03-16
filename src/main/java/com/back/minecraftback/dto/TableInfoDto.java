package com.back.minecraftback.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TableInfoDto {
    private String tableName;
    private String schema;
    private long rowCount;
    private List<String> columns;
    private List<Map<String, Object>> sampleData;
    private Map<String, String> columnTypes;
}
