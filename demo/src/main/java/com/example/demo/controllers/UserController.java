package com.example.demo.controllers;

import com.example.demo.models.Pool;
import com.example.demo.models.User;
import com.example.demo.services.AccessService;
import com.example.demo.services.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/users/")
public class UserController {
    private final UserService userService ;
    private final AccessService accessService;

    public UserController(UserService userService, AccessService accessService) {
        this.userService = userService ;
        this.accessService = accessService;
    }
    @GetMapping("/")
    public ResponseEntity<List<User>> getAllUsers() {
        List<User> user = userService.getAllUser();
        if (user.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
        }
        return ResponseEntity.ok(user);
    }
    @GetMapping("/{id}")
    public ResponseEntity<User> getUserById(@PathVariable int id) {
        User user = userService.findById(id);
        if(user == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(user);
    }

    @PostMapping("/")
    public ResponseEntity<User> createUser(@RequestBody User user) {
        User createdUser = userService.createUser(user);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdUser);
    }

    @PutMapping("/{id}")
    public ResponseEntity<User> updateUser(@PathVariable int id, @RequestBody User user) {
        User modifiedUser = userService.updateUser(id, user);
        return ResponseEntity.ok(modifiedUser);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<User> deleteUser(@PathVariable int id) {
        if (userService.findById(id) == null) {
            return ResponseEntity.notFound().build();
        }
        userService.deleteUser(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/email/{email}")
    public ResponseEntity<User> getUserByEmail(@PathVariable String email) {
        User user = userService.findByEmail(email);
        if (user == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(user);
    }
    @GetMapping("/lastname/{lastname}")
    public ResponseEntity<User> getUserByLastName(@PathVariable String lastname) {
        User user = userService.findByLastName(lastname);
        if(user == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(user);
    }
    @GetMapping("/firstname/{firstname}")
    public ResponseEntity<User>  getUserByFirstName(@PathVariable String firstname) {
        User user = userService.findByFirstName(firstname);
        if(user == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(user);
    }
    @GetMapping("/role/{role}")
    public ResponseEntity<List<User>> getUserByRole(@PathVariable String role) {
        List<User> users = userService.findByRole(role);
        if (users.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(users);
    }
    @GetMapping("/pools/{id}")
    public ResponseEntity<List<Pool>> getPoolsFromUser(@PathVariable int id){
        List<Pool> pools = accessService.getPoolsFromUser(id);
        if(pools.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(pools);
    }
    @GetMapping("/pools/count/{id}")
    public int countPoolsByUser(@PathVariable int id){
        return accessService.countPoolsByUser(id);
    }
}
