package com.smarthealth.hms.service;

import com.smarthealth.hms.domain.Role;
import com.smarthealth.hms.domain.User;
import com.smarthealth.hms.exception.BusinessRuleException;
import com.smarthealth.hms.exception.ResourceNotFoundException;
import com.smarthealth.hms.repository.UserRepository;
import com.smarthealth.hms.security.AppUserDetails;
import com.smarthealth.hms.util.PasswordPolicy;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class UserService {

    private final UserRepository users;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository users, PasswordEncoder passwordEncoder) {
        this.users = users;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public User createUser(String username, String rawPassword, String fullName, String email, Role role) {
        String name = username.trim();
        String mail = email.trim();
        PasswordPolicy.validate(rawPassword);
        if (users.existsByUsernameIgnoreCase(name)) {
            throw new BusinessRuleException("Username '" + name + "' is already taken.");
        }
        if (users.existsByEmailIgnoreCase(mail)) {
            throw new BusinessRuleException("An account with email '" + mail + "' already exists.");
        }
        User user = new User();
        user.setUsername(name);
        user.setPasswordHash(passwordEncoder.encode(rawPassword));
        user.setFullName(fullName.trim());
        user.setEmail(mail);
        user.setRole(role);
        return users.save(user);
    }

    @Transactional
    public void updateProfile(User user, String fullName, String email) {
        String mail = email.trim();
        if (!user.getEmail().equalsIgnoreCase(mail) && users.existsByEmailIgnoreCase(mail)) {
            throw new BusinessRuleException("An account with email '" + mail + "' already exists.");
        }
        user.setFullName(fullName.trim());
        user.setEmail(mail);
        users.save(user);
    }

    @Transactional(readOnly = true)
    public List<User> findAll() {
        return users.findAllByOrderByRoleAscFullNameAsc();
    }

    @Transactional
    public void setEnabled(Long id, boolean enabled, AppUserDetails actor) {
        User user = users.findById(id).orElseThrow(() -> new ResourceNotFoundException("User not found"));
        if (!enabled && user.getId().equals(actor.getUserId())) {
            throw new BusinessRuleException("You cannot disable your own account.");
        }
        user.setEnabled(enabled);
    }

    @Transactional
    public void resetPassword(Long id, String newPassword) {
        PasswordPolicy.validate(newPassword);
        User user = users.findById(id).orElseThrow(() -> new ResourceNotFoundException("User not found"));
        user.setPasswordHash(passwordEncoder.encode(newPassword));
    }

    @Transactional
    public void delete(User user) {
        users.delete(user);
    }
}
