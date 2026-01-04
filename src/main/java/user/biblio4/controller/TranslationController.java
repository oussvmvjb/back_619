package user.biblio4.controller;

import user.biblio4.service.TranslationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/translations")
public class TranslationController {

    private final TranslationService translationService;

    @Autowired
    public TranslationController(TranslationService translationService) {
        this.translationService = translationService;
    }

    @GetMapping("/test")
    public ResponseEntity<Map<String, Object>> test() {
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Translation controller is working!");
        response.put("timestamp", System.currentTimeMillis());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/word")
    public ResponseEntity<Map<String, Object>> getWordTranslation(
            @RequestParam String wordKey,
            @RequestParam(defaultValue = "ar") String language) {

        try {
            Map<String, Object> translation = translationService.getWordTranslation(wordKey, language);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("translation", translation);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", e.getMessage());
            error.put("wordKey", wordKey);
            error.put("language", language);
            return ResponseEntity.badRequest().body(error);
        }
    }

    @GetMapping("/all")
    public ResponseEntity<Map<String, Object>> getAllTranslations(@RequestParam String wordKey) {
        try {
            Map<String, Object> translations = translationService.getAllTranslations(wordKey);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("translations", translations);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", e.getMessage());
            error.put("wordKey", wordKey);
            return ResponseEntity.badRequest().body(error);
        }
    }

    @GetMapping("/search")
    public ResponseEntity<Map<String, Object>> searchWords(
            @RequestParam String query,
            @RequestParam(defaultValue = "ar") String language) {

        try {
            java.util.List<Map<String, Object>> results = translationService.searchWords(query, language);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("query", query);
            response.put("language", language);
            response.put("results", results);
            response.put("count", results.size());

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", e.getMessage());
            error.put("query", query);
            error.put("language", language);
            return ResponseEntity.badRequest().body(error);
        }
    }

    @GetMapping("/category")
    public ResponseEntity<Map<String, Object>> getWordsByCategory(
            @RequestParam String category,
            @RequestParam(defaultValue = "ar") String language) {

        try {
            java.util.List<Map<String, Object>> words = translationService.getWordsByCategory(category, language);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("category", category);
            response.put("language", language);
            response.put("words", words);
            response.put("count", words.size());

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", e.getMessage());
            error.put("category", category);
            error.put("language", language);
            return ResponseEntity.badRequest().body(error);
        }
    }

    @GetMapping("/languages")
    public ResponseEntity<Map<String, Object>> getAvailableLanguages() {
        try {
            java.util.List<Map<String, Object>> languages = translationService.getLanguages();

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("languages", languages);
            response.put("count", languages.size());

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    @GetMapping("/categories")
    public ResponseEntity<Map<String, Object>> getAvailableCategories() {
        try {
            java.util.List<Map<String, Object>> categories = translationService.getCategories();

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("categories", categories);
            response.put("count", categories.size());

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> healthCheck() {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "UP");
        response.put("service", "TranslationService");
        response.put("timestamp", System.currentTimeMillis());
        return ResponseEntity.ok(response);
    }
}