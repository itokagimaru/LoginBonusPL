package io.github.itokagimaru.loginBonusPL.MySQL.DAO;

import javax.sql.DataSource;
import java.sql.*;
import java.util.*;

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
     uuidからサブアカウント一覧の取得
     */
    public List<UUID> findIAltAccountByUuid(UUID uuid) throws SQLException {

        String sql = String.format(
                "SELECT DISTINCT %s FROM %s WHERE %s IN ("
                        + "  SELECT DISTINCT %s FROM %s WHERE %s = ? AND %s IS NOT NULL"
                        + ") AND %s IS NOT NULL",
                uuidColumn, tableName, ipColumn,
                ipColumn, tableName, uuidColumn, ipColumn,
                uuidColumn
        );

        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, uuid.toString());

            try (ResultSet rs = ps.executeQuery()) {

                List<UUID> result = new ArrayList<>();

                while (rs.next()) {
                    try {
                        result.add(UUID.fromString(rs.getString(uuidColumn)));
                    } catch (IllegalArgumentException e) {
                        //不正なUUIDは無視
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
