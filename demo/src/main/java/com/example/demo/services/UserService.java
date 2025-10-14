package com.example.demo.services;

import com.example.demo.models.User;
import com.example.demo.repositories.UserRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class UserService {
    private final UserRepository userRepository;
    public UserService(UserRepository userRepository){
        this.userRepository = userRepository;
    }
    public User findById(int id){
        return userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User with id : "+id+" not found"));
    }
    public long getUsersCount() {
        return userRepository.count();
    }
    public User findByLastName(String lastName){
        return userRepository.findByLastName(lastName)
                .orElseThrow(() -> new RuntimeException("User with lastname : "+lastName+" not found"));
    }
    public User findByFirstName(String firstName){
        return userRepository.findByFirstName(firstName)
                .orElseThrow(() -> new RuntimeException("User with firstname : "+firstName+" not found"));
    }
    public User findByEmail(String email){
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User with email : "+email+" not found"));
    }
    
 
    public User findByEmailSafe(String email){
        return userRepository.findByEmail(email).orElse(null);
    }
    
    public List<User> findByRole(String role){
        return userRepository.findByRole(role);
    }
    public List<User> getAllUser(){
        return userRepository.findAll();
    }

    public User createUser(User user){
        return userRepository.save(user);
    }

    public User updateUser(int id, User user){
        User modifiedUser = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User with id : "+id+" not found"));
        if(user.getFirstName() != null){
            modifiedUser.setFirstName(user.getFirstName());
        }
        if(user.getLastName() != null){
            modifiedUser.setLastName(user.getLastName());
        }
        if(user.getEmail() != null){
            modifiedUser.setEmail(user.getEmail());
        }
        if(user.getRole() != null){
            modifiedUser.setRole(user.getRole());
        }
        return userRepository.save(modifiedUser);
    }

    public void deleteUser(int id){
        userRepository.deleteById(id);
    }
    public boolean existsByEmail(String email) {
        return userRepository.existsByEmail(email);
    }

}
