package user.biblio4.model;

import jakarta.persistence.*;
import lombok.*;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "quiz_question")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class QuizQuestion {
    
    public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public Integer getLevelNumber() {
		return levelNumber;
	}

	public void setLevelNumber(Integer levelNumber) {
		this.levelNumber = levelNumber;
	}

	public String getQuestionType() {
		return questionType;
	}

	public void setQuestionType(String questionType) {
		this.questionType = questionType;
	}

	public String getQuestionText() {
		return questionText;
	}

	public void setQuestionText(String questionText) {
		this.questionText = questionText;
	}

	public String getCorrectAnswer() {
		return correctAnswer;
	}

	public void setCorrectAnswer(String correctAnswer) {
		this.correctAnswer = correctAnswer;
	}

	public String getOptionsJson() {
		return optionsJson;
	}

	public void setOptionsJson(String optionsJson) {
		this.optionsJson = optionsJson;
	}

	public String getGifUrl() {
		return gifUrl;
	}

	public void setGifUrl(String gifUrl) {
		this.gifUrl = gifUrl;
	}

	public Integer getPoints() {
		return points;
	}

	public void setPoints(Integer points) {
		this.points = points;
	}

	public Integer getRequiredScore() {
		return requiredScore;
	}

	public void setRequiredScore(Integer requiredScore) {
		this.requiredScore = requiredScore;
	}

	public Integer getTimeLimit() {
		return timeLimit;
	}

	public void setTimeLimit(Integer timeLimit) {
		this.timeLimit = timeLimit;
	}

	public String getExplanation() {
		return explanation;
	}

	public void setExplanation(String explanation) {
		this.explanation = explanation;
	}

	@Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "level_number", nullable = false)
    private Integer levelNumber;
    
    @Column(name = "question_type", nullable = false, length = 50)
    private String questionType;
    
    @Column(name = "question_text", nullable = false, length = 500)
    private String questionText;
    
    @Column(name = "correct_answer", nullable = false, length = 255)
    private String correctAnswer;
    
    @Column(name = "options_json", columnDefinition = "TEXT")
    private String optionsJson;
    
    @Column(name = "gif_url", length = 500)
    private String gifUrl;
    
    @Column(name = "points", nullable = false)
    @Builder.Default
    private Integer points = 20;
    
    @Column(name = "required_score", nullable = false)
    @Builder.Default
    private Integer requiredScore = 70;
    
    @Column(name = "time_limit")
    private Integer timeLimit;
    
    @Column(name = "explanation", length = 500)
    private String explanation;
    
    public List<String> getOptions() {
        try {
            ObjectMapper mapper = new ObjectMapper();
            return mapper.readValue(optionsJson, new TypeReference<List<String>>() {});
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }
    
    public void setOptions(List<String> options) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            this.optionsJson = mapper.writeValueAsString(options);
        } catch (Exception e) {
            this.optionsJson = "[]";
        }
    }
}