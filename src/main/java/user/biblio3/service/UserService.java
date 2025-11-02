package user.biblio3.service;

import user.biblio3.model.User;
import user.biblio3.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class UserService {
    
    @Autowired
    private UserRepository userRepository;
    
    public User saveUser(User user) {
        return userRepository.save(user);
    }
    
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }
    
    public Optional<User> getUserById(Long id) {
        return userRepository.findById(id);
    }
    
    public List<User> getUsersByNomComplet(String nomComplet) {
        return userRepository.findByNomCompletContainingIgnoreCase(nomComplet);
    }
    
    public User updateUser(Long id, User userDetails) {
        Optional<User> userOptional = userRepository.findById(id);
        
        if (userOptional.isPresent()) {
            User user = userOptional.get();
            user.setNomComplet(userDetails.getNomComplet());
            user.setEmail(userDetails.getEmail());
            user.setUsername(userDetails.getUsername());
            user.setPassword(userDetails.getPassword());
            
            return userRepository.save(user);
        }
        
        return null;
    }
    
    public boolean deleteUser(Long id) {
        if (userRepository.existsById(id)) {
            userRepository.deleteById(id);
            return true;
        }
        return false;
    }
    
    public boolean emailExists(String email) {
        return userRepository.existsByEmail(email);
    }
    
    public boolean usernameExists(String username) {
        return userRepository.existsByUsername(username);
    }
}