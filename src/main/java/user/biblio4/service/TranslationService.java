package user.biblio4.service;

import user.biblio4.model.Translation;
import user.biblio4.model.LevelWord;
import user.biblio4.repository.TranslationRepository;
import user.biblio4.repository.LevelWordRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class TranslationService {
    
    private final TranslationRepository translationRepository = null;
    private final LevelWordRepository levelWordRepository = null;
    
    @Transactional(readOnly = true)
    public Map<String, Object> getWordTranslation(String wordKey, String language) {
        
        // Vérification de debug
        if (translationRepository == null) {
            throw new IllegalStateException("TranslationRepository is null - Spring injection failed!");
        }
        
        Optional<Translation> translationOpt = translationRepository
                .findByWordKeyAndLanguageCode(wordKey, language);
        
        if (translationOpt.isEmpty()) {
        
        }
        
        Translation translation = translationOpt.get();
        
        Map<String, Object> result = new HashMap<>();
        result.put("wordKey", wordKey);
        result.put("language", language);
        result.put("text", translation.getText());
        result.put("gifUrl", translation.getGifUrl());
        result.put("audioUrl", translation.getAudioUrl());
		return result;
        
        
    }
    
    @Transactional(readOnly = true)
    public Map<String, Object> getAllTranslations(String wordKey) {
        
        List<Translation> translations = translationRepository.findByWordKey(wordKey);
        
        if (translations.isEmpty()) {
            throw new RuntimeException("No translations found for word: " + wordKey);
        }
        
        Map<String, Object> result = new HashMap<>();
        result.put("wordKey", wordKey);
        
        // Organiser par langue
        Map<String, Map<String, Object>> byLanguage = new HashMap<>();
        
        for (Translation t : translations) {
            Map<String, Object> langData = new HashMap<>();
            langData.put("text", t.getText());
            langData.put("gifUrl", t.getGifUrl());
            langData.put("audioUrl", t.getAudioUrl());
            byLanguage.put(t.getLanguageCode(), langData);
        }
        
        result.put("translations", byLanguage);
        
        return result;
    }
    
    @Transactional(readOnly = true)
    public List<Map<String, Object>> searchWords(String query, String language) {
        
        List<Translation> translations;
        
        try {
            // Essayez d'abord la méthode native du repository
            translations = translationRepository.findByLanguageCodeAndTextContainingIgnoreCase(language, query);
        } catch (Exception e) {
            // Fallback : utiliser la méthode @Query
            translations = translationRepository.findByTextContainingAndLanguageCode(query, language);
        }
        
        // Si toujours vide, récupérer tout et filtrer manuellement
        if (translations == null || translations.isEmpty()) {
            List<Translation> allTranslations = translationRepository.findByLanguageCode(language);
            translations = allTranslations.stream()
                    .filter(t -> t.getText() != null && 
                               t.getText().toLowerCase().contains(query.toLowerCase()))
                    .collect(Collectors.toList());
        }
        
        List<Map<String, Object>> results = new ArrayList<>();
        
        for (Translation t : translations) {
            Map<String, Object> result = new HashMap<>();
            result.put("wordKey", t.getWordKey());
            result.put("text", t.getText());
            
            // Ajouter les informations du LevelWord si disponible
            Optional<LevelWord> wordOpt = levelWordRepository.findByWordKey(t.getWordKey());
            if (wordOpt.isPresent()) {
                LevelWord word = wordOpt.get();
                result.put("category", word.getCategory());
                result.put("points", word.getPoints());
                result.put("level", word.getLevelNumber());
            }
            
            result.put("gifUrl", t.getGifUrl());
            result.put("audioUrl", t.getAudioUrl());
            
            results.add(result);
        }
        
        return results;
    }
    
    @Transactional(readOnly = true)
    public List<Map<String, Object>> getWordsByCategory(String category, String language) {
        
        List<LevelWord> words = levelWordRepository.findByCategory(category);
        
        if (words.isEmpty()) {
            throw new RuntimeException("No words found in category: " + category);
        }
        
        List<Map<String, Object>> result = new ArrayList<>();
        
        for (LevelWord word : words) {
            Optional<Translation> translationOpt = translationRepository
                    .findByWordKeyAndLanguageCode(word.getWordKey(), language);
            
            if (translationOpt.isPresent()) {
                Translation translation = translationOpt.get();
                
                Map<String, Object> wordData = new HashMap<>();
                wordData.put("wordKey", word.getWordKey());
                wordData.put("text", translation.getText());
                wordData.put("category", word.getCategory());
                wordData.put("points", word.getPoints());
                wordData.put("level", word.getLevelNumber());
                wordData.put("gifUrl", translation.getGifUrl());
                wordData.put("audioUrl", translation.getAudioUrl());
                
                result.add(wordData);
            }
        }
        
        return result;
    }
    
    @Transactional(readOnly = true)
    public List<Map<String, Object>> getLanguages() {
        
        List<String> languages = translationRepository.findAll()
                .stream()
                .map(Translation::getLanguageCode)
                .distinct()
                .collect(Collectors.toList());
        
        List<Map<String, Object>> result = new ArrayList<>();
        for (String lang : languages) {
            Map<String, Object> langData = new HashMap<>();
            langData.put("code", lang);
            langData.put("name", getLanguageName(lang));
            result.add(langData);
        }
        
        return result;
    }
    
    @Transactional(readOnly = true)
    public List<Map<String, Object>> getCategories() {
        
        List<String> categories = levelWordRepository.findAll()
                .stream()
                .map(LevelWord::getCategory)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
        
        List<Map<String, Object>> result = new ArrayList<>();
        for (String category : categories) {
            Map<String, Object> categoryData = new HashMap<>();
            categoryData.put("name", category);
            
            // Compter les mots par catégorie
            long wordCount = levelWordRepository.findAll()
                    .stream()
                    .filter(w -> category.equals(w.getCategory()))
                    .count();
            categoryData.put("wordCount", wordCount);
            
            result.add(categoryData);
        }
        
        return result;
    }
    
    private String getLanguageName(String code) {
        switch (code.toLowerCase()) {
            case "ar": return "Arabic";
            case "fr": return "French";
            case "en": return "English";
            case "es": return "Spanish";
            default: return code.toUpperCase();
        }
    }
}