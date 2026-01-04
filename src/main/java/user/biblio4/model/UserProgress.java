package user.biblio4.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "user_progress")
public class UserProgress {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
    
    @Column(name = "level_number", nullable = false)
    private Integer levelNumber;
    
    @ElementCollection
    @CollectionTable(name = "user_progress_completed_words", 
                     joinColumns = @JoinColumn(name = "progress_id"))
    @Column(name = "word_key")
    private List<String> completedWords = new ArrayList<>();
    
    @ElementCollection
    @CollectionTable(name = "user_progress_mastered_words", 
                     joinColumns = @JoinColumn(name = "progress_id"))
    @Column(name = "word_key")
    private List<String> masteredWords = new ArrayList<>();
    
    @Column(name = "total_points")
    private Integer totalPoints = 0;
    
    @Column(name = "quiz_passed")
    private Boolean quizPassed = false;
    
    @Column(name = "quiz_score")
    private Integer quizScore;
    
    @Column(name = "attempts")
    private Integer attempts = 0;
    
    @Column(name = "best_score")
    private Integer bestScore;
    
    @Column(name = "unlocked_at", nullable = false)
    private LocalDateTime unlockedAt = LocalDateTime.now();
    
    @Column(name = "completed_at")
    private LocalDateTime completedAt;
    
    @Column(name = "last_attempt")
    private LocalDateTime lastAttempt;
    
    // Constructeurs
    public UserProgress() {}
    
    public UserProgress(User user, Integer levelNumber) {
        this.user = user;
        this.levelNumber = levelNumber;
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
    
    public Integer getLevelNumber() {
        return levelNumber;
    }
    
    public void setLevelNumber(Integer levelNumber) {
        this.levelNumber = levelNumber;
    }
    
    public List<String> getCompletedWords() {
        if (completedWords == null) {
            completedWords = new ArrayList<>();
        }
        return completedWords;
    }
    
    public void setCompletedWords(List<String> completedWords) {
        this.completedWords = completedWords;
    }
    
    public List<String> getMasteredWords() {
        if (masteredWords == null) {
            masteredWords = new ArrayList<>();
        }
        return masteredWords;
    }
    
    public void setMasteredWords(List<String> masteredWords) {
        this.masteredWords = masteredWords;
    }
    
    public Integer getTotalPoints() {
        return totalPoints != null ? totalPoints : 0;
    }
    
    public void setTotalPoints(Integer totalPoints) {
        this.totalPoints = totalPoints;
    }
    
    public Boolean getQuizPassed() {
        return quizPassed != null ? quizPassed : false;
    }
    
    public void setQuizPassed(Boolean quizPassed) {
        this.quizPassed = quizPassed;
    }
    
    public Integer getQuizScore() {
        return quizScore;
    }
    
    public void setQuizScore(Integer quizScore) {
        this.quizScore = quizScore;
    }
    
    public Integer getAttempts() {
        return attempts != null ? attempts : 0;
    }
    
    public void setAttempts(Integer attempts) {
        this.attempts = attempts;
    }
    
    public Integer getBestScore() {
        return bestScore;
    }
    
    public void setBestScore(Integer bestScore) {
        this.bestScore = bestScore;
    }
    
    public LocalDateTime getUnlockedAt() {
        return unlockedAt;
    }
    
    public void setUnlockedAt(LocalDateTime unlockedAt) {
        this.unlockedAt = unlockedAt;
    }
    
    public LocalDateTime getCompletedAt() {
        return completedAt;
    }
    
    public void setCompletedAt(LocalDateTime completedAt) {
        this.completedAt = completedAt;
    }
    
    public LocalDateTime getLastAttempt() {
        return lastAttempt;
    }
    
    public void setLastAttempt(LocalDateTime lastAttempt) {
        this.lastAttempt = lastAttempt;
    }
    
    // Méthodes utilitaires
    public void addCompletedWord(String wordKey) {
        if (completedWords == null) {
            completedWords = new ArrayList<>();
        }
        if (!completedWords.contains(wordKey)) {
            completedWords.add(wordKey);
        }
    }
    
    public void addMasteredWord(String wordKey) {
        if (masteredWords == null) {
            masteredWords = new ArrayList<>();
        }
        if (!masteredWords.contains(wordKey)) {
            masteredWords.add(wordKey);
        }
    }
    
    public boolean isQuizAvailable() {
        return getCompletedWords().size() >= 10;
    }
}