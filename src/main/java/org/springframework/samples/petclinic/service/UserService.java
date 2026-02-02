package org.springframework.samples.petclinic.service;

import org.springframework.dao.DataAccessException;
import org.springframework.samples.petclinic.model.User;

public interface UserService {

    void saveUser(User user);

    User findByUsername(String username) throws DataAccessException;

    boolean validateCredentials(String username, String password);
}
