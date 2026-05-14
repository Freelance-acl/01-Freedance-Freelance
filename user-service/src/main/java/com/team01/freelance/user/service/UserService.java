package com.team01.freelance.user.service;

import com.team01.freelance.user.dto.TopFreelancerDTO;
import com.team01.freelance.user.model.User;
import com.team01.freelance.user.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

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

    public List<TopFreelancerDTO> getTopFreelancersByEarnings(LocalDate startDate, LocalDate endDate, Integer limit) {
        if (startDate == null || endDate == null || startDate.isAfter(endDate)) {
            throw new IllegalArgumentException("startDate must be on or before endDate");
        }

        int queryLimit = limit == null ? 10 : limit;
        if (queryLimit <= 0) {
            throw new IllegalArgumentException("limit must be greater than zero");
        }

        return userRepository.findTopFreelancersByEarnings(
                        startDate.atStartOfDay(),
                        endDate.atTime(LocalTime.MAX),
                        queryLimit)
                .stream()
                .map(this::toTopFreelancerDTO)
                .toList();
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

    private TopFreelancerDTO toTopFreelancerDTO(Object[] row) {
        return new TopFreelancerDTO(
                toLong(row[0]),
                (String) row[1],
                toBigDecimal(row[2]),
                toLong(row[3]));
    }

    private Long toLong(Object value) {
        if (value instanceof Long longValue) {
            return longValue;
        }
        if (value instanceof Integer integerValue) {
            return integerValue.longValue();
        }
        if (value instanceof BigInteger bigIntegerValue) {
            return bigIntegerValue.longValue();
        }
        if (value instanceof BigDecimal bigDecimalValue) {
            return bigDecimalValue.longValue();
        }
        if (value instanceof Number numberValue) {
            return numberValue.longValue();
        }
        return Long.valueOf(value.toString());
    }

    private BigDecimal toBigDecimal(Object value) {
        if (value instanceof BigDecimal bigDecimalValue) {
            return bigDecimalValue;
        }
        if (value instanceof BigInteger bigIntegerValue) {
            return new BigDecimal(bigIntegerValue);
        }
        if (value instanceof Number numberValue) {
            return BigDecimal.valueOf(numberValue.doubleValue());
        }
        return new BigDecimal(value.toString());
    }
}
