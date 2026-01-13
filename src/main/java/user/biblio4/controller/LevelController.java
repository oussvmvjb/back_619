package user.biblio4.controller;

import user.biblio4.model.UserProgress;
import user.biblio4.service.LevelService;
import user.biblio4.service.ProgressService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/levels")
public class LevelController {

    private final LevelService levelService;
    private final ProgressService progressService;

    @Autowired
    public LevelController(LevelService levelService, ProgressService progressService) {
        this.levelService = levelService;
        this.progressService = progressService;
    }

    @GetMapping("/{levelNumber}")
    public ResponseEntity<Map<String, Object>> getLevel(
            @PathVariable Integer levelNumber,
            @RequestParam Long userId,
            @RequestParam(defaultValue = "ar") String language) {

        try {
            Map<String, Object> levelData = levelService.getLevelWithProgress(userId, levelNumber, language);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("level", levelData);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    @PostMapping("/{levelNumber}/complete-word")
    public ResponseEntity<Map<String, Object>> completeWord(
            @PathVariable Integer levelNumber,
            @RequestParam Long userId,
            @RequestParam String wordKey) {

        try {
            Map<String, Object> result = levelService.completeWord(userId, levelNumber, wordKey);

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

    @PostMapping("/{levelNumber}/master-word")
    public ResponseEntity<Map<String, Object>> masterWord(
            @PathVariable Integer levelNumber,
            @RequestParam Long userId,
            @RequestParam String wordKey) {

        try {
            Map<String, Object> result = levelService.masterWord(userId, levelNumber, wordKey);

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

    @PostMapping("/unlock-next")
    public ResponseEntity<Map<String, Object>> unlockNextLevel(@RequestParam Long userId) {
        try {
            Map<String, Object> result = unlockNextLevelLogic(userId);
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
    private Map<String, Object> unlockNextLevelLogic(Long userId) {
        // الحصول على آخر مستوى مكتمل
        Optional<UserProgress> lastCompleted = progressService.findLastCompletedLevel(userId);

        Map<String, Object> result = new HashMap<>();

        if (lastCompleted.isEmpty()) {
            // المستخدم جديد، افتح المستوى 1
            result.put("nextLevel", 1);
            result.put("unlocked", true);
        } else {
            // المستخدم لديه مستوى مكتمل
            int nextLevel = lastCompleted.get().getLevelNumber() + 1;
            levelService.unlockSpecificLevel(userId, nextLevel); // دالة لفتح المستوى التالي
            result.put("nextLevel", nextLevel);
            result.put("unlocked", true);
        }

        return result;
    }


    @GetMapping("/user/stats")
    public ResponseEntity<Map<String, Object>> getUserStats(@RequestParam Long userId) {
        try {
            Map<String, Object> stats = levelService.getUserStats(userId);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("stats", stats);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    @GetMapping("/user/levels")
    public ResponseEntity<Map<String, Object>> getUserLevels(@RequestParam Long userId) {
        try {
            java.util.List<Map<String, Object>> levels = levelService.getUserLevels(userId);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("levels", levels);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }
}