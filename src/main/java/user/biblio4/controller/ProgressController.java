package user.biblio4.controller;

import user.biblio4.service.ProgressService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/progress")
public class ProgressController {

    private final ProgressService progressService;

    @Autowired
    public ProgressController(ProgressService progressService) {
        this.progressService = progressService;
    }

    @GetMapping("/overall")
    public ResponseEntity<Map<String, Object>> getOverallProgress(@RequestParam Long userId) {
        try {
            Map<String, Object> progress = progressService.getOverallProgress(userId);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("progress", progress);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    @GetMapping("/level")
    public ResponseEntity<Map<String, Object>> getLevelProgress(
            @RequestParam Long userId,
            @RequestParam Integer levelNumber) {

        try {
            Map<String, Object> progress = progressService.getLevelProgress(userId, levelNumber);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("progress", progress);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    @PostMapping("/update-streak")
    public ResponseEntity<Map<String, Object>> updateDailyStreak(@RequestParam Long userId) {
        try {
            Map<String, Object> result = progressService.updateDailyStreak(userId);

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

    @GetMapping("/leaderboard")
    public ResponseEntity<Map<String, Object>> getLeaderboard(
            @RequestParam(defaultValue = "xp") String type,
            @RequestParam(defaultValue = "10") Integer limit) {

        try {
            java.util.List<Map<String, Object>> leaderboard = progressService.getLeaderboard(type, limit);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("type", type);
            response.put("leaderboard", leaderboard);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    @GetMapping("/weekly-stats")
    public ResponseEntity<Map<String, Object>> getWeeklyStats(@RequestParam Long userId) {
        try {
            Map<String, Object> stats = progressService.getWeeklyStats(userId);

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
}