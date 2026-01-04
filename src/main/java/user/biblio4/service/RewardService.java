package user.biblio4.service;

import user.biblio4.model.User;
import user.biblio4.model.UserRewardProgress;
import user.biblio4.repository.UserRepository;
import user.biblio4.repository.UserRewardProgressRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class RewardService {

    private final UserRepository userRepository;
    private final UserRewardProgressRepository rewardProgressRepository;

    /**
     * Obtenir ou créer la progression de récompense d'un utilisateur
     */
    private UserRewardProgress getOrCreateUserRewardProgress(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));

        return rewardProgressRepository.findByUserId(userId)
                .orElseGet(() -> {
                    UserRewardProgress progress = new UserRewardProgress(user);
                    return rewardProgressRepository.save(progress);
                });
    }

    @Transactional
    public Map<String, Object> awardLevelCompletion(Long userId, Integer levelNumber) {
        UserRewardProgress progress = getOrCreateUserRewardProgress(userId);

        int xpReward = levelNumber * 50;
        int coinReward = levelNumber * 20;
        String badge = "level_" + levelNumber + "_complete";

        progress.addXP(xpReward);
        progress.addCoins(coinReward);
        rewardProgressRepository.save(progress);

        Map<String, Object> reward = new HashMap<>();
        reward.put("type", "level_completion");
        reward.put("xp", xpReward);
        reward.put("coins", coinReward);
        reward.put("badge", badge);
        reward.put("message", "Félicitations ! Niveau " + levelNumber + " complété");
        reward.put("totalXP", progress.getTotalXP());
        reward.put("totalCoins", progress.getCoins());

        return reward;
    }

    @Transactional
    public Map<String, Object> awardQuizSuccess(Long userId, Integer levelNumber, Integer score) {
        UserRewardProgress progress = getOrCreateUserRewardProgress(userId);

        int baseXP = 100;
        int bonusXP = 0;

        if (score >= 90) {
            bonusXP = 50;
        } else if (score >= 80) {
            bonusXP = 30;
        } else if (score >= 70) {
            bonusXP = 10;
        }

        int totalXP = baseXP + bonusXP;
        int coins = 50 + (levelNumber * 10);
        String badge = getQuizBadge(score, levelNumber);

        progress.addXP(totalXP);
        progress.addCoins(coins);
        rewardProgressRepository.save(progress);

        Map<String, Object> reward = new HashMap<>();
        reward.put("type", "quiz_success");
        reward.put("score", score);
        reward.put("xp", totalXP);
        reward.put("bonusXP", bonusXP);
        reward.put("coins", coins);
        reward.put("badge", badge);
        reward.put("message", getQuizSuccessMessage(score));
        reward.put("totalXP", progress.getTotalXP());
        reward.put("totalCoins", progress.getCoins());

        return reward;
    }

    @Transactional
    public Map<String, Object> awardDailyStreak(Long userId, int streakDays) {
        UserRewardProgress progress = getOrCreateUserRewardProgress(userId);

        int baseCoins = 10;
        int streakBonus = Math.min(streakDays * 5, 50);
        int totalCoins = baseCoins + streakBonus;

        int xpReward = 20 + (streakDays * 2);

        progress.addCoins(totalCoins);
        progress.addXP(xpReward);

        // Mettre à jour le streak
        progress.setStreakDays(streakDays);
        progress.setLastLogin(LocalDateTime.now());
        progress.setLastDailyReward(LocalDateTime.now());

        rewardProgressRepository.save(progress);

        Map<String, Object> reward = new HashMap<>();
        reward.put("type", "daily_streak");
        reward.put("streakDays", streakDays);
        reward.put("coins", totalCoins);
        reward.put("streakBonus", streakBonus);
        reward.put("xp", xpReward);
        reward.put("message", "Jour " + streakDays + " consécutif ! Continuez comme ça !");
        reward.put("totalXP", progress.getTotalXP());
        reward.put("totalCoins", progress.getCoins());

        return reward;
    }

    @Transactional
    public Map<String, Object> awardLevelUnlock(Long userId, Integer levelNumber) {
        UserRewardProgress progress = getOrCreateUserRewardProgress(userId);

        int coinReward = levelNumber * 25;
        int xpReward = 50;

        progress.addCoins(coinReward);
        progress.addXP(xpReward);

        // Mettre à jour le niveau actuel
        if (levelNumber > progress.getCurrentLevel()) {
            progress.setCurrentLevel(levelNumber);
        }

        rewardProgressRepository.save(progress);

        Map<String, Object> reward = new HashMap<>();
        reward.put("type", "level_unlock");
        reward.put("levelNumber", levelNumber);
        reward.put("coins", coinReward);
        reward.put("xp", xpReward);
        reward.put("badge", "level_" + levelNumber + "_unlocked");
        reward.put("message", "Félicitations ! Niveau " + levelNumber + " débloqué");
        reward.put("currentLevel", progress.getCurrentLevel());
        reward.put("totalXP", progress.getTotalXP());
        reward.put("totalCoins", progress.getCoins());

        return reward;
    }

    @Transactional
    public Map<String, Object> awardWordMastery(Long userId, String wordKey) {
        UserRewardProgress progress = getOrCreateUserRewardProgress(userId);

        int xpReward = 25;
        int coinReward = 15;

        progress.addXP(xpReward);
        progress.addCoins(coinReward);
        rewardProgressRepository.save(progress);

        Map<String, Object> reward = new HashMap<>();
        reward.put("type", "word_mastery");
        reward.put("wordKey", wordKey);
        reward.put("xp", xpReward);
        reward.put("coins", coinReward);
        reward.put("message", "Excellent ! Mot maîtrisé : " + wordKey);
        reward.put("totalXP", progress.getTotalXP());
        reward.put("totalCoins", progress.getCoins());

        return reward;
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> getUserRewards(Long userId) {
        UserRewardProgress progress = rewardProgressRepository.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("Progression non trouvée"));

        List<Map<String, Object>> rewards = new ArrayList<>();

        // Ajouter les récompenses de niveaux complétés
        Integer currentLevel = progress.getCurrentLevel();
        if (currentLevel != null && currentLevel > 1) {
            for (int i = 1; i < currentLevel; i++) {
                Map<String, Object> reward = new HashMap<>();
                reward.put("type", "level_completion");
                reward.put("level", i);
                reward.put("date", LocalDateTime.now().minusDays(currentLevel - i));
                rewards.add(reward);
            }
        }

        // Ajouter les récompenses de streak
        Integer streakDays = progress.getStreakDays();
        if (streakDays != null && streakDays >= 7) {
            Map<String, Object> reward = new HashMap<>();
            reward.put("type", "weekly_streak");
            reward.put("streakDays", 7);
            reward.put("date", LocalDateTime.now());
            rewards.add(reward);
        }

        // Ajouter les récompenses de jalons XP
        Integer totalXP = progress.getTotalXP();
        if (totalXP != null && totalXP >= 1000) {
            Map<String, Object> reward = new HashMap<>();
            reward.put("type", "xp_milestone");
            reward.put("milestone", "1000_xp");
            reward.put("date", LocalDateTime.now());
            rewards.add(reward);
        }

        return rewards;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getUserProgress(Long userId) {
        UserRewardProgress progress = getOrCreateUserRewardProgress(userId);

        Map<String, Object> userProgress = new HashMap<>();
        userProgress.put("userId", userId);
        userProgress.put("totalXP", progress.getTotalXP());
        userProgress.put("coins", progress.getCoins());
        userProgress.put("currentLevel", progress.getCurrentLevel());
        userProgress.put("streakDays", progress.getStreakDays());
        userProgress.put("lastLogin", progress.getLastLogin());
        userProgress.put("lastDailyReward", progress.getLastDailyReward());

        return userProgress;
    }

    @Transactional(readOnly = true)
    public Integer getUserCoins(Long userId) {
        UserRewardProgress progress = rewardProgressRepository.findByUserId(userId)
                .orElseGet(() -> {
                    User user = userRepository.findById(userId)
                            .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));
                    return new UserRewardProgress(user);
                });

        return progress.getCoins();
    }

    @Transactional(readOnly = true)
    public Integer getUserXP(Long userId) {
        UserRewardProgress progress = rewardProgressRepository.findByUserId(userId)
                .orElseGet(() -> {
                    User user = userRepository.findById(userId)
                            .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));
                    return new UserRewardProgress(user);
                });

        return progress.getTotalXP();
    }

    @Transactional
    public boolean deductCoins(Long userId, int amount) {
        UserRewardProgress progress = getOrCreateUserRewardProgress(userId);

        if (progress.deductCoins(amount)) {
            rewardProgressRepository.save(progress);

            return true;
        }

        return false;
    }

    @Transactional
    public void addCoins(Long userId, int amount) {
        UserRewardProgress progress = getOrCreateUserRewardProgress(userId);

        progress.addCoins(amount);
        rewardProgressRepository.save(progress);

    }

    @Transactional
    public void addXP(Long userId, int amount) {
        UserRewardProgress progress = getOrCreateUserRewardProgress(userId);

        progress.addXP(amount);
        rewardProgressRepository.save(progress);

    }

    @Transactional
    public void updateCurrentLevel(Long userId, Integer level) {
        UserRewardProgress progress = getOrCreateUserRewardProgress(userId);

        progress.setCurrentLevel(level);
        rewardProgressRepository.save(progress);

    }

    @Transactional
    public void updateStreak(Long userId, Integer streakDays) {
        UserRewardProgress progress = getOrCreateUserRewardProgress(userId);

        progress.setStreakDays(streakDays);
        progress.setLastLogin(LocalDateTime.now());
        rewardProgressRepository.save(progress);

    }

    private String getQuizBadge(int score, int level) {
        if (score >= 95) {
            return "quiz_master_lvl_" + level;
        } else if (score >= 85) {
            return "quiz_expert_lvl_" + level;
        } else if (score >= 75) {
            return "quiz_pro_lvl_" + level;
        } else {
            return "quiz_pass_lvl_" + level;
        }
    }

    private String getQuizSuccessMessage(int score) {
        if (score >= 95) {
            return "Score légendaire ! " + score + "%";
        } else if (score >= 85) {
            return "Performance incroyable ! " + score + "%";
        } else if (score >= 75) {
            return "Bon travail ! " + score + "%";
        } else {
            return "Quiz réussi ! " + score + "%";
        }
    }
}