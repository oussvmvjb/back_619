package user.biblio4.service;

import user.biblio4.model.*;
import user.biblio4.repository.*;
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
public class LevelService {

    private final LevelWordRepository levelWordRepository;
    private final TranslationRepository translationRepository;
    private final UserProgressRepository userProgressRepository;
    private final UserRepository userRepository;
    private final RewardService rewardService;

    @Transactional(readOnly = true)
    public Map<String, Object> getLevelWithProgress(Long userId, Integer levelNumber, String language) {
        // Récupérer les mots du niveau
        List<LevelWord> words = levelWordRepository.findByLevelNumberOrderByDisplayOrderAsc(levelNumber);

        if (words.isEmpty()) {
            throw new RuntimeException("Niveau non trouvé : " + levelNumber);
        }

        // Récupérer ou créer la progression de l'utilisateur
        UserProgress progress = getOrCreateUserProgress(userId, levelNumber);

        // Construire la liste des mots avec leur état
        List<Map<String, Object>> wordList = new ArrayList<>();

        for (LevelWord word : words) {
            // Récupérer les traductions
            Optional<Translation> translation = translationRepository
                    .findByWordKeyAndLanguageCode(word.getWordKey(), language);

            Map<String, Object> wordData = new HashMap<>();
            wordData.put("id", word.getId());
            wordData.put("wordKey", word.getWordKey());
            wordData.put("category", word.getCategory());
            wordData.put("points", word.getPoints() != null ? word.getPoints() : 10);
            wordData.put("displayOrder", word.getDisplayOrder() != null ? word.getDisplayOrder() : 0);

            // Ajouter les traductions si trouvées
            translation.ifPresent(t -> {
                wordData.put("text", t.getText());
                wordData.put("gifUrl", t.getGifUrl());
                wordData.put("audioUrl", t.getAudioUrl());
            });

            // État d'apprentissage
            wordData.put("learned", progress.getCompletedWords() != null &&
                    progress.getCompletedWords().contains(word.getWordKey()));
            wordData.put("mastered", progress.getMasteredWords() != null &&
                    progress.getMasteredWords().contains(word.getWordKey()));

            wordList.add(wordData);
        }

        // Statistiques du niveau
        int totalWords = words.size();
        int learnedWords = (int) wordList.stream()
                .filter(w -> (Boolean) w.get("learned"))
                .count();
        int masteredWords = (int) wordList.stream()
                .filter(w -> (Boolean) w.get("mastered"))
                .count();

        int progressPercentage = totalWords > 0 ? (learnedWords * 100) / totalWords : 0;

        // Construire le résultat
        Map<String, Object> result = new HashMap<>();
        result.put("levelNumber", levelNumber);
        result.put("totalWords", totalWords);
        result.put("learnedWords", learnedWords);
        result.put("masteredWords", masteredWords);
        result.put("progressPercentage", progressPercentage);
        result.put("totalPoints", progress.getTotalPoints());
        result.put("quizAvailable", isQuizAvailable(progress));
        result.put("quizPassed", progress.getQuizPassed() != null ? progress.getQuizPassed() : false);
        result.put("quizScore", progress.getQuizScore());
        result.put("words", wordList);

        // Vérifier si le niveau suivant peut être débloqué
        if (progress.getQuizPassed() != null && progress.getQuizPassed()) {
            boolean canUnlockNext = canUnlockNextLevel(userId, levelNumber);
            result.put("nextLevelUnlocked", canUnlockNext);
            if (canUnlockNext) {
                result.put("nextLevelNumber", levelNumber + 1);
            }
        }

        return result;
    }

    @Transactional
    public Map<String, Object> completeWord(Long userId, Integer levelNumber, String wordKey) {
        // Récupérer ou créer la progression de l'utilisateur
        UserProgress progress = getOrCreateUserProgress(userId, levelNumber);

        // Vérifier l'existence du mot dans le niveau
        Optional<LevelWord> wordOpt = levelWordRepository.findByWordKeyAndLevelNumber(wordKey, levelNumber);
        if (wordOpt.isEmpty()) {
            throw new RuntimeException("Mot non trouvé dans ce niveau");
        }

        LevelWord word = wordOpt.get();

        // Ajouter le mot aux complétés s'il n'existe pas
        List<String> completedWords = progress.getCompletedWords();
        if (!completedWords.contains(wordKey)) {
            completedWords.add(wordKey);
            progress.setCompletedWords(completedWords);
            progress.setTotalPoints(progress.getTotalPoints() + (word.getPoints() != null ? word.getPoints() : 10));
            progress.setLastAttempt(LocalDateTime.now());
            userProgressRepository.save(progress);

        }

        // Vérifier si tous les mots du niveau sont complétés
        int totalWordsInLevel = levelWordRepository.countByLevelNumber(levelNumber);
        boolean allWordsCompleted = completedWords.size() >= Math.min(totalWordsInLevel, 10);

        // Attribuer des récompenses
        Map<String, Object> reward = new HashMap<>();
        if (allWordsCompleted) {
            reward = rewardService.awardLevelCompletion(userId, levelNumber);
        }

        // Construire la réponse
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Félicitations ! Mot appris : " + wordKey);
        response.put("wordKey", wordKey);
        response.put("pointsEarned", word.getPoints() != null ? word.getPoints() : 10);
        response.put("totalCompleted", completedWords.size());
        response.put("allWordsCompleted", allWordsCompleted);
        response.put("quizAvailable", allWordsCompleted && progress.getQuizPassed() == null);

        if (allWordsCompleted && !reward.isEmpty()) {
            response.put("reward", reward);
        }

        return response;
    }

    @Transactional
    public Map<String, Object> masterWord(Long userId, Integer levelNumber, String wordKey) {
        // Récupérer la progression de l'utilisateur
        UserProgress progress = userProgressRepository
                .findByUserIdAndLevelNumber(userId, levelNumber)
                .orElseThrow(() -> new RuntimeException("Niveau non ouvert"));

        // Vérifier que le mot est complété d'abord
        List<String> completedWords = progress.getCompletedWords();
        if (!completedWords.contains(wordKey)) {
            throw new RuntimeException("Vous devez apprendre le mot avant de le maîtriser");
        }

        // Ajouter le mot aux maîtrisés s'il n'existe pas
        List<String> masteredWords = progress.getMasteredWords();
        if (!masteredWords.contains(wordKey)) {
            masteredWords.add(wordKey);
            progress.setMasteredWords(masteredWords);
            progress.setLastAttempt(LocalDateTime.now());
            userProgressRepository.save(progress);

            Map<String, Object> reward = rewardService.awardWordMastery(userId, wordKey);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Excellent ! Mot maîtrisé : " + wordKey);
            response.put("wordKey", wordKey);
            response.put("masteredWords", masteredWords.size());
            response.put("reward", reward);

            return response;
        }

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Mot déjà maîtrisé");
        response.put("wordKey", wordKey);
        response.put("masteredWords", masteredWords.size());

        return response;
    }

    @Transactional
    public Map<String, Object> unlockNextLevel(Long userId) {
        // Trouver le niveau actuel complété
        List<UserProgress> userProgresses = userProgressRepository.findByUserId(userId);

        Optional<UserProgress> completedProgress = userProgresses.stream()
                .filter(p -> p.getQuizPassed() != null && p.getQuizPassed())
                .max(Comparator.comparing(UserProgress::getLevelNumber));

        if (completedProgress.isEmpty()) {
            throw new RuntimeException("Aucun niveau complété trouvé");
        }

        int currentLevel = completedProgress.get().getLevelNumber();
        int nextLevel = currentLevel + 1;

        // Vérifier si le niveau suivant existe déjà
        boolean nextLevelExists = userProgresses.stream()
                .anyMatch(p -> p.getLevelNumber() == nextLevel);

        if (nextLevelExists) {
            throw new RuntimeException("Le niveau suivant est déjà ouvert");
        }

        // Vérifier le nombre maximum de niveaux
        int maxLevel = 10; // À ajuster selon votre configuration
        if (nextLevel > maxLevel) {
            throw new RuntimeException("Vous avez atteint le niveau maximum");
        }

        // Ouvrir le niveau suivant
        UserProgress nextProgress = createUserProgress(userId, nextLevel);

        // Attribuer une récompense pour l'ouverture d'un nouveau niveau
        Map<String, Object> reward = rewardService.awardLevelUnlock(userId, nextLevel);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Félicitations ! Niveau " + nextLevel + " débloqué");
        response.put("unlockedLevel", nextLevel);
        response.put("reward", reward);

        return response;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getUserStats(Long userId) {
        // Récupérer toutes les progressions de l'utilisateur
        List<UserProgress> allProgress = userProgressRepository.findByUserId(userId);

        // Calculer les statistiques
        int totalWordsLearned = allProgress.stream()
                .mapToInt(p -> p.getCompletedWords() != null ? p.getCompletedWords().size() : 0)
                .sum();

        int totalWordsMastered = allProgress.stream()
                .mapToInt(p -> p.getMasteredWords() != null ? p.getMasteredWords().size() : 0)
                .sum();

        int totalPoints = allProgress.stream()
                .mapToInt(p -> p.getTotalPoints() != null ? p.getTotalPoints() : 0)
                .sum();

        long levelsCompleted = allProgress.stream()
                .filter(p -> p.getQuizPassed() != null && p.getQuizPassed())
                .count();

        // Construire le résultat
        Map<String, Object> stats = new HashMap<>();
        stats.put("userId", userId);
        stats.put("totalWordsLearned", totalWordsLearned);
        stats.put("totalWordsMastered", totalWordsMastered);
        stats.put("totalPoints", totalPoints);
        stats.put("levelsCompleted", levelsCompleted);
        stats.put("averageQuizScore", calculateAverageScore(allProgress));
        stats.put("totalLevelsUnlocked", allProgress.size());

        return stats;
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> getUserLevels(Long userId) {
        List<UserProgress> progressList = userProgressRepository.findByUserId(userId);

        return progressList.stream()
                .sorted(Comparator.comparing(UserProgress::getLevelNumber))
                .map(this::mapProgressToLevelInfo)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getLevelStatus(Long userId, Integer levelNumber) {
        Optional<UserProgress> progressOpt = userProgressRepository
                .findByUserIdAndLevelNumber(userId, levelNumber);

        Map<String, Object> status = new HashMap<>();
        status.put("levelNumber", levelNumber);
        status.put("userId", userId);

        if (progressOpt.isPresent()) {
            UserProgress progress = progressOpt.get();
            status.put("unlocked", true);
            status.put("unlockedAt", progress.getUnlockedAt());
            status.put("completedWords",
                    progress.getCompletedWords() != null ? progress.getCompletedWords().size() : 0);
            status.put("masteredWords", progress.getMasteredWords() != null ? progress.getMasteredWords().size() : 0);
            status.put("quizPassed", progress.getQuizPassed() != null ? progress.getQuizPassed() : false);
            status.put("quizScore", progress.getQuizScore());
            status.put("totalPoints", progress.getTotalPoints() != null ? progress.getTotalPoints() : 0);
            status.put("isQuizAvailable", isQuizAvailable(progress));
        } else {
            status.put("unlocked", false);
            status.put("unlockedAt", null);
            status.put("completedWords", 0);
            status.put("masteredWords", 0);
            status.put("quizPassed", false);
            status.put("quizScore", null);
            status.put("totalPoints", 0);
            status.put("isQuizAvailable", false);
        }

        return status;
    }

    // ========== MÉTHODES D'AIDE ==========

    /**
     * Récupérer ou créer UserProgress
     */
    @Transactional
    private UserProgress getOrCreateUserProgress(Long userId, Integer levelNumber) {
        return userProgressRepository
                .findByUserIdAndLevelNumber(userId, levelNumber)
                .orElseGet(() -> createUserProgress(userId, levelNumber));
    }

    /**
     * Créer un nouveau UserProgress
     */
    @Transactional
    private UserProgress createUserProgress(Long userId, Integer levelNumber) {
        // Vérifier que l'utilisateur existe
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));

        UserProgress newProgress = new UserProgress();
        newProgress.setUser(user);
        newProgress.setLevelNumber(levelNumber);
        newProgress.setUnlockedAt(LocalDateTime.now());
        newProgress.setCompletedWords(new ArrayList<>());
        newProgress.setMasteredWords(new ArrayList<>());
        newProgress.setTotalPoints(0);
        newProgress.setAttempts(0);
        newProgress.setQuizPassed(false);
        newProgress.setQuizScore(null);

        return userProgressRepository.save(newProgress);
    }

    /**
     * Vérifier si le quiz est disponible
     */
    private boolean isQuizAvailable(UserProgress progress) {
        if (progress == null)
            return false;

        // Quiz disponible si au moins 10 mots complétés ou tous les mots du niveau
        int completedWords = progress.getCompletedWords() != null ? progress.getCompletedWords().size() : 0;
        return completedWords >= 10 && (progress.getQuizPassed() == null || !progress.getQuizPassed());
    }

    /**
     * Vérifier la possibilité de débloquer le niveau suivant
     */
    private boolean canUnlockNextLevel(Long userId, Integer currentLevel) {
        if (currentLevel >= 10)
            return false;

        return !userProgressRepository
                .existsByUserIdAndLevelNumber(userId, currentLevel + 1);
    }

    /**
     * Calculer la moyenne des scores de quiz
     */
    private double calculateAverageScore(List<UserProgress> progressList) {
        List<Integer> scores = progressList.stream()
                .filter(p -> p.getQuizScore() != null)
                .map(UserProgress::getQuizScore)
                .collect(Collectors.toList());

        if (scores.isEmpty())
            return 0.0;

        double average = scores.stream()
                .mapToInt(Integer::intValue)
                .average()
                .orElse(0.0);

        return Math.round(average * 100.0) / 100.0;
    }

    /**
     * Convertir UserProgress en informations de niveau
     */
    private Map<String, Object> mapProgressToLevelInfo(UserProgress progress) {
        Map<String, Object> levelInfo = new HashMap<>();
        levelInfo.put("levelNumber", progress.getLevelNumber());
        levelInfo.put("completedWords", progress.getCompletedWords() != null ? progress.getCompletedWords().size() : 0);
        levelInfo.put("masteredWords", progress.getMasteredWords() != null ? progress.getMasteredWords().size() : 0);
        levelInfo.put("totalPoints", progress.getTotalPoints() != null ? progress.getTotalPoints() : 0);
        levelInfo.put("quizPassed", progress.getQuizPassed() != null ? progress.getQuizPassed() : false);
        levelInfo.put("quizScore", progress.getQuizScore());
        levelInfo.put("unlockedAt", progress.getUnlockedAt());
        levelInfo.put("completedAt", progress.getCompletedAt());
        levelInfo.put("lastAttempt", progress.getLastAttempt());

        // État du niveau
        String status = "locked";
        if (progress.getUnlockedAt() != null) {
            if (progress.getQuizPassed() != null && progress.getQuizPassed()) {
                status = "completed";
            } else if (isQuizAvailable(progress)) {
                status = "ready_for_quiz";
            } else if (progress.getCompletedWords() != null && progress.getCompletedWords().size() > 0) {
                status = "in_progress";
            } else {
                status = "unlocked";
            }
        }
        levelInfo.put("status", status);

        // Pourcentage de progression
        int progressPercentage = 0;
        if (progress.getCompletedWords() != null) {
            progressPercentage = (progress.getCompletedWords().size() * 100) / 10;
        }
        levelInfo.put("progressPercentage", Math.min(progressPercentage, 100));

        return levelInfo;
    }

    /**
     * Récupérer les mots restants pour compléter le niveau
     */
    @Transactional(readOnly = true)
    public Map<String, Object> getRemainingWords(Long userId, Integer levelNumber, String language) {
        List<LevelWord> allWords = levelWordRepository.findByLevelNumberOrderByDisplayOrderAsc(levelNumber);
        UserProgress progress = getOrCreateUserProgress(userId, levelNumber);

        List<Map<String, Object>> remainingWords = new ArrayList<>();
        List<Map<String, Object>> completedWords = new ArrayList<>();

        for (LevelWord word : allWords) {
            Optional<Translation> translation = translationRepository
                    .findByWordKeyAndLanguageCode(word.getWordKey(), language);

            Map<String, Object> wordData = new HashMap<>();
            wordData.put("wordKey", word.getWordKey());
            wordData.put("category", word.getCategory());
            wordData.put("points", word.getPoints() != null ? word.getPoints() : 10);
            wordData.put("displayOrder", word.getDisplayOrder() != null ? word.getDisplayOrder() : 0);

            translation.ifPresent(t -> {
                wordData.put("text", t.getText());
                wordData.put("gifUrl", t.getGifUrl());
            });

            boolean isCompleted = progress.getCompletedWords() != null &&
                    progress.getCompletedWords().contains(word.getWordKey());

            if (isCompleted) {
                boolean isMastered = progress.getMasteredWords() != null &&
                        progress.getMasteredWords().contains(word.getWordKey());
                wordData.put("mastered", isMastered);
                completedWords.add(wordData);
            } else {
                remainingWords.add(wordData);
            }
        }

        Map<String, Object> result = new HashMap<>();
        result.put("levelNumber", levelNumber);
        result.put("totalWords", allWords.size());
        result.put("completedCount", completedWords.size());
        result.put("remainingCount", remainingWords.size());
        result.put("completedWords", completedWords);
        result.put("remainingWords", remainingWords);
        result.put("progressPercentage", allWords.size() > 0 ? (completedWords.size() * 100) / allWords.size() : 0);

        return result;
    }

    /**
     * Réinitialiser la progression d'un niveau
     */
    @Transactional
    public Map<String, Object> resetLevelProgress(Long userId, Integer levelNumber) {
        UserProgress progress = userProgressRepository
                .findByUserIdAndLevelNumber(userId, levelNumber)
                .orElseThrow(() -> new RuntimeException("Niveau non ouvert"));

        progress.setCompletedWords(new ArrayList<>());
        progress.setMasteredWords(new ArrayList<>());
        progress.setTotalPoints(0);
        progress.setQuizPassed(false);
        progress.setQuizScore(null);
        progress.setAttempts(0);
        progress.setBestScore(null);
        progress.setLastAttempt(null);
        progress.setCompletedAt(null);

        userProgressRepository.save(progress);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Progression du niveau " + levelNumber + " réinitialisée");
        response.put("levelNumber", levelNumber);

        return response;
    }

    /**
     * Débloquer un niveau spécifique (pour les tests ou administration)
     */
    @Transactional
    public Map<String, Object> unlockSpecificLevel(Long userId, Integer levelNumber) {
        // Vérifier si le niveau existe déjà
        Optional<UserProgress> existingProgress = userProgressRepository
                .findByUserIdAndLevelNumber(userId, levelNumber);

        if (existingProgress.isPresent()) {
            throw new RuntimeException("Niveau déjà débloqué");
        }

        // Créer la progression pour le niveau
        UserProgress progress = createUserProgress(userId, levelNumber);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Niveau " + levelNumber + " débloqué avec succès");
        response.put("levelNumber", levelNumber);
        response.put("unlockedAt", progress.getUnlockedAt());

        return response;
    }
}