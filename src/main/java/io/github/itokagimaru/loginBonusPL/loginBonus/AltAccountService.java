package io.github.itokagimaru.loginBonusPL.loginBonus;

import io.github.itokagimaru.loginBonusPL.MySQL.DAO.ConnectionLogDAO;

import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.CompletableFuture;

public class AltAccountService {
    ConnectionLogDAO connectionLogDAO;
    boolean enabled;

    public AltAccountService(ConnectionLogDAO connectionLogDAO, boolean enable) {
        this.connectionLogDAO = connectionLogDAO;
        this.enabled = enable;
    }

    public CompletableFuture<List<UUID>> getAltAccountList(UUID playerUUID) {
        return connectionLogDAO.findIAltAccountByUuid(playerUUID);
    }

    public boolean isEnabled() {
        return enabled;
    }
}
