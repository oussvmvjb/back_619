package user.biblio4.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;


@Entity
@Table(name = "level_word", 
       uniqueConstraints = @UniqueConstraint(columnNames = {"level_number", "display_order"}))
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LevelWord {
    
    public List<Translation> getTranslations() {
		return translations;
	}

	public void setTranslations(List<Translation> translations) {
		this.translations = translations;
	}

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

	public String getWordKey() {
		return wordKey;
	}

	public void setWordKey(String wordKey) {
		this.wordKey = wordKey;
	}

	public String getCategory() {
		return category;
	}

	public void setCategory(String category) {
		this.category = category;
	}

	public Integer getDisplayOrder() {
		return displayOrder;
	}

	public void setDisplayOrder(Integer displayOrder) {
		this.displayOrder = displayOrder;
	}

	public Integer getPoints() {
		return points;
	}

	public void setPoints(Integer points) {
		this.points = points;
	}

	public LocalDateTime getCreatedAt() {
		return createdAt;
	}

	public void setCreatedAt(LocalDateTime createdAt) {
		this.createdAt = createdAt;
	}
	
    @OneToMany(mappedBy = "levelWord", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Translation> translations;
	@Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "level_number", nullable = false)
    private Integer levelNumber;
    
    @Column(name = "word_key", nullable = false, unique = true, length = 100)
    private String wordKey;
    
    @Column(name = "category", length = 50)
    private String category;
    
    @Column(name = "display_order")
    private Integer displayOrder;
    
    @Column(name = "points", nullable = false)
    @Builder.Default
    private Integer points = 10;
    
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
}