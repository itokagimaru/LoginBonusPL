package io.github.itokagimaru.loginBonusPL.loginBonus;

import io.github.itokagimaru.loginBonusPL.MySQL.DAO.ConnectionLogDAO;

import java.sql.SQLException;
import java.util.*;

public class AltAccountService {
    ConnectionLogDAO connectionLogDAO;
    boolean enabled;

    public AltAccountService(ConnectionLogDAO connectionLogDAO, boolean enable) {
        this.connectionLogDAO = connectionLogDAO;
        this.enabled = enable;
    }
    public List<String> getIPList(UUID playerUUID) throws SQLException {
        return connectionLogDAO.findIpsByUuid(playerUUID);
    }

    public List<UUID> getAltAccountList(String ip) throws SQLException {
        return connectionLogDAO.findUuidsByIp(ip);
    }

    public List<UUID> getAltAccountList(UUID playerUUID) throws SQLException {
        List<UUID> altAccountList = new ArrayList<>();
        Set<UUID> checker = new HashSet<>(); //返すのは List がいいけど重複はまずいのでこの形
        for (String ip : getIPList(playerUUID)) {
            for (UUID uuid : getAltAccountList(ip)) {
                if (checker.add(uuid)) altAccountList.add(uuid);
            }
        }
        return altAccountList;
    }


    public boolean isEnabled() {
        return enabled;
    }
}
