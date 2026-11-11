package com.smarthealth.hms.security;

import com.smarthealth.hms.repository.UserRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class AppUserDetailsService implements UserDetailsService {

    private final UserRepository users;
    private final LoginAttemptService loginAttempts;

    public AppUserDetailsService(UserRepository users, LoginAttemptService loginAttempts) {
        this.users = users;
        this.loginAttempts = loginAttempts;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        return users.findByUsernameIgnoreCase(username)
                .map(u -> new AppUserDetails(u, !loginAttempts.isBlocked(username)))
                .orElseThrow(() -> new UsernameNotFoundException("Invalid credentials"));
    }
}
