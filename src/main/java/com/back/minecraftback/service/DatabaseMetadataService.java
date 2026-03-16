package com.back.minecraftback.service;

import com.back.minecraftback.dto.TableInfoDto;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Service;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DatabaseMetadataService {

    private static final String SCHEMA = "mc_backend";

    private final JdbcTemplate jdbcTemplate;

    public List<String> getAllTableNames() {
        String sql = """
            SELECT table_name
            FROM information_schema.tables
            WHERE table_schema = ?
            AND table_type = 'BASE TABLE'
            ORDER BY table_name
            """;
        return jdbcTemplate.query(sql, (rs, rowNum) -> rs.getString("table_name"), SCHEMA);
    }

    public TableInfoDto getTableInfo(String tableName) {
        if (tableName == null || !tableName.matches("^[a-zA-Z0-9_]+$")) {
            throw new IllegalArgumentException("Invalid table name");
        }
        List<String> allowed = getAllTableNames();
        if (!allowed.contains(tableName)) {
            throw new IllegalArgumentException("Table not found: " + tableName);
        }

        TableInfoDto info = new TableInfoDto();
        info.setTableName(tableName);
        info.setSchema(SCHEMA);

        Long rowCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM " + SCHEMA + "." + tableName, Long.class);
        info.setRowCount(rowCount != null ? rowCount : 0);

        String columnsSql = """
            SELECT column_name, data_type
            FROM information_schema.columns
            WHERE table_schema = ?
            AND table_name = ?
            ORDER BY ordinal_position
            """;
        List<Map<String, Object>> columnInfo = jdbcTemplate.query(columnsSql,
                (rs, rowNum) -> {
                    Map<String, Object> col = new HashMap<>();
                    col.put("name", rs.getString("column_name"));
                    col.put("type", rs.getString("data_type"));
                    return col;
                },
                SCHEMA, tableName);

        List<String> columns = new ArrayList<>();
        Map<String, String> columnTypes = new LinkedHashMap<>();
        for (Map<String, Object> col : columnInfo) {
            String colName = (String) col.get("name");
            String colType = (String) col.get("type");
            columns.add(colName);
            columnTypes.put(colName, colType);
        }
        info.setColumns(columns);
        info.setColumnTypes(columnTypes);

        if (info.getRowCount() > 0) {
            String sampleSql = "SELECT * FROM " + SCHEMA + "." + tableName + " LIMIT 10";
            List<Map<String, Object>> sampleData = jdbcTemplate.query(sampleSql, new RowMapper<Map<String, Object>>() {
                @Override
                public Map<String, Object> mapRow(ResultSet rs, int rowNum) throws java.sql.SQLException {
                    ResultSetMetaData metaData = rs.getMetaData();
                    int columnCount = metaData.getColumnCount();
                    Map<String, Object> row = new LinkedHashMap<>();
                    for (int i = 1; i <= columnCount; i++) {
                        String columnName = metaData.getColumnName(i);
                        Object value = rs.getObject(i);
                        if (value instanceof java.sql.Timestamp) {
                            row.put(columnName, value.toString());
                        } else if (value instanceof java.sql.Array) {
                            row.put(columnName, Arrays.toString((Object[]) ((java.sql.Array) value).getArray()));
                        } else {
                            row.put(columnName, value);
                        }
                    }
                    return row;
                }
            });
            info.setSampleData(sampleData);
        } else {
            info.setSampleData(new ArrayList<>());
        }

        return info;
    }

    public List<TableInfoDto> getAllTablesInfo() {
        return getAllTableNames().stream()
                .map(this::getTableInfo)
                .collect(Collectors.toList());
    }
}
