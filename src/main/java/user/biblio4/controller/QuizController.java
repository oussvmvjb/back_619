package user.biblio4.controller;

import user.biblio4.service.QuizService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/quiz")
public class QuizController {

    private final QuizService quizService;

    @Autowired
    public QuizController(QuizService quizService) {
        this.quizService = quizService;
    }

    @PostMapping("/start")
    public ResponseEntity<Map<String, Object>> startQuiz(
            @RequestParam Long userId,
            @RequestParam Integer levelNumber) {

        try {
            Map<String, Object> quizSession = quizService.startQuiz(userId, levelNumber);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("session", quizSession);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    @PostMapping("/submit")
    public ResponseEntity<Map<String, Object>> submitQuiz(
            @RequestBody Map<String, Object> submissionData) {

        try {
            Long userId = Long.valueOf(submissionData.get("userId").toString());
            Integer levelNumber = Integer.valueOf(submissionData.get("levelNumber").toString());

            @SuppressWarnings("unchecked")
            Map<String, Object> submission = (Map<String, Object>) submissionData.get("submission");

            Map<String, Object> result = quizService.submitQuiz(userId, levelNumber, submission);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("result", result);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    @GetMapping("/history")
    public ResponseEntity<Map<String, Object>> getQuizHistory(@RequestParam Long userId) {
        try {
            java.util.List<Map<String, Object>> history = quizService.getQuizHistory(userId);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("history", history);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    @GetMapping("/result")
    public ResponseEntity<Map<String, Object>> getQuizResult(
            @RequestParam Long userId,
            @RequestParam Integer levelNumber) {

        try {
            Map<String, Object> result = quizService.getQuizResult(userId, levelNumber);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("result", result);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    @PostMapping("/retake")
    public ResponseEntity<Map<String, Object>> retakeQuiz(
            @RequestParam Long userId,
            @RequestParam Integer levelNumber) {

        try {
            Map<String, Object> result = quizService.retakeQuiz(userId, levelNumber);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("result", result);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }
}