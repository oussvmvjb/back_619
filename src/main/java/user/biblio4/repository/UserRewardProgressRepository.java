package user.biblio4.repository;

import user.biblio4.model.UserRewardProgress;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface UserRewardProgressRepository extends JpaRepository<UserRewardProgress, Long> {
    Optional<UserRewardProgress> findByUserId(Long userId);
    boolean existsByUserId(Long userId);
}