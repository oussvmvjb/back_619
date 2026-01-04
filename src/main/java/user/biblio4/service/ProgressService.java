package user.biblio4.service;

import user.biblio4.model.UserProgress;
import user.biblio4.model.User;
import user.biblio4.repository.UserProgressRepository;
import user.biblio4.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProgressService {

    private final UserProgressRepository userProgressRepository;
    private final UserRepository userRepository;
    private final RewardService rewardService;

    /**
     * Récupérer les statistiques hebdomadaires
     */
    @Transactional(readOnly = true)
    public Map<String, Object> getWeeklyStats1(Long userId) {
        LocalDateTime weekAgo = LocalDateTime.now().minusDays(7);

        // Récupérer les progrès de la dernière semaine
        List<UserProgress> recentProgress = userProgressRepository
                .findByUserIdAndLastAttemptAfter(userId, weekAgo);

        // Calculer les statistiques
        int wordsLearnedThisWeek = recentProgress.stream()
                .mapToInt(p -> p.getCompletedWords() != null ? p.getCompletedWords().size() : 0)
                .sum();

        int quizzesTakenThisWeek = (int) recentProgress.stream()
                .filter(p -> p.getQuizPassed() != null)
                .count();

        int xpEarnedThisWeek = recentProgress.stream()
                .mapToInt(p -> p.getTotalPoints() != null ? p.getTotalPoints() : 0)
                .sum();

        // Calculer l'activité quotidienne
        Map<String, Integer> dailyActivity = new LinkedHashMap<>();
        for (int i = 6; i >= 0; i--) {
            LocalDateTime day = LocalDateTime.now().minusDays(i);
            String dayKey = day.toLocalDate().toString();

            int activity = (int) recentProgress.stream()
                    .filter(p -> p.getLastAttempt() != null &&
                            p.getLastAttempt().toLocalDate().equals(day.toLocalDate()))
                    .count();

            dailyActivity.put(dayKey, activity);
        }

        Map<String, Object> stats = new HashMap<>();
        stats.put("wordsLearnedThisWeek", wordsLearnedThisWeek);
        stats.put("quizzesTakenThisWeek", quizzesTakenThisWeek);
        stats.put("xpEarnedThisWeek", xpEarnedThisWeek);
        stats.put("dailyActivity", dailyActivity);
        stats.put("averageDailyWords", wordsLearnedThisWeek > 0 ? wordsLearnedThisWeek / 7 : 0);

        return stats;
    }

    /**
     * Mettre à jour le streak quotidien
     */
    @Transactional
    public Map<String, Object> updateDailyStreak(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));

        LocalDateTime now = LocalDateTime.now();
        boolean streakUpdated = false;

        // NOTE: Vous devez ajouter ces champs à votre entité User
        // if (user.getLastLogin() == null) {
        // // Première connexion
        // user.setStreakDays(1);
        // streakUpdated = true;
        // } else {
        // LocalDateTime lastLoginDate =
        // user.getLastLogin().toLocalDate().atStartOfDay();
        // LocalDateTime today = now.toLocalDate().atStartOfDay();
        // LocalDateTime yesterday = today.minusDays(1);
        //
        // if (lastLoginDate.isEqual(yesterday)) {
        // // Connexion consécutive
        // user.setStreakDays(user.getStreakDays() + 1);
        // streakUpdated = true;
        // } else if (!lastLoginDate.isEqual(today)) {
        // // Streak cassé
        // user.setStreakDays(1);
        // streakUpdated = true;
        // }
        // }
        //
        // user.setLastLogin(now);
        // userRepository.save(user);

        // Attribuer une récompense de streak si mis à jour
        Map<String, Object> reward = new HashMap<>();
        if (streakUpdated) {
            reward = rewardService.awardDailyStreak(userId, 1); // user.getStreakDays()
        }

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("streakUpdated", streakUpdated);
        response.put("reward", reward);

        return response;
    }

    /**
     * Récupérer la progression d'un niveau spécifique
     */
    @Transactional(readOnly = true)
    public Map<String, Object> getLevelProgress(Long userId, Integer levelNumber) {
        UserProgress progress = userProgressRepository
                .findByUserIdAndLevelNumber(userId, levelNumber)
                .orElseThrow(() -> new RuntimeException("Niveau non ouvert"));

        Map<String, Object> result = new HashMap<>();
        result.put("levelNumber", levelNumber);
        result.put("completedWords",
                progress.getCompletedWords() != null ? progress.getCompletedWords() : new ArrayList<>());
        result.put("masteredWords",
                progress.getMasteredWords() != null ? progress.getMasteredWords() : new ArrayList<>());
        result.put("totalPoints", progress.getTotalPoints() != null ? progress.getTotalPoints() : 0);
        result.put("quizPassed", progress.getQuizPassed() != null ? progress.getQuizPassed() : false);
        result.put("quizScore", progress.getQuizScore() != null ? progress.getQuizScore() : 0);
        result.put("attempts", progress.getAttempts() != null ? progress.getAttempts() : 0);
        result.put("bestScore", progress.getBestScore() != null ? progress.getBestScore() : 0);
        result.put("unlockedAt", progress.getUnlockedAt());
        result.put("completedAt", progress.getCompletedAt());
        result.put("lastAttempt", progress.getLastAttempt());

        // État du niveau
        String status = "not_started";
        if (progress.getQuizPassed() != null && progress.getQuizPassed()) {
            status = "completed";
        } else if (progress.getCompletedWords() != null && progress.getCompletedWords().size() >= 10) {
            status = "ready_for_quiz";
        } else if (progress.getCompletedWords() != null && progress.getCompletedWords().size() > 0) {
            status = "in_progress";
        }
        result.put("status", status);

        // Calculer le pourcentage de progression
        int progressPercentage = 0;
        if (progress.getCompletedWords() != null) {
            progressPercentage = (progress.getCompletedWords().size() * 100) / 10;
        }
        result.put("progressPercentage", Math.min(progressPercentage, 100));

        return result;
    }

    /**
     * Mettre à jour le meilleur score
     */
    @Transactional
    public void updateBestScore(Long userId, Integer levelNumber, Integer score) {
        UserProgress progress = userProgressRepository
                .findByUserIdAndLevelNumber(userId, levelNumber)
                .orElseThrow(() -> new RuntimeException("Niveau non ouvert"));

        if (progress.getBestScore() == null || score > progress.getBestScore()) {
            progress.setBestScore(score);
            userProgressRepository.save(progress);

        }
    }

    /**
     * Récupérer le classement
     */
    @Transactional(readOnly = true)
    public List<Map<String, Object>> getLeaderboard(String type, int limit) {
        List<User> users = userRepository.findAll();

        return users.stream()
                .sorted((u1, u2) -> {
                    // Pour l'instant, tri par ID ou autre critère disponible
                    // Vous devez ajouter les champs nécessaires à votre entité User
                    return Long.compare(u2.getId(), u1.getId());

                    // Code original (nécessite les champs dans User) :
                    // switch (type) {
                    // case "xp":
                    // return Integer.compare(u2.getTotalXP(), u1.getTotalXP());
                    // case "levels":
                    // return Integer.compare(u2.getCurrentLevel(), u1.getCurrentLevel());
                    // case "streak":
                    // return Integer.compare(u2.getStreakDays(), u1.getStreakDays());
                    // default:
                    // return Integer.compare(u2.getTotalXP(), u1.getTotalXP());
                    // }
                })
                .limit(Math.min(limit, 50)) // Limite à 50 maximum
                .map(user -> {
                    Map<String, Object> entry = new HashMap<>();
                    entry.put("id", user.getId());
                    entry.put("username", user.getUsername());
                    entry.put("email", user.getEmail());
                    entry.put("role", user.getRole());
                    entry.put("level", user.getLevel());
                    entry.put("createdAt", user.getCreatedAt());

                    // Ajouter ces champs si vous les ajoutez à User
                    // entry.put("totalXP", user.getTotalXP());
                    // entry.put("currentLevel", user.getCurrentLevel());
                    // entry.put("streakDays", user.getStreakDays());
                    // entry.put("coins", user.getCoins());

                    return entry;
                })
                .collect(Collectors.toList());
    }

    /**
     * Récupérer les statistiques hebdomadaires
     */
    @Transactional(readOnly = true)
    public Map<String, Object> getWeeklyStats(Long userId) {
        LocalDateTime weekAgo = LocalDateTime.now().minusDays(7);

        // Récupérer les progrès de la dernière semaine
        // NOTE: Vous devez créer cette méthode dans le repository
        List<UserProgress> recentProgress = userProgressRepository
                .findByUserIdAndLastAttemptAfter(userId, weekAgo);

        // Calculer les statistiques
        int wordsLearnedThisWeek = recentProgress.stream()
                .mapToInt(p -> p.getCompletedWords() != null ? p.getCompletedWords().size() : 0)
                .sum();

        int quizzesTakenThisWeek = (int) recentProgress.stream()
                .filter(p -> p.getQuizPassed() != null)
                .count();

        int xpEarnedThisWeek = recentProgress.stream()
                .mapToInt(p -> p.getTotalPoints() != null ? p.getTotalPoints() : 0)
                .sum();

        // Calculer l'activité quotidienne
        Map<String, Integer> dailyActivity = new LinkedHashMap<>();
        for (int i = 6; i >= 0; i--) {
            LocalDateTime day = LocalDateTime.now().minusDays(i);
            String dayKey = day.toLocalDate().toString();

            int activity = (int) recentProgress.stream()
                    .filter(p -> p.getLastAttempt() != null &&
                            p.getLastAttempt().toLocalDate().equals(day.toLocalDate()))
                    .count();

            dailyActivity.put(dayKey, activity);
        }

        Map<String, Object> stats = new HashMap<>();
        stats.put("wordsLearnedThisWeek", wordsLearnedThisWeek);
        stats.put("quizzesTakenThisWeek", quizzesTakenThisWeek);
        stats.put("xpEarnedThisWeek", xpEarnedThisWeek);
        stats.put("dailyActivity", dailyActivity);
        stats.put("averageDailyWords", wordsLearnedThisWeek > 0 ? wordsLearnedThisWeek / 7 : 0);

        return stats;
    }

    // ========== MÉTHODES D'AIDE ==========

    /**
     * Calculer le temps total passé
     */
    private int calculateTotalTimeSpent(List<UserProgress> progressList) {
        // Estimation du temps : chaque mot = 5 minutes, chaque quiz = 10 minutes
        int totalWords = progressList.stream()
                .mapToInt(p -> p.getCompletedWords() != null ? p.getCompletedWords().size() : 0)
                .sum();

        int totalQuizzes = (int) progressList.stream()
                .filter(p -> p.getQuizPassed() != null)
                .count();

        return (totalWords * 5) + (totalQuizzes * 10);
    }

    /**
     * Calculer la progression de l'objectif quotidien
     */
    private Map<String, Object> calculateDailyGoalProgress(Long userId) {
        Map<String, Object> goalProgress = new HashMap<>();

        // Objectif : apprendre 5 mots par jour
        goalProgress.put("dailyGoal", 5);

        // Dans une application réelle, vous calculeriez les mots appris aujourd'hui
        // Pour l'instant, retourner des valeurs par défaut
        goalProgress.put("todayProgress", 0);
        goalProgress.put("goalCompleted", false);
        goalProgress.put("remaining", 5);

        return goalProgress;
    }

    /**
     * Méthode pour obtenir les progrès récents (pour getWeeklyStats)
     * Vous devez créer cette méthode dans UserProgressRepository
     */
    private List<UserProgress> getRecentProgress(Long userId, LocalDateTime fromDate) {
        // Solution temporaire - utilisez toutes les données
        return userProgressRepository.findByUserId(userId).stream()
                .filter(p -> p.getLastAttempt() != null &&
                        p.getLastAttempt().isAfter(fromDate))
                .collect(Collectors.toList());
    }

    public Map<String, Object> getOverallProgress(Long userId) {
        // TODO Auto-generated method stub
        return null;
    }
}