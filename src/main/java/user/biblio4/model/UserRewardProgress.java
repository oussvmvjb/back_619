package user.biblio4.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "user_reward_progress")
public class UserRewardProgress {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @OneToOne
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;
    
    @Column(name = "total_xp", nullable = false)
    private Integer totalXP = 0;
    
    @Column(name = "coins", nullable = false)
    private Integer coins = 0;
    
    @Column(name = "current_level", nullable = false)
    private Integer currentLevel = 1;
    
    @Column(name = "streak_days", nullable = false)
    private Integer streakDays = 0;
    
    @Column(name = "last_login")
    private LocalDateTime lastLogin;
    
    @Column(name = "last_daily_reward")
    private LocalDateTime lastDailyReward;
    
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
    
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt = LocalDateTime.now();
    
    // Constructeurs
    public UserRewardProgress() {}
    
    public UserRewardProgress(User user) {
        this.user = user;
    }
    
    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
    
    // Getters et Setters
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public User getUser() {
        return user;
    }
    
    public void setUser(User user) {
        this.user = user;
    }
    
    public Integer getTotalXP() {
        return totalXP != null ? totalXP : 0;
    }
    
    public void setTotalXP(Integer totalXP) {
        this.totalXP = totalXP;
    }
    
    public Integer getCoins() {
        return coins != null ? coins : 0;
    }
    
    public void setCoins(Integer coins) {
        this.coins = coins;
    }
    
    public Integer getCurrentLevel() {
        return currentLevel != null ? currentLevel : 1;
    }
    
    public void setCurrentLevel(Integer currentLevel) {
        this.currentLevel = currentLevel;
    }
    
    public Integer getStreakDays() {
        return streakDays != null ? streakDays : 0;
    }
    
    public void setStreakDays(Integer streakDays) {
        this.streakDays = streakDays;
    }
    
    public LocalDateTime getLastLogin() {
        return lastLogin;
    }
    
    public void setLastLogin(LocalDateTime lastLogin) {
        this.lastLogin = lastLogin;
    }
    
    public LocalDateTime getLastDailyReward() {
        return lastDailyReward;
    }
    
    public void setLastDailyReward(LocalDateTime lastDailyReward) {
        this.lastDailyReward = lastDailyReward;
    }
    
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
    
    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
    
    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
    
    // Méthodes utilitaires
    public void addXP(int xp) {
        this.totalXP = getTotalXP() + xp;
    }
    
    public void addCoins(int coins) {
        this.coins = getCoins() + coins;
    }
    
    public boolean deductCoins(int coins) {
        if (getCoins() >= coins) {
            this.coins = getCoins() - coins;
            return true;
        }
        return false;
    }
    
    public void incrementLevel() {
        this.currentLevel = getCurrentLevel() + 1;
    }
    
    public void incrementStreak() {
        this.streakDays = getStreakDays() + 1;
    }
    
    public void resetStreak() {
        this.streakDays = 0;
    }
}