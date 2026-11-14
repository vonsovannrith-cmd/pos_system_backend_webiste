package com.example.pos_system.security;

import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.userdetails.User;

public class AppUserDetails extends User {

    private final int tokenVersion;

    public AppUserDetails(String username, String password, boolean enabled, String role, int tokenVersion) {
        super(username, password, enabled, true, true, true,
                AuthorityUtils.createAuthorityList("ROLE_" + role));
        this.tokenVersion = tokenVersion;
    }

    public int getTokenVersion() {
        return tokenVersion;
    }
}
