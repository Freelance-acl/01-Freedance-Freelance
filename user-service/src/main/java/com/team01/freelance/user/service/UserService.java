package com.team01.freelance.user.service;

import com.team01.freelance.user.model.User;
import com.team01.freelance.user.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.team01.freelance.user.model.UserRole;
import java.util.List;
import java.util.Optional;
import java.util.Map;
import java.util.HashMap;
import jakarta.transaction.Transactional;

@Service
public class UserService {

    @Autowired
    private UserRepository userRepository;

    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    public Optional<User> getUserById(Long id) {
        return userRepository.findById(id);
    }

    public User createUser(User user) {
        return userRepository.save(user);
    }

    /**
     * Updates an existing user and throws if it does not exist.
     *
     * @param id The ID of the user to update
     * @param userDetails The object containing updated fields
     * @return The updated user
     * @throws EntityNotFoundException if the user is not found
     */
    public User updateUser(Long id, User userDetails) {
        return userRepository.findById(id).map(existingUser -> {
            if (userDetails.getId() != null && !id.equals(userDetails.getId())) {
                throw new IllegalArgumentException("User ID cannot be changed. Use the path ID only.");
            }
            if (userDetails.getName() != null) existingUser.setName(userDetails.getName());
            if (userDetails.getEmail() != null) existingUser.setEmail(userDetails.getEmail());
            if (userDetails.getPassword() != null) existingUser.setPassword(userDetails.getPassword());
            if (userDetails.getPhone() != null) existingUser.setPhone(userDetails.getPhone());
            if (userDetails.getRole() != null) existingUser.setRole(userDetails.getRole());
            if (userDetails.getStatus() != null) existingUser.setStatus(userDetails.getStatus());
            if (userDetails.getPreferences() != null) existingUser.setPreferences(userDetails.getPreferences());
            if (userDetails.getCreatedAt() != null) existingUser.setCreatedAt(userDetails.getCreatedAt());
            return userRepository.save(existingUser);
        }).orElseThrow(() -> new EntityNotFoundException("User not found with id: " + id));
    }

    public boolean deleteUserById(Long id) {
        if (!userRepository.existsById(id)) {
            return false;
        }
        userRepository.deleteById(id);
        return true;
    }

    public void deleteAllUsers() {
        userRepository.deleteAll();
    }

    public List<User> searchUsers(String name, String email, UserRole role) {
        return userRepository.searchUsers(name, email, role);
    }

    @Transactional
    public User updatePreferences(Long id, Map<String, Object> incomingPreferences) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("User not found with id: " + id));

        Map<String, Object> merged = user.getPreferences() != null
                ? new HashMap<>(user.getPreferences())
                : new HashMap<>();

        if (incomingPreferences != null) {
            merged.putAll(incomingPreferences);
        }

        user.setPreferences(merged);
        return userRepository.save(user);
    }
}
