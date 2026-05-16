package com.team01.freelance.user.dto;

import java.util.List;
import java.util.Map;

public class UserProfileDTO {
    private Long userId;
    private String name;
    private String email;
    private String phone;
    private Map<String, Object> preferences;
    private List<UserProfileSkillDTO> skills;
    private int totalSkills;

    public UserProfileDTO() {
    }

    public UserProfileDTO(Long userId, String name, String email, String phone,
                          Map<String, Object> preferences,
                          List<UserProfileSkillDTO> skills,
                          int totalSkills) {
        this.userId = userId;
        this.name = name;
        this.email = email;
        this.phone = phone;
        this.preferences = preferences;
        this.skills = skills;
        this.totalSkills = totalSkills;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public Map<String, Object> getPreferences() {
        return preferences;
    }

    public void setPreferences(Map<String, Object> preferences) {
        this.preferences = preferences;
    }

    public List<UserProfileSkillDTO> getSkills() {
        return skills;
    }

    public void setSkills(List<UserProfileSkillDTO> skills) {
        this.skills = skills;
    }

    public int getTotalSkills() {
        return totalSkills;
    }

    public void setTotalSkills(int totalSkills) {
        this.totalSkills = totalSkills;
    }
}