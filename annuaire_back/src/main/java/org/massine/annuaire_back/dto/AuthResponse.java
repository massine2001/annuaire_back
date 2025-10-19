package org.massine.annuaire_back.dto;

import org.massine.annuaire_back.models.User;

public class AuthResponse {
    private User user;

    public AuthResponse(User user) {
        this.user = user;
    }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }
}
