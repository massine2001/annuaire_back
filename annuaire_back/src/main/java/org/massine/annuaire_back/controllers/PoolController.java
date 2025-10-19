package org.massine.annuaire_back.controllers;

import com.example.demo.dto.AcceptInvitationRequest;
import com.example.demo.dto.InvitationRequest;
import com.example.demo.models.Access;
import com.example.demo.models.File;
import com.example.demo.models.Pool;
import com.example.demo.models.User;
import com.example.demo.repositories.AccessRepository;
import com.example.demo.repositories.UserRepository;
import com.example.demo.services.AccessService;
import com.example.demo.services.FileService;
import com.example.demo.services.PoolService;
import com.example.demo.services.UserService;
import com.example.demo.services.JwtService;
import com.example.demo.services.CookieService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/pool/")
public class PoolController {
    private final PoolService poolService;
    private final AccessService accessService;
    private final FileService fileService;
    private final UserService userService;
    private final UserRepository userRepository;
    private final AccessRepository accessRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final CookieService cookieService;


    public PoolController(
            PoolService poolService,
            AccessService accessService,
            FileService fileService,
            UserService userService,
            UserRepository userRepository,
            AccessRepository accessRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService,
            CookieService cookieService
    ) {
        this.poolService = poolService;
        this.accessService = accessService;
        this.fileService = fileService;
        this.userService = userService;
        this.userRepository = userRepository;
        this.accessRepository = accessRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.cookieService = cookieService;
    }

    /**
     * Récupère l'utilisateur actuellement connecté depuis le contexte de sécurité.
     * Le principal contient l'email de l'utilisateur (String), pas l'objet User complet.
     */
    private User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof String)) {
            return null;
        }
        String email = (String) authentication.getPrincipal();
        return userService.findByEmailSafe(email);
    }

    @GetMapping("/")
    public ResponseEntity<List<Pool>> getPools() {
        User currentUser = getCurrentUser();
        if (currentUser == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        List<Pool> pools = poolService.getAllPoolsByUserId(currentUser.getId());

        if(pools.isEmpty()) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.ok(pools);
    }

    @GetMapping("/count")
    public ResponseEntity<Long> getPoolsCount() {
        long count = poolService.getPoolsCount();
        return ResponseEntity.ok(count);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Pool> getPoolById(@PathVariable("id") int id) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        User currentUser = getCurrentUser();
        if (currentUser == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        Pool pool = poolService.getPoolById(id);

        if(pool == null) {
            return ResponseEntity.notFound().build();
        }

        if (!accessService.userHasAccessToPool(currentUser.getId(), id)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        return ResponseEntity.ok(pool);
    }


    @PostMapping("/")
    public ResponseEntity<Pool> createPool(@RequestBody Pool pool) {
        User currentUser = getCurrentUser();
        if (currentUser == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        pool.setCreatedBy(currentUser.getId());

        Pool createdPool = poolService.savePool(pool);

        Access ownerAccess = new Access();
        ownerAccess.setUser(currentUser);
        ownerAccess.setPool(createdPool);
        ownerAccess.setRole("owner");
        accessService.saveAccess(ownerAccess);

        return ResponseEntity.status(HttpStatus.CREATED).body(createdPool);
    }
    @PutMapping("/{id}")
    public ResponseEntity<Pool> updatePool(
            @PathVariable int id,
            @RequestBody Pool pool
    ) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        User currentUser = getCurrentUser();
        if (currentUser == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        Pool existingPool = poolService.getPoolById(id);

        if (existingPool == null) {
            return ResponseEntity.notFound().build();
        }

        if (!accessService.userIsOwnerOrAdmin(currentUser.getId(), id)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        Pool updatedPool = poolService.updatePool(id, pool);
        return ResponseEntity.ok(updatedPool);
    }


    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletePool(@PathVariable("id") int id) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        User currentUser = getCurrentUser();
        if (currentUser == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        Pool pool = poolService.getPoolById(id);

        if (pool == null) {
            return ResponseEntity.notFound().build();
        }

        Access access = accessService.getUserAccessToPool(currentUser.getId(), id);
        if (access == null || !"owner".equalsIgnoreCase(access.getRole())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        poolService.deletePoolById(id);
        return ResponseEntity.noContent().build();
    }
    @GetMapping("/users/{id}")
    public ResponseEntity<List<User>> getUsersFromPool(@PathVariable int id){
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        User currentUser = getCurrentUser();
        if (currentUser == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        if (!accessService.userHasAccessToPool(currentUser.getId(), id)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        List<User> users = accessService.getUsersFromPool(id);
        if(users.isEmpty()) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.ok(users);
    }
    @GetMapping("/users/count/{id}")
    public int countUsersByPool(@PathVariable int id){
        return accessService.countUsersByPool(id);
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<Pool>> getAllPoolsByUserId(@PathVariable int userId) {
        List<Pool> pools = poolService.getAllPoolsByUserId(userId);
        if (pools.isEmpty()) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.ok(pools);
    }
    @GetMapping("/stats/{poolId}")
    public ResponseEntity<Map<String, Object>> getPoolStats(@PathVariable int poolId) {
        Pool pool = poolService.getPoolById(poolId);

        if (pool == null) {
            return ResponseEntity.notFound().build();
        }

        boolean isPublicPool = pool.getPublicAccess() != null && pool.getPublicAccess();

        if (!isPublicPool) {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication == null || !authentication.isAuthenticated()) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }

            User currentUser = getCurrentUser();
            if (currentUser == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }

            if (!accessService.userHasAccessToPool(currentUser.getId(), poolId)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }
        }

        Map<String, Object> stats = new HashMap<>();


        // ==================== INFOS DE BASE ====================
        stats.put("pool", pool);

        // ==================== STATS MEMBRES ====================
        List<User> members = accessService.getUsersFromPool(poolId);
        List<Access> accesses = accessService.getAccessesByPool(poolId);

        stats.put("membersCount", members.size());
        stats.put("members", members);
        stats.put("accesses", accesses);

        // Répartition par rôle dans la pool
        Map<String, Long> roleDistribution = accesses.stream()
                .filter(access -> access.getRole() != null)
                .collect(Collectors.groupingBy(Access::getRole, Collectors.counting()));
        stats.put("roleDistribution", roleDistribution);

        // Répartition par rôle global des users
        Map<String, Long> userRoleDistribution = members.stream()
                .filter(user -> user.getRole() != null)
                .collect(Collectors.groupingBy(User::getRole, Collectors.counting()));
        stats.put("userRoleDistribution", userRoleDistribution);

        // ==================== STATS FICHIERS ====================
        List<File> files = fileService.findByPoolId(poolId);

        stats.put("filesCount", files.size());
        stats.put("files", files);

        // Top uploaders
        Map<User, Long> uploaderStats = files.stream()
                .filter(file -> file.getUserUploader() != null)
                .collect(Collectors.groupingBy(File::getUserUploader, Collectors.counting()));
        List<Map.Entry<User, Long>> topUploaders = uploaderStats.entrySet().stream()
                .sorted(Map.Entry.<User, Long>comparingByValue().reversed())
                .limit(5)
                .collect(Collectors.toList());
        stats.put("topUploaders", topUploaders);

        // Fichiers par jour (7 derniers jours)
        Map<String, Long> filesPerDay = files.stream()
                .filter(file -> file.getCreatedAt() != null)
                .collect(Collectors.groupingBy(
                        file -> file.getCreatedAt().toString().substring(0, 10),
                        Collectors.counting()
                ));
        stats.put("filesPerDay", filesPerDay);

        // Dernier fichier uploadé
        Optional<File> lastFile = files.stream()
                .filter(file -> file.getCreatedAt() != null)
                .max(Comparator.comparing(File::getCreatedAt));
        stats.put("lastFile", lastFile.orElse(null));

        // ==================== STATS ACTIVITÉ ====================
        // Membres les plus actifs (par nombre de fichiers)
        stats.put("mostActiveMembers", topUploaders);

        // Membres inactifs (0 fichiers uploadés)
        List<User> inactiveMembers = members.stream()
                .filter(user -> files.stream().noneMatch(f -> f.getUserUploader() != null && f.getUserUploader().getId().equals(user.getId())))
                .collect(Collectors.toList());
        stats.put("inactiveMembers", inactiveMembers);
        stats.put("inactiveMembersCount", inactiveMembers.size());

        // ==================== STATS TEMPORELLES ====================
        // Date de création de la pool
        stats.put("poolCreatedAt", pool.getCreatedAt());

        // Âge de la pool en jours
        long poolAgeInDays = 0;
        if (pool.getCreatedAt() != null) {
            poolAgeInDays = ChronoUnit.DAYS.between(
                    pool.getCreatedAt(),
                    Instant.now()
            );
        }
        stats.put("poolAgeInDays", poolAgeInDays);

        // Nouveau membre le plus récent
        Optional<User> newestMember = members.stream()
                .filter(user -> user.getCreatedAt() != null)
                .max(Comparator.comparing(User::getCreatedAt));
        stats.put("newestMember", newestMember.orElse(null));

        // Membre le plus ancien
        Optional<User> oldestMember = members.stream()
                .filter(user -> user.getCreatedAt() != null)
                .min(Comparator.comparing(User::getCreatedAt));
        stats.put("oldestMember", oldestMember.orElse(null));

        // ==================== STATS AVANCÉES ====================
        // Moyenne de fichiers par membre
        double avgFilesPerMember = members.isEmpty() ? 0 : (double) files.size() / members.size();
        stats.put("avgFilesPerMember", Math.round(avgFilesPerMember * 100.0) / 100.0);

        double activityRate = members.isEmpty() ? 0 :
                ((double) (members.size() - inactiveMembers.size()) / members.size() * 100);
        stats.put("activityRate", Math.round(activityRate * 100.0) / 100.0);

        Map<String, Long> fileExtensions = files.stream()
                .filter(f -> f.getName() != null)
                .map(f -> {
                    String name = f.getName();
                    int lastDot = name.lastIndexOf('.');
                    return lastDot > 0 ? name.substring(lastDot + 1).toLowerCase() : "sans extension";
                })
                .collect(Collectors.groupingBy(ext -> ext, Collectors.counting()));
        stats.put("fileExtensions", fileExtensions);

        if (pool.getCreatedBy() != null) {
            User creator = userService.findById(pool.getCreatedBy());
            stats.put("creator", creator);
        }

        return ResponseEntity.ok(stats);
    }
    @GetMapping("/files/{poolId}")
    public ResponseEntity<List<File>> getFilesOfPool(@PathVariable int poolId) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        User currentUser = getCurrentUser();
        if (currentUser == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        Pool pool = poolService.getPoolById(poolId);

        if (pool == null) {
            return ResponseEntity.notFound().build();
        }

        if (!accessService.userHasAccessToPool(currentUser.getId(), poolId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        List<File> files = fileService.findByPoolId(poolId);
        if (files.isEmpty()) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.ok(files);
    }

    @GetMapping("/files/count/{poolId}")
    public ResponseEntity<Long> getFilesCountOfPool(@PathVariable int poolId) {
        Pool pool = poolService.getPoolById(poolId);
        if (pool == null) {
            return ResponseEntity.notFound().build();
        }

        long count = fileService.findByPoolId(poolId).size();
        return ResponseEntity.ok(count);
    }



    @PostMapping("/invitations/generate-token")
    public ResponseEntity<?> generateInvitationToken(
            @RequestBody InvitationRequest request,
            Principal principal
    ) {
        Optional<User> currentUserOpt = userRepository.findByEmail(principal.getName());
        if (currentUserOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        User currentUser = currentUserOpt.get();
        Optional<Access> accessOpt = accessRepository.findByUserIdAndPoolId(currentUser.getId(), request.getPoolId().intValue());

        if (accessOpt.isEmpty() || (!accessOpt.get().getRole().equals("owner") && !accessOpt.get().getRole().equals("admin"))) {
            return ResponseEntity.status(403).body("Seuls les admins/owners peuvent inviter");
        }

        Optional<User> existingUserOpt = userRepository.findByEmail(request.getEmail());
        if (existingUserOpt.isPresent()) {
            Optional<Access> existingAccessOpt = accessRepository.findByUserIdAndPoolId(existingUserOpt.get().getId(), request.getPoolId().intValue());
            if (existingAccessOpt.isPresent()) {
                return ResponseEntity.badRequest().body("Cet utilisateur est déjà membre du pool");
            }
        }

        String invitationToken = jwtService.generateInvitationToken(request.getEmail(), request.getPoolId(), currentUser.getId());

        return ResponseEntity.ok(Map.of(
                "success", true,
                "token", invitationToken,
                "expiresAt", new Date(System.currentTimeMillis() + 7 * 24 * 60 * 60 * 1000)
        ));
    }




    @GetMapping("/invitations/validate/{token}")
    public ResponseEntity<?> validateInvitationToken(@PathVariable String token) {
        try {
            Claims claims = jwtService.validateToken(token);

            if (!"invitation".equals(claims.get("type"))) {
                return ResponseEntity.badRequest().body(Map.of(
                        "valid", false,
                        "message", "Ce token n'est pas un token d'invitation"
                ));
            }

            String email = claims.get("email", String.class);
            Integer poolId = claims.get("poolId", Integer.class);

            Pool pool = poolService.getPoolById(poolId);
            if (pool == null) {
                return ResponseEntity.status(404).body(Map.of(
                        "valid", false,
                        "message", "Ce pool n'existe plus"
                ));
            }

            Optional<User> existingUserOpt = userRepository.findByEmail(email);
            if (existingUserOpt.isPresent()) {
                Optional<Access> existingAccessOpt = accessRepository.findByUserIdAndPoolId(existingUserOpt.get().getId(), poolId);
                if (existingAccessOpt.isPresent()) {
                    return ResponseEntity.badRequest().body(Map.of(
                            "valid", false,
                            "message", "Vous êtes déjà membre de ce pool. Connectez-vous."
                    ));
                }
            }

            return ResponseEntity.ok(Map.of(
                    "valid", true,
                    "email", email,
                    "poolId", poolId,
                    "poolName", pool.getName(),
                    "expiresAt", claims.getExpiration()
            ));

        } catch (ExpiredJwtException e) {
            return ResponseEntity.status(410).body(Map.of(
                    "valid", false,
                    "message", "Ce lien d'invitation a expiré"
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "valid", false,
                    "message", "Token d'invitation invalide"
            ));
        }
    }




    @PostMapping("/invitations/accept")
    public ResponseEntity<?> acceptInvitation(@RequestBody AcceptInvitationRequest request) {
        try {
            Claims claims = jwtService.validateToken(request.getToken());

            if (!"invitation".equals(claims.get("type"))) {
                return ResponseEntity.badRequest().body(Map.of(
                        "success", false,
                        "message", "Token invalide"
                ));
            }

            String email = claims.get("email", String.class);
            Integer poolId = claims.get("poolId", Integer.class);

            if (!email.equalsIgnoreCase(request.getEmail())) {
                return ResponseEntity.badRequest().body(Map.of(
                        "success", false,
                        "message", "L'email ne correspond pas à l'invitation"
                ));
            }

            Optional<User> existingUserOpt = userRepository.findByEmail(email);
            User user;

            if (existingUserOpt.isPresent()) {
                user = existingUserOpt.get();

                Optional<Access> existingAccessOpt = accessRepository.findByUserIdAndPoolId(user.getId(), poolId);
                if (existingAccessOpt.isPresent()) {
                    return ResponseEntity.badRequest().body(Map.of(
                            "success", false,
                            "message", "Vous êtes déjà membre de ce pool. Connectez-vous pour y accéder."
                    ));
                }

                Pool pool = poolService.getPoolById(poolId);
                if (pool == null) {
                    return ResponseEntity.status(404).body(Map.of(
                            "success", false,
                            "message", "Le pool n'existe plus"
                    ));
                }

                Access access = new Access();
                access.setUser(user);
                access.setPool(pool);
                access.setRole("member");
                accessRepository.save(access);

                String authToken = jwtService.generateToken(user.getId(), user.getEmail(), user.getRole());
                String cookie = cookieService.createAuthCookie(authToken);

                return ResponseEntity.ok()
                        .header(HttpHeaders.SET_COOKIE, cookie)
                        .body(Map.of(
                                "success", true,
                                "message", "Vous avez rejoint le pool avec succès !",
                                "user", Map.of(
                                        "id", user.getId(),
                                        "email", user.getEmail(),
                                        "firstName", user.getFirstName(),
                                        "lastName", user.getLastName()
                                )
                        ));

            } else {
                User newUser = new User();
                newUser.setEmail(email);
                newUser.setFirstName(request.getFirstName());
                newUser.setLastName(request.getLastName());
                newUser.setPassword(passwordEncoder.encode(request.getPassword()));
                newUser.setRole("USER");
                newUser = userRepository.save(newUser);

                Pool pool = poolService.getPoolById(poolId);
                if (pool == null) {
                    userRepository.delete(newUser);
                    return ResponseEntity.status(404).body(Map.of(
                            "success", false,
                            "message", "Le pool n'existe plus"
                    ));
                }

                Access access = new Access();
                access.setUser(newUser);
                access.setPool(pool);
                access.setRole("member");
                accessRepository.save(access);

                String authToken = jwtService.generateToken(newUser.getId(), newUser.getEmail(), newUser.getRole());
                String cookie = cookieService.createAuthCookie(authToken);

                return ResponseEntity.ok()
                        .header(HttpHeaders.SET_COOKIE, cookie)
                        .body(Map.of(
                                "success", true,
                                "message", "Compte créé et ajouté au pool avec succès !",
                                "user", Map.of(
                                        "id", newUser.getId(),
                                        "email", newUser.getEmail(),
                                        "firstName", newUser.getFirstName(),
                                        "lastName", newUser.getLastName()
                                )
                        ));
            }

        } catch (ExpiredJwtException e) {
            return ResponseEntity.status(410).body(Map.of(
                    "success", false,
                    "message", "Ce lien d'invitation a expiré"
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "Erreur lors de l'acceptation de l'invitation"
            ));
        }
    }

}