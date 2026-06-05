package com.team01.freelance.user.dto;

import java.util.ArrayList;
import java.util.List;

public class UserActivityFeedDTO {
    private List<UserActivityEventDTO> content = new ArrayList<>();
    private int page;
    private int size;
    private long totalElements;
    private int totalPages;

    public UserActivityFeedDTO() {
    }

    public UserActivityFeedDTO(List<UserActivityEventDTO> content, int page, int size,
                               long totalElements, int totalPages) {
        this.content = content != null ? new ArrayList<>(content) : new ArrayList<>();
        this.page = page;
        this.size = size;
        this.totalElements = totalElements;
        this.totalPages = totalPages;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private List<UserActivityEventDTO> content = new ArrayList<>();
        private int page;
        private int size;
        private long totalElements;
        private int totalPages;

        public Builder content(List<UserActivityEventDTO> content) {
            this.content = content != null ? new ArrayList<>(content) : new ArrayList<>();
            return this;
        }

        public Builder page(int page) {
            this.page = page;
            return this;
        }

        public Builder size(int size) {
            this.size = size;
            return this;
        }

        public Builder totalElements(long totalElements) {
            this.totalElements = totalElements;
            return this;
        }

        public Builder totalPages(int totalPages) {
            this.totalPages = totalPages;
            return this;
        }

        public UserActivityFeedDTO build() {
            return new UserActivityFeedDTO(content, page, size, totalElements, totalPages);
        }
    }

    public List<UserActivityEventDTO> getContent() {
        return content;
    }

    public int getPage() {
        return page;
    }

    public int getSize() {
        return size;
    }

    public long getTotalElements() {
        return totalElements;
    }

    public int getTotalPages() {
        return totalPages;
    }

    public void setContent(List<UserActivityEventDTO> content) {
        this.content = content != null ? new ArrayList<>(content) : new ArrayList<>();
    }

    public void setPage(int page) {
        this.page = page;
    }

    public void setSize(int size) {
        this.size = size;
    }

    public void setTotalElements(long totalElements) {
        this.totalElements = totalElements;
    }

    public void setTotalPages(int totalPages) {
        this.totalPages = totalPages;
    }
}