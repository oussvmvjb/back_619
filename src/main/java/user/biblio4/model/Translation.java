package user.biblio4.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "translation", 
       uniqueConstraints = @UniqueConstraint(columnNames = {"word_key", "language_code"}))
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Translation {
    
    public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getWordKey() {
		return wordKey;
	}

	public void setWordKey(String wordKey) {
		this.wordKey = wordKey;
	}

	public String getLanguageCode() {
		return languageCode;
	}

	public void setLanguageCode(String languageCode) {
		this.languageCode = languageCode;
	}

	public String getText() {
		return text;
	}

	public void setText(String text) {
		this.text = text;
	}

	public String getGifUrl() {
		return gifUrl;
	}

	public void setGifUrl(String gifUrl) {
		this.gifUrl = gifUrl;
	}

	public String getAudioUrl() {
		return audioUrl;
	}

	public void setAudioUrl(String audioUrl) {
		this.audioUrl = audioUrl;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	@Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "word_key", nullable = false, length = 100)
    private String wordKey;
    
    @Column(name = "language_code", nullable = false, length = 2)
    private String languageCode;
    
    @Column(name = "text", nullable = false, length = 255)
    private String text;
    
    @Column(name = "gif_url", length = 500)
    private String gifUrl;
    
    @Column(name = "audio_url", length = 500)
    private String audioUrl;
    
    @Column(name = "description", length = 500)
    private String description;
}