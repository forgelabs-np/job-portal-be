package com.jobportal.v1.service;


import com.jobportal.v1.entity.User;

import java.util.Optional;

public interface UserService {
    Optional<User> findByEmail(String email);
    boolean existsByEmail(String email);
    User save(User user);
}