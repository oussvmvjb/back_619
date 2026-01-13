package user.biblio4.service;

import user.biblio4.model.LevelWord;
import user.biblio4.model.QuizQuestion;
import user.biblio4.model.Translation;
import user.biblio4.model.UserProgress;
import user.biblio4.model.User;
import user.biblio4.repository.LevelWordRepository;
import user.biblio4.repository.QuizQuestionRepository;
import user.biblio4.repository.UserProgressRepository;
import user.biblio4.repository.UserRepository;
import lombok.Builder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
@Service
@RequiredArgsConstructor
@Slf4j
@Builder
public class QuizService {
    private final QuizQuestionRepository quizQuestionRepository;
    private final UserProgressRepository userProgressRepository;
    private final UserRepository userRepository;
    private final RewardService rewardService;
    private final LevelWordRepository levelWordRepository;
    private static final Logger log = LoggerFactory.getLogger(QuizService.class);

    public QuizService(
            QuizQuestionRepository quizQuestionRepository,
            UserProgressRepository userProgressRepository,
            UserRepository userRepository,
            RewardService rewardService,
            LevelWordRepository levelWordRepository
    ) {
        this.quizQuestionRepository = quizQuestionRepository;
        this.userProgressRepository = userProgressRepository;
        this.userRepository = userRepository;
        this.rewardService = rewardService;
        this.levelWordRepository = levelWordRepository;
    }

    /**
     * إنشاء سؤال Quiz عشوائي على مستوى معين
     */
    @Transactional
    public QuizQuestion createImageQuiz(Integer levelNumber, String language) {
        // جلب كلمات المستوى
        List<LevelWord> words = levelWordRepository.findByLevelNumberOrderByDisplayOrderAsc(levelNumber);
        if (words.size() < 3) {
            throw new RuntimeException("المستوى يحتاج على الأقل 3 كلمات لإنشاء Quiz");
        }

        // اختيار 3 كلمات مختلفة عشوائياً
        Collections.shuffle(words);
        List<LevelWord> selectedWords = words.subList(0, 3);

        // اختيار واحدة لتكون الإجابة الصحيحة
        Random rand = new Random();
        LevelWord correctWord = selectedWords.get(rand.nextInt(selectedWords.size()));

        // ترجمة الإجابة الصحيحة حسب اللغة
        Optional<Translation> correctTranslation = correctWord.getTranslations().stream()
                .filter(t -> t.getLanguageCode().equals(language))
                .findFirst();

        String correctText;
        String imageUrl = null;
        if (correctTranslation.isPresent()) {
            correctText = correctTranslation.get().getText();
            imageUrl = correctTranslation.get().getGifUrl();
        } else {
            // ⚠️ لا توجد ترجمة للغة المطلوبة
            correctText = correctWord.getWordKey(); // fallback
            log.warn("No translation found for correct word '{}' in language '{}'", correctWord.getWordKey(), language);
        }

        // إعداد الخيارات مترجمة حسب اللغة
        List<String> options = new ArrayList<>();
        for (LevelWord w : selectedWords) {
            Optional<Translation> tr = w.getTranslations().stream()
                    .filter(t -> t.getLanguageCode().equals(language))
                    .findFirst();
            if (tr.isPresent()) {
                options.add(tr.get().getText());
            } else {
                // ⚠️ لا توجد ترجمة للغة المطلوبة
                options.add(w.getWordKey()); // fallback
                log.warn("No translation found for word '{}' in language '{}'", w.getWordKey(), language);
            }
        }

        // خلط الخيارات
        Collections.shuffle(options);

        // إنشاء QuizQuestion بدون حفظ في DB
        QuizQuestion quiz = new QuizQuestion();
        quiz.setLevelNumber(levelNumber);
        quiz.setQuestionType("image");

        // questionText حسب اللغة
        switch (language.toLowerCase()) {
            case "en":
                quiz.setQuestionText("Choose the correct word for the image");
                break;
            case "fr":
                quiz.setQuestionText("Choisissez le mot correct pour l'image");
                break;
            case "ar":
            default:
                quiz.setQuestionText("اختر الكلمة الصحيحة للصورة");
                break;
        }

        quiz.setCorrectAnswer(correctText);  // الإجابة الصحيحة المترجمة
        quiz.setGifUrl(imageUrl);
        quiz.setPoints(20);
        quiz.setTimeLimit(60);
        quiz.setRequiredScore(70);
        quiz.setOptions(options);           // الخيارات مترجمة

        return quizQuestionRepository.save(quiz);
  // ❌ لا تحفظ في DB
    }



    /**
     * Démarrer un quiz pour un niveau
     */
    @Transactional(readOnly = true)
    public Map<String, Object> startQuiz(Long userId, Integer levelNumber) {
        // Vérifier l'existence de l'utilisateur
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));

        // Vérifier l'éligibilité au quiz
        UserProgress progress = userProgressRepository
                .findByUserIdAndLevelNumber(userId, levelNumber)
                .orElseThrow(() -> new RuntimeException("Niveau non ouvert"));

        // Vérifier si le quiz est disponible (vous devez implémenter cette méthode)
        // if (!isQuizAvailable(progress)) {
        // throw new RuntimeException("Quiz non disponible. Complétez 10 mots d'abord");
        // }

        // Récupérer les questions du quiz (5 questions aléatoires)
        List<QuizQuestion> questions = getRandomQuestionsByLevel(levelNumber, 5);

        if (questions.isEmpty()) {
            throw new RuntimeException("Aucune question disponible pour ce niveau");
        }

        // Créer une session de quiz
        String sessionId = UUID.randomUUID().toString();

        // Convertir les questions en format DTO
        List<Map<String, Object>> questionList = questions.stream()
                .map(this::mapQuestionToDTO)
                .collect(Collectors.toList());

        // Calculer le temps limite total
        int totalTimeLimit = questions.stream()
                .mapToInt(q -> q.getTimeLimit() != null ? q.getTimeLimit() : 30)
                .sum();

        // Construire la réponse
        Map<String, Object> response = new HashMap<>();
        response.put("sessionId", sessionId);
        response.put("levelNumber", levelNumber);
        response.put("totalQuestions", questions.size());
        response.put("totalPoints", questions.stream().mapToInt(QuizQuestion::getPoints).sum());
        response.put("requiredScore", questions.get(0).getRequiredScore());
        response.put("timeLimit", totalTimeLimit);
        response.put("startTime", LocalDateTime.now());
        response.put("questions", questionList);

        return response;
    }

    /**
     * Soumettre et corriger un quiz
     */
    @Transactional
    public Map<String, Object> submitQuiz(Long userId, Integer levelNumber,
            Map<String, Object> submission) {

        // Extraire les données
        String sessionId = (String) submission.get("sessionId");
        @SuppressWarnings("unchecked")
        Map<String, String> answers = (Map<String, String>) submission.get("answers");

        // Vérifier l'existence de l'utilisateur
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));

        // Vérifier la progression de l'utilisateur
        UserProgress progress = userProgressRepository
                .findByUserIdAndLevelNumber(userId, levelNumber)
                .orElseThrow(() -> new RuntimeException("Niveau non ouvert"));

        // Récupérer les questions originales
        List<Long> questionIds = answers.keySet().stream()
                .map(Long::parseLong)
                .collect(Collectors.toList());
        List<QuizQuestion> questions = quizQuestionRepository.findAllById(questionIds);

        // Corriger les réponses
        int correctAnswers = 0;
        int totalPoints = 0;
        Map<Long, Boolean> results = new HashMap<>();
        Map<Long, String> correctAnswersMap = new HashMap<>();

        for (QuizQuestion question : questions) {
            String userAnswer = answers.get(question.getId().toString());
            boolean isCorrect = question.getCorrectAnswer() != null &&
                    question.getCorrectAnswer().equalsIgnoreCase(userAnswer);

            results.put(question.getId(), isCorrect);
            correctAnswersMap.put(question.getId(), question.getCorrectAnswer());

            if (isCorrect) {
                correctAnswers++;
                totalPoints += question.getPoints() != null ? question.getPoints() : 10;
            }
        }

        // Calculer le score
        int totalQuestions = questions.size();
        int scorePercentage = totalQuestions > 0 ? (correctAnswers * 100) / totalQuestions : 0;
        boolean passed = questions.size() > 0 && scorePercentage >= questions.get(0).getRequiredScore();

        // Mettre à jour la progression de l'utilisateur
        progress.setQuizPassed(passed);
        progress.setQuizScore(scorePercentage);
        progress.setLastAttempt(LocalDateTime.now());
        progress.setAttempts(progress.getAttempts() != null ? progress.getAttempts() + 1 : 1);

        if (passed) {
            progress.setCompletedAt(LocalDateTime.now());
            progress.setTotalPoints(
                    progress.getTotalPoints() != null ? progress.getTotalPoints() + totalPoints : totalPoints);

            // Mettre à jour les points de l'utilisateur
            // Note: Vous devez ajouter ces champs à votre entité User
            // user.setTotalXP(user.getTotalXP() + totalPoints);
            // user.setCoins(user.getCoins() + 100);
            userRepository.save(user);

            // Accorder des récompenses de succès
            rewardService.awardQuizSuccess(userId, levelNumber, scorePercentage);
        }

        userProgressRepository.save(progress);

        // Construire le résultat du quiz
        Map<String, Object> result = new HashMap<>();
        result.put("passed", passed);
        result.put("score", scorePercentage);
        result.put("correctAnswers", correctAnswers);
        result.put("totalQuestions", totalQuestions);
        result.put("totalPoints", totalPoints);
        result.put("results", results);
        result.put("correctAnswersMap", correctAnswersMap);
        result.put("message", getResultMessage(scorePercentage, passed));

        // Si réussi, vérifier si le niveau suivant peut être débloqué
        if (passed && levelNumber < 10) {
            boolean canUnlockNext = canUnlockNextLevel(userId, levelNumber);
            result.put("nextLevelAvailable", canUnlockNext);

            if (canUnlockNext) {
                result.put("nextLevelNumber", levelNumber + 1);
            }
        }

        // Ajouter les récompenses si réussi
        if (passed) {
            Map<String, Object> reward = new HashMap<>();
            reward.put("xp", totalPoints);
            reward.put("coins", 100);
            reward.put("badge", "quiz_master_" + levelNumber);
            result.put("reward", reward);
        }

        return result;
    }

    /**
     * Récupérer l'historique des quiz
     */
    @Transactional(readOnly = true)
    public List<Map<String, Object>> getQuizHistory(Long userId) {
        List<UserProgress> progressList = userProgressRepository.findByUserId(userId);

        return progressList.stream()
                .filter(p -> p.getQuizScore() != null)
                .sorted((p1, p2) -> {
                    if (p1.getLastAttempt() == null && p2.getLastAttempt() == null)
                        return 0;
                    if (p1.getLastAttempt() == null)
                        return 1;
                    if (p2.getLastAttempt() == null)
                        return -1;
                    return p2.getLastAttempt().compareTo(p1.getLastAttempt());
                })
                .map(this::mapProgressToHistory)
                .collect(Collectors.toList());
    }

    /**
     * Récupérer le résultat d'un quiz spécifique
     */
    @Transactional(readOnly = true)
    public Map<String, Object> getQuizResult(Long userId, Integer levelNumber) {
        UserProgress progress = userProgressRepository
                .findByUserIdAndLevelNumber(userId, levelNumber)
                .orElseThrow(() -> new RuntimeException("Aucun quiz effectué pour ce niveau"));

        if (progress.getQuizScore() == null) {
            throw new RuntimeException("Aucun quiz effectué pour ce niveau");
        }

        Map<String, Object> result = new HashMap<>();
        result.put("levelNumber", levelNumber);
        result.put("score", progress.getQuizScore());
        result.put("passed", progress.getQuizPassed() != null ? progress.getQuizPassed() : false);
        result.put("attempts", progress.getAttempts() != null ? progress.getAttempts() : 0);
        result.put("bestScore", progress.getBestScore() != null ? progress.getBestScore() : 0);
        result.put("lastAttempt", progress.getLastAttempt());
        result.put("completedAt", progress.getCompletedAt());

        return result;
    }

    /**
     * Repasser un quiz
     */
    @Transactional
    public Map<String, Object> retakeQuiz(Long userId, Integer levelNumber) {
        UserProgress progress = userProgressRepository
                .findByUserIdAndLevelNumber(userId, levelNumber)
                .orElseThrow(() -> new RuntimeException("Niveau non ouvert"));

        // Réinitialiser le résultat du quiz précédent
        progress.setQuizPassed(null);
        progress.setQuizScore(null);
        progress.setLastAttempt(null);

        userProgressRepository.save(progress);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Quiz réinitialisé. Vous pouvez recommencer");
        response.put("levelNumber", levelNumber);

        return response;
    }

    // ========== MÉTHODES D'AIDE ==========

    /**
     * Convertir une question en DTO
     */
    private Map<String, Object> mapQuestionToDTO(QuizQuestion question) {
        Map<String, Object> dto = new HashMap<>();
        dto.put("id", question.getId());
        dto.put("questionType", question.getQuestionType());
        dto.put("questionText", question.getQuestionText());
        dto.put("options", question.getOptions() != null ? question.getOptions() : new ArrayList<>());
        dto.put("gifUrl", question.getGifUrl());
        dto.put("timeLimit", question.getTimeLimit() != null ? question.getTimeLimit() : 30);
        dto.put("points", question.getPoints() != null ? question.getPoints() : 10);
        dto.put("requiredScore", question.getRequiredScore() != null ? question.getRequiredScore() : 70);
        return dto;
    }

    /**
     * Convertir une progression en historique
     */
    private Map<String, Object> mapProgressToHistory(UserProgress progress) {
        Map<String, Object> history = new HashMap<>();
        history.put("levelNumber", progress.getLevelNumber());
        history.put("score", progress.getQuizScore());
        history.put("passed", progress.getQuizPassed() != null ? progress.getQuizPassed() : false);
        history.put("attempts", progress.getAttempts() != null ? progress.getAttempts() : 0);
        history.put("bestScore", progress.getBestScore() != null ? progress.getBestScore() : 0);
        history.put("date", progress.getLastAttempt());
        history.put("totalPoints", progress.getTotalPoints() != null ? progress.getTotalPoints() : 0);
        return history;
    }

    /**
     * Message de résultat
     */
    private String getResultMessage(int score, boolean passed) {
        if (!passed) {
            return "Vous n'avez pas réussi le quiz. Score: " + score + "%. Essayez encore!";
        }

        if (score >= 90) {
            return "Incroyable! Score excellent: " + score + "%";
        } else if (score >= 80) {
            return "Excellent! Très bon score: " + score + "%";
        } else if (score >= 70) {
            return "Bien! Vous avez réussi le quiz: " + score + "%";
        }

        return "Vous avez réussi le quiz: " + score + "%";
    }

    /**
     * Vérifier si le niveau suivant peut être débloqué
     */
    private boolean canUnlockNextLevel(Long userId, Integer currentLevel) {
        if (currentLevel >= 10)
            return false;

        return userProgressRepository
                .findByUserIdAndLevelNumber(userId, currentLevel + 1)
                .isEmpty();
    }

    /**
     * Vérifier si un quiz est disponible
     * Vous devez implémenter cette logique selon vos règles métier
     */
    private boolean isQuizAvailable(UserProgress progress) {
        // Exemple: quiz disponible si 10 mots sont complétés
        return progress.getCompletedWords() != null &&
                progress.getCompletedWords().size() >= 10;
    }

    /**
     * Récupérer des questions aléatoires pour un niveau
     * Note: Vous devez créer cette méthode dans QuizQuestionRepository
     */
    private List<QuizQuestion> getRandomQuestionsByLevel(Integer levelNumber, int count) {
        // Solution temporaire - récupérer toutes les questions et en sélectionner
        // aléatoirement
        List<QuizQuestion> allQuestions = quizQuestionRepository.findByLevelNumber(levelNumber);

        if (allQuestions.size() <= count) {
            return allQuestions;
        }

        Collections.shuffle(allQuestions);
        return allQuestions.subList(0, Math.min(count, allQuestions.size()));
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
}