package org.example.model;

public class PasswordEntry {
    private long id;
    private String title = "";
    private String username = "";
    private String password = "";
    private String url = "";
    private String category = "默认";
    private String notes = "";
    private String createdAt = "";
    private String updatedAt = "";

    public PasswordEntry() {
    }

    public PasswordEntry copy() {
        PasswordEntry copy = new PasswordEntry();
        copy.id = id;
        copy.title = title;
        copy.username = username;
        copy.password = password;
        copy.url = url;
        copy.category = category;
        copy.notes = notes;
        copy.createdAt = createdAt;
        copy.updatedAt = updatedAt;
        return copy;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }

    public String getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(String updatedAt) {
        this.updatedAt = updatedAt;
    }
}
