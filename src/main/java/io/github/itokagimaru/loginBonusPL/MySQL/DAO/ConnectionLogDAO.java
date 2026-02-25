package io.github.itokagimaru.loginBonusPL.MySQL.DAO;

import javax.sql.DataSource;
import java.sql.*;
import java.util.*;
import java.util.stream.Collectors;

public class ConnectionLogDAO {

    private final DataSource dataSource;
    private final String tableName;
    private final String uuidColumn;
    private final String ipColumn;

    public ConnectionLogDAO(DataSource dataSource,
                            String tableName,
                            String uuidColumn,
                            String ipColumn) throws SQLException {
        this.dataSource = dataSource;
        this.tableName = tableName;
        this.uuidColumn = uuidColumn;
        this.ipColumn = ipColumn;

        validateSchema();
    }

    /**
     * 1. uuidから含まれるIP一覧を取得
     */
    public List<String> findIpsByUuid(UUID uuid) throws SQLException {

        String sql = String.format(
                "SELECT DISTINCT %s FROM %s WHERE %s = ? AND %s IS NOT NULL",
                ipColumn, tableName, uuidColumn, ipColumn
        );

        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, uuid.toString());

            try (ResultSet rs = ps.executeQuery()) {

                List<String> result = new ArrayList<>();

                while (rs.next()) {
                    result.add(rs.getString(ipColumn));
                }

                return result;
            }
        }
    }

    /**
     * 2. ipからそのipが含まれるuuid一覧を取得
     */
    public List<UUID> findUuidsByIp(String ip) throws SQLException {

        String sql = String.format(
                "SELECT DISTINCT %s FROM %s WHERE %s = ? AND %s IS NOT NULL",
                uuidColumn, tableName, ipColumn, uuidColumn
        );

        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, ip);

            try (ResultSet rs = ps.executeQuery()) {

                List<UUID> result = new ArrayList<>();

                while (rs.next()) {
                    String uuidStr = rs.getString(uuidColumn);

                    try {
                        result.add(UUID.fromString(uuidStr));
                    } catch (IllegalArgumentException ignored) {
                        // 不正UUIDはスキップ
                    }
                }

                return result;
            }
        }
    }
    // 接続できるかのテスト
    public void validateSchema() throws SQLException {

        try (Connection conn = dataSource.getConnection()) {

            DatabaseMetaData metaData = conn.getMetaData();

            // ===== テーブル存在チェック =====
            boolean tableExists = false;

            try (ResultSet tables = metaData.getTables(
                    conn.getCatalog(),
                    null,
                    tableName,
                    new String[]{"TABLE"}
            )) {
                tableExists = tables.next();
            }

            if (!tableExists) {
                throw new IllegalStateException("Table not found: " + tableName);
            }

            // ===== カラム存在チェック =====
            Set<String> columns = new HashSet<>();

            try (ResultSet cols = metaData.getColumns(
                    conn.getCatalog(),
                    null,
                    tableName,
                    null
            )) {
                while (cols.next()) {
                    columns.add(cols.getString("COLUMN_NAME").toLowerCase());
                }
            }

            if (!columns.contains(uuidColumn.toLowerCase())) {
                throw new IllegalStateException("Column not found: " + uuidColumn);
            }

            if (!columns.contains(ipColumn.toLowerCase())) {
                throw new IllegalStateException("Column not found: " + ipColumn);
            }
        }
    }
}
