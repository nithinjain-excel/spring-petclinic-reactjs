# Authentication - Baseline PRD

| Version | Date | Author | Status |
|---------|------|--------|--------|
| 1.1 | 2026-01-17 | AI Assistant | Baseline |

> **Change Log:** v1.1 - Added reference to centralized Database Migration Strategy in ARCHITECTURE.md

---

## Overview

This document outlines the current baseline implementation of authentication and authorization in the Spring PetClinic application. The system provides role-based API access control using Spring Security with HTTP Basic Authentication. **Note:** This is a baseline document capturing the existing state - the frontend currently has no login UI.

---

## Business Requirements

### Current State (Baseline)

#### User Management
- Users are stored in database with username, password, and enabled status
- Users can have multiple roles assigned
- User creation is restricted to administrators only (via API)
- No self-registration capability exists

#### Security Requirements
- API endpoints can be protected with role-based access control
- Authentication is disabled by default (`petclinic.security.enable=false`)
- When enabled, HTTP Basic Authentication is used
- Passwords are stored in plain text (not production-ready)

#### Role-Based Access
- Three roles defined: `ROLE_OWNER_ADMIN`, `ROLE_VET_ADMIN`, `ROLE_ADMIN`
- Each API endpoint can require specific roles
- Method-level security using `@PreAuthorize` annotations

### Missing Features (Gaps)
- ❌ No login page in frontend
- ❌ No registration page for new users
- ❌ No password encryption (plain text storage)
- ❌ No session/token management
- ❌ No logout functionality
- ❌ No protected routes in frontend
- ❌ No password reset capability

---

## Technical Requirements

### Database Schema

#### Users Table

```sql
-- Location: src/main/resources/db/hsqldb/initDB.sql

CREATE TABLE users (
  username    VARCHAR(20) NOT NULL,
  password    VARCHAR(20) NOT NULL,
  enabled     BOOLEAN DEFAULT TRUE NOT NULL,
  PRIMARY KEY (username)
);
```

| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| username | VARCHAR(20) | PK, NOT NULL | Unique user identifier |
| password | VARCHAR(20) | NOT NULL | Plain text with `{noop}` prefix |
| enabled | BOOLEAN | DEFAULT TRUE | Account active status |

#### Roles Table

```sql
-- Location: src/main/resources/db/hsqldb/initDB.sql

CREATE TABLE roles (
  id        INTEGER IDENTITY PRIMARY KEY,
  username  VARCHAR(20) NOT NULL,
  role      VARCHAR(20) NOT NULL
);

ALTER TABLE roles ADD CONSTRAINT fk_username 
  FOREIGN KEY (username) REFERENCES users (username);

CREATE INDEX fk_username_idx ON roles (username);
```

| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| id | INTEGER | PK, AUTO | Unique role record ID |
| username | VARCHAR(20) | FK → users.username | User reference |
| role | VARCHAR(20) | NOT NULL | Role name (e.g., ROLE_ADMIN) |

#### Sample Data

```sql
-- Location: src/main/resources/db/hsqldb/populateDB.sql

INSERT INTO users(username, password, enabled) 
  VALUES ('admin', '{noop}admin', true);

INSERT INTO roles (username, role) VALUES ('admin', 'ROLE_OWNER_ADMIN');
INSERT INTO roles (username, role) VALUES ('admin', 'ROLE_VET_ADMIN');
INSERT INTO roles (username, role) VALUES ('admin', 'ROLE_ADMIN');
```

#### Entity Relationship

```
┌──────────────┐         ┌──────────────┐
│    users     │         │    roles     │
├──────────────┤         ├──────────────┤
│ username (PK)│◄────────│ username (FK)│
│ password     │    1:N  │ id (PK)      │
│ enabled      │         │ role         │
└──────────────┘         └──────────────┘
```

---

### Domain Model Layer

#### User Entity

```java
// Location: src/main/java/org/springframework/samples/petclinic/model/User.java

@Entity
@Table(name = "users")
public class User {
    @Id
    @Column(name = "username")
    private String username;

    @Column(name = "password")
    private String password;

    @Column(name = "enabled")
    private Boolean enabled;

    @OneToMany(cascade = CascadeType.ALL, mappedBy = "user", fetch = FetchType.EAGER)
    private Set<Role> roles;
    
    // Getters and setters
}
```

#### Role Entity

```java
// Location: src/main/java/org/springframework/samples/petclinic/model/Role.java

@Entity
@Table(name = "roles", uniqueConstraints = @UniqueConstraint(
    columnNames = {"username", "role"}))
public class Role {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne
    @JoinColumn(name = "username")
    private User user;

    @Column(name = "role")
    private String name;
    
    // Getters and setters
}
```

---

### Repository Layer

#### UserRepository Interface

```java
// Location: src/main/java/org/springframework/samples/petclinic/repository/UserRepository.java

public interface UserRepository {
    void save(User user) throws DataAccessException;
}
```

#### Spring Data JPA Implementation

```java
// Location: src/main/java/org/springframework/samples/petclinic/repository/springdatajpa/SpringDataUserRepository.java

@Profile("spring-data-jpa")
public interface SpringDataUserRepository extends UserRepository, Repository<User, Integer> {
}
```

---

### Service Layer

#### UserService Interface

```java
// Location: src/main/java/org/springframework/samples/petclinic/service/UserService.java

public interface UserService {
    void saveUser(User user) throws DataAccessException;
}
```

#### UserServiceImpl

```java
// Location: src/main/java/org/springframework/samples/petclinic/service/UserServiceImpl.java

@Service
public class UserServiceImpl implements UserService {

    @Autowired
    private UserRepository userRepository;

    @Override
    @Transactional
    public void saveUser(User user) {
        // Validates user has at least one role
        if(user.getRoles() == null || user.getRoles().isEmpty()) {
            throw new IllegalArgumentException("User must have at least a role set!");
        }

        // Auto-prefix roles with "ROLE_" if not present
        for (Role role : user.getRoles()) {
            if(!role.getName().startsWith("ROLE_")) {
                role.setName("ROLE_" + role.getName());
            }
            if(role.getUser() == null) {
                role.setUser(user);
            }
        }

        userRepository.save(user);
    }
}
```

**Service Layer Responsibilities:**
- Validate user has at least one role
- Auto-prefix role names with "ROLE_"
- Link roles back to user entity
- Persist user with cascade to roles

---

### Security Configuration Layer

#### Roles Definition

```java
// Location: src/main/java/org/springframework/samples/petclinic/security/Roles.java

@Component
public class Roles {
    public final String OWNER_ADMIN = "ROLE_OWNER_ADMIN";
    public final String VET_ADMIN = "ROLE_VET_ADMIN";
    public final String ADMIN = "ROLE_ADMIN";
}
```

| Role | Constant | Purpose |
|------|----------|---------|
| ROLE_OWNER_ADMIN | `@roles.OWNER_ADMIN` | Manage owners, pets, visits |
| ROLE_VET_ADMIN | `@roles.VET_ADMIN` | Manage veterinarians, specialties |
| ROLE_ADMIN | `@roles.ADMIN` | Create users, full admin access |

#### Security Disabled Configuration (Default)

```java
// Location: src/main/java/org/springframework/samples/petclinic/security/DisableSecurityConfig.java

@Configuration
@ConditionalOnProperty(name = "petclinic.security.enable", havingValue = "false")
public class DisableSecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests((authz) -> authz
               .anyRequest().permitAll()
            )
            .csrf().disable();
        return http.build();
    }
}
```

#### Security Enabled Configuration

```java
// Location: src/main/java/org/springframework/samples/petclinic/security/BasicAuthenticationConfig.java

@Configuration
@EnableGlobalMethodSecurity(prePostEnabled = true)
@ConditionalOnProperty(name = "petclinic.security.enable", havingValue = "true")
public class BasicAuthenticationConfig {

    @Autowired
    private DataSource dataSource;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests((authz) -> authz
                .anyRequest().authenticated()
            )
            .httpBasic()
            .and()
            .csrf().disable();
        return http.build();
    }

    @Autowired
    public void configureGlobal(AuthenticationManagerBuilder auth) throws Exception {
        auth
            .jdbcAuthentication()
            .dataSource(dataSource)
            .usersByUsernameQuery("select username,password,enabled from users where username=?")
            .authoritiesByUsernameQuery("select username,role from roles where username=?");
    }
}
```

**Key Features:**
- JDBC-based authentication (queries database directly)
- HTTP Basic Authentication
- Method-level security enabled via `@EnableGlobalMethodSecurity`
- CSRF disabled for REST API usage

#### CORS Configuration

```java
// Location: src/main/java/org/springframework/samples/petclinic/security/WebSecurityConfig.java

@Configuration
@EnableWebSecurity
public class WebSecurityConfig {

    @Bean
    public SecurityFilterChain apiFilterChain(HttpSecurity http) throws Exception {
        http.authorizeHttpRequests(authz -> authz.anyRequest().permitAll())
            .csrf(csrf -> csrf.disable())
            .cors(cors -> cors.configurationSource(apiConfigurationSource()));
        return http.build();
    }

    private CorsConfigurationSource apiConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(List.of("http://localhost:4444"));
        configuration.setAllowedMethods(List.of("OPTIONS", "GET", "POST", "PUT"));
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
```

---

### API Endpoints

#### POST /api/users - Create User

```java
// Location: src/main/java/org/springframework/samples/petclinic/rest/controller/UserRestController.java

@RestController
@CrossOrigin(exposedHeaders = "errors, content-type")
@RequestMapping("api")
public class UserRestController implements UsersApi {

    private final UserService userService;
    private final UserMapper userMapper;

    @PreAuthorize("hasRole(@roles.ADMIN)")
    @Override
    public ResponseEntity<UserDto> addUser(UserDto userDto) {
        HttpHeaders headers = new HttpHeaders();
        User user = userMapper.toUser(userDto);
        this.userService.saveUser(user);
        return new ResponseEntity<>(userMapper.toUserDto(user), headers, HttpStatus.CREATED);
    }
}
```

**Request Body:**
```json
{
  "username": "newuser",
  "password": "password123",
  "enabled": true,
  "roles": [
    { "name": "OWNER_ADMIN" }
  ]
}
```

**Responses:**
| Status | Description |
|--------|-------------|
| 201 Created | User successfully created |
| 400 Bad Request | Validation error (e.g., no roles) |
| 401 Unauthorized | Not authenticated (when security enabled) |
| 403 Forbidden | User lacks ROLE_ADMIN |

#### Protected Endpoints by Role

| Endpoint | Required Role | Controller |
|----------|---------------|------------|
| `/api/owners/**` | ROLE_OWNER_ADMIN | OwnerRestController |
| `/api/pets/**` | ROLE_OWNER_ADMIN | PetRestController |
| `/api/visits/**` | ROLE_OWNER_ADMIN | VisitRestController |
| `/api/vets/**` | ROLE_VET_ADMIN | VetRestController |
| `/api/specialties/**` | ROLE_VET_ADMIN | SpecialtyRestController |
| `/api/pettypes/**` | ROLE_VET_ADMIN | PetTypeRestController |
| `/api/users` | ROLE_ADMIN | UserRestController |

---

### Mapper Layer

#### UserMapper

```java
// Location: src/main/java/org/springframework/samples/petclinic/mapper/UserMapper.java

@Mapper
public interface UserMapper {
    User toUser(UserDto userDto);
    UserDto toUserDto(User user);
}
```

---

### Frontend Implementation

#### Current State: NO AUTHENTICATION UI

The React frontend has **no authentication components**:

```
client/src/components/
├── App.tsx              # No auth state
├── Menu.tsx             # No login/logout buttons
├── WelcomePage.tsx      # No auth check
├── owners/              # Direct access
├── pets/                # Direct access
├── vets/                # Direct access
└── visits/              # Direct access
```

**Menu.tsx - No Auth Links:**
```tsx
// Location: client/src/components/Menu.tsx

<ul className='nav navbar-nav navbar-right'>
  <MenuItem url='/' title='home page'>Home</MenuItem>
  <MenuItem url='/owners/list' title='find owners'>Find owners</MenuItem>
  <MenuItem url='/vets' title='veterinarians'>Veterinarians</MenuItem>
  <MenuItem url='/error' title='error'>Error</MenuItem>
  // ❌ No Login button
  // ❌ No Logout button
  // ❌ No User profile
</ul>
```

**Routes - No Protection:**
```tsx
// Location: client/src/configureRoutes.tsx

<Route component={App}>
  <Route path='/' component={WelcomePage} />
  <Route path='/owners/list' component={FindOwnersPage} />
  <Route path='/owners/:ownerId' component={OwnersPage} />
  <Route path='/vets' component={VetsPage} />
  // ❌ No protected routes
  // ❌ No auth redirects
</Route>
```

---

### Application Configuration

```properties
# Location: src/main/resources/application.properties

# Security toggle (default: disabled)
petclinic.security.enable=false
```

| Setting | Value | Effect |
|---------|-------|--------|
| `petclinic.security.enable=false` | Default | All APIs public, no auth required |
| `petclinic.security.enable=true` | Optional | HTTP Basic Auth, role checks active |

---

## Implementation Phases

### Phase 1: Database Layer - ✅ BASELINE COMPLETE

**Objective**: User and role data storage

**Existing Components**:
1. ✅ `users` table with username, password, enabled
2. ✅ `roles` table with user-role mapping
3. ✅ Foreign key relationship established
4. ✅ Default admin user seeded

**Files**:
- `src/main/resources/db/hsqldb/initDB.sql`
- `src/main/resources/db/hsqldb/populateDB.sql`

---

### Phase 2: Domain Model Layer - ✅ BASELINE COMPLETE

**Objective**: JPA entities for users and roles

**Existing Components**:
1. ✅ User entity with JPA annotations
2. ✅ Role entity with JPA annotations
3. ✅ OneToMany/ManyToOne relationship

**Files**:
- `src/main/java/.../model/User.java`
- `src/main/java/.../model/Role.java`

---

### Phase 3: Repository Layer - ✅ BASELINE COMPLETE

**Objective**: Data access for users

**Existing Components**:
1. ✅ UserRepository interface
2. ✅ SpringDataUserRepository implementation

**Files**:
- `src/main/java/.../repository/UserRepository.java`
- `src/main/java/.../repository/springdatajpa/SpringDataUserRepository.java`

---

### Phase 4: Service Layer - ✅ BASELINE COMPLETE

**Objective**: User management business logic

**Existing Components**:
1. ✅ UserService interface
2. ✅ UserServiceImpl with role validation

**Files**:
- `src/main/java/.../service/UserService.java`
- `src/main/java/.../service/UserServiceImpl.java`

---

### Phase 5: Security Configuration - ✅ BASELINE COMPLETE

**Objective**: Spring Security setup

**Existing Components**:
1. ✅ Roles component with constants
2. ✅ DisableSecurityConfig (default)
3. ✅ BasicAuthenticationConfig (optional)
4. ✅ WebSecurityConfig with CORS

**Files**:
- `src/main/java/.../security/Roles.java`
- `src/main/java/.../security/DisableSecurityConfig.java`
- `src/main/java/.../security/BasicAuthenticationConfig.java`
- `src/main/java/.../security/WebSecurityConfig.java`

---

### Phase 6: API Layer - ✅ BASELINE COMPLETE

**Objective**: User management API

**Existing Components**:
1. ✅ UserRestController with addUser endpoint
2. ✅ UserMapper for DTO conversion
3. ✅ @PreAuthorize on all controllers

**Files**:
- `src/main/java/.../rest/controller/UserRestController.java`
- `src/main/java/.../mapper/UserMapper.java`

---

### Phase 7: Frontend Implementation - ❌ NOT IMPLEMENTED

**Objective**: Login/Registration UI

**Missing Components**:
1. ❌ Login page component
2. ❌ Registration page component
3. ❌ Auth context/state management
4. ❌ Protected route wrapper
5. ❌ Login/Logout buttons in menu
6. ❌ Token/session storage

**Files to Create**:
- `client/src/components/auth/LoginPage.tsx`
- `client/src/components/auth/RegisterPage.tsx`
- `client/src/contexts/AuthContext.tsx`
- `client/src/components/auth/ProtectedRoute.tsx`

---

## Success Criteria

### Backend (Baseline - Complete)
- [x] Users table exists with proper schema
- [x] Roles table exists with FK to users
- [x] User entity maps to database
- [x] Role entity maps to database
- [x] UserRepository can save users
- [x] UserService validates and saves users
- [x] Security can be enabled via config
- [x] HTTP Basic Auth works when enabled
- [x] Role-based access control on endpoints
- [x] Default admin user available

### Frontend (Not Implemented)
- [ ] Login page renders
- [ ] User can enter credentials
- [ ] Auth state persists across pages
- [ ] Protected routes redirect to login
- [ ] Logout clears auth state
- [ ] Registration form works
- [ ] Error messages display

---

## Database Migration

> **📖 Reference**: See [ARCHITECTURE.md - Database Migration Strategy](../ARCHITECTURE.md#database-migration-strategy) for the complete migration documentation applicable to all domains.

**Key Points for Authentication:**
- `users` and `roles` tables are security-sensitive
- Password migration requires special handling (encryption changes)
- Role changes may affect API access across the application

---

## Dependencies

### Backend Dependencies
- Spring Boot Starter Security
- Spring Security Web
- Spring Security Config
- Spring Data JPA

### Frontend Dependencies (Current)
- No authentication-related dependencies

### Frontend Dependencies (Needed for Full Implementation)
- Auth state management library (or React Context)
- HTTP client with auth header support
- Secure token storage solution

---

## Risks and Mitigation

### Security Risks
| Risk | Impact | Current State | Mitigation Needed |
|------|--------|---------------|-------------------|
| Plain text passwords | Critical | `{noop}` prefix used | Implement BCrypt encoding |
| No HTTPS | High | HTTP only | Configure TLS/SSL |
| CSRF disabled | Medium | Disabled for API | Re-evaluate for web forms |
| No token expiry | Medium | N/A (Basic Auth) | Implement JWT with expiry |

### Implementation Risks
| Risk | Impact | Mitigation |
|------|--------|------------|
| Breaking changes to API | High | Version API endpoints |
| Session management complexity | Medium | Use established patterns (JWT) |
| Frontend auth state bugs | Medium | Comprehensive testing |

---

## Future Enhancements

1. **JWT Token Authentication** - Replace HTTP Basic with JWT
2. **Password Encryption** - BCrypt password encoding
3. **OAuth2/SSO Integration** - Social login support
4. **Two-Factor Authentication** - Enhanced security
5. **Password Reset Flow** - Self-service password recovery
6. **Account Lockout** - Brute force protection
7. **Audit Logging** - Track authentication events
8. **Remember Me** - Persistent sessions

---

## Current Status

**Last Updated**: 2026-01-16
**Current Phase**: Baseline Documentation
**Status**: ✅ BASELINE COMPLETE
**Next Steps**: Implement frontend authentication UI (if required)

---

## Notes for AI Agents

When updating this PRD:
1. Update phase status markers as work progresses
2. Add implementation details under each phase as code is written
3. Mark success criteria as complete when features work
4. Add troubleshooting entries when bugs are found and fixed
5. Update "Current Status" section at the top after changes
6. Use code references format: `filepath:line-number` when citing code
7. Maintain metadata version and date on updates

