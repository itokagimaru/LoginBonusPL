package io.github.itokagimaru.loginBonusPL.loginBonus;

import io.github.itokagimaru.loginBonusPL.MySQL.DAO.ConnectionLogDAO;

import java.sql.SQLException;
import java.time.LocalDate;
import java.util.*;

public class AltAccountService {
    ConnectionLogDAO connectionLogDAO;
    boolean enabled;

    public AltAccountService(ConnectionLogDAO connectionLogDAO, boolean enable) {
        this.connectionLogDAO = connectionLogDAO;
        this.enabled = enable;
    }

    public List<UUID> getAltAccountList(UUID playerUUID) throws SQLException {
        return connectionLogDAO.findIAltAccountByUuid(playerUUID);
    }

    public boolean isEnabled() {
        return enabled;
    }
}
