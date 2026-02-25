package io.github.itokagimaru.loginBonusPL.loginBonus;

import java.time.LocalDate;
import java.util.UUID;

public class PlayerLoginProgress {

    private final UUID uuid;
    private final int eventId;
    private LocalDate lastLoginDate;
    private int continuousDays;
    private int totalLoginDays;

    public PlayerLoginProgress(UUID uuid,
                               int eventId,
                               LocalDate lastLoginDate,
                               int continuousDays,
                               int totalLoginDays) {
        this.uuid = uuid;
        this.eventId = eventId;
        this.lastLoginDate = lastLoginDate;
        this.continuousDays = continuousDays;
        this.totalLoginDays = totalLoginDays;
    }

    public UUID getUuid() { return uuid; }
    public int getEventId() { return eventId; }
    public LocalDate getLastLoginDate() { return lastLoginDate; }
    public int getContinuousDays() { return continuousDays; }
    public int getTotalLoginDays() { return totalLoginDays; }

    public void setContinuousDays(int continuousDays) {
        this.continuousDays = continuousDays;
    }
    public void addContinuousDays() {
        setContinuousDays(getContinuousDays() + 1);
    }
    public void setTotalLoginDays(int totalLoginDays) {
        this.totalLoginDays = totalLoginDays;
    }
    public void addTotalLoginDays() {
        setTotalLoginDays(getTotalLoginDays() + 1);
    }
    public void setLastLoginDate(LocalDate lastLoginDate) {
        this.lastLoginDate = lastLoginDate;
    }
}
