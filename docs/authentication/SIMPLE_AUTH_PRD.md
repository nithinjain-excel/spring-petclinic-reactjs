# Simple Login & Logout - Technical PRD

| Version | Date | Author | Status |
|---------|------|--------|--------|
| 1.0 | 2026-02-03 | AI Assistant | Draft |
| 1.1 | 2026-02-03 | AI Assistant | ✅ **COMPLETED** |

---

## Overview

This document outlines the requirements for implementing simple login and logout functionality in the Spring PetClinic application. The system will provide Basic Authentication with username/password validation against the database, storing credentials in browser localStorage for API authorization. This is a first-phase implementation focusing on functionality over security best practices.

> **⚠️ Security Note**: This implementation stores credentials in localStorage, which is not recommended for production. A future enhancement will implement a more secure approach (JWT tokens, secure cookies, etc.).

---

## Business Requirements

### User Authentication
- Users can log in with username and password
- Users can log out from the application
- Login state persists across page refreshes (via localStorage)
- Invalid credentials display clear error messages

### API Security
- All API endpoints require authentication when security is enabled
- Frontend sends Base64-encoded credentials in Authorization header
- Backend validates credentials using Spring Security Basic Authentication

### User Experience
- Login button visible in navigation menu when logged out
- Logout button visible in navigation menu when logged in
- Successful login redirects to home page
- Failed login shows error message without page redirect

---

## Technical Requirements

### Database Schema

**No schema changes required** - Using existing `users` and `roles` tables.

```sql
-- Existing tables (from AUTHENTICATION_BASELINE_PRD.md)
-- Location: src/main/resources/db/hsqldb/initDB.sql

CREATE TABLE users (
  username    VARCHAR(20) NOT NULL,
  password    VARCHAR(20) NOT NULL,
  enabled     BOOLEAN DEFAULT TRUE NOT NULL,
  PRIMARY KEY (username)
);

CREATE TABLE roles (
  id        INTEGER IDENTITY PRIMARY KEY,
  username  VARCHAR(20) NOT NULL,
  role      VARCHAR(20) NOT NULL
);
```

### Repository Layer Changes

#### UserRepository Interface - Add findByUsername

```java
// Location: src/main/java/org/springframework/samples/petclinic/repository/UserRepository.java

public interface UserRepository {
    void save(User user) throws DataAccessException;
    User findByUsername(String username) throws DataAccessException;  // NEW
}
```

#### SpringDataUserRepository - Add Query

```java
// Location: src/main/java/org/springframework/samples/petclinic/repository/springdatajpa/SpringDataUserRepository.java

@Profile("spring-data-jpa")
public interface SpringDataUserRepository extends UserRepository, Repository<User, String> {
    
    @Override
    @Query("SELECT u FROM User u LEFT JOIN FETCH u.roles WHERE u.username = :username")
    User findByUsername(@Param("username") String username);
}
```

### Service Layer Changes

#### UserService Interface - Add Authentication Method

```java
// Location: src/main/java/org/springframework/samples/petclinic/service/UserService.java

public interface UserService {
    void saveUser(User user) throws DataAccessException;
    User findByUsername(String username) throws DataAccessException;  // NEW
    boolean validateCredentials(String username, String password);     // NEW
}
```

#### UserServiceImpl - Implement Authentication

```java
// Location: src/main/java/org/springframework/samples/petclinic/service/UserServiceImpl.java

@Override
@Transactional(readOnly = true)
public User findByUsername(String username) {
    return userRepository.findByUsername(username);
}

@Override
@Transactional(readOnly = true)
public boolean validateCredentials(String username, String password) {
    User user = userRepository.findByUsername(username);
    if (user == null || !user.getEnabled()) {
        return false;
    }
    // Password stored as {noop}password - strip prefix for comparison
    String storedPassword = user.getPassword();
    if (storedPassword.startsWith("{noop}")) {
        storedPassword = storedPassword.substring(6);
    }
    return storedPassword.equals(password);
}
```

### API Endpoints

#### POST /api/auth/login - User Login

**Request Body:**
```json
{
  "username": "admin",
  "password": "admin"
}
```

**Response - Success (200):**
```json
{
  "username": "admin",
  "roles": ["ROLE_OWNER_ADMIN", "ROLE_VET_ADMIN", "ROLE_ADMIN"],
  "message": "Login successful"
}
```

**Response - Error (401):**
```json
{
  "message": "Invalid username or password"
}
```

**Response - Error (400):**
```json
{
  "message": "Username and password are required"
}
```

#### AuthRestController Implementation

```java
// Location: src/main/java/org/springframework/samples/petclinic/rest/controller/AuthRestController.java

@RestController
@CrossOrigin(exposedHeaders = "errors, content-type")
@RequestMapping("api/auth")
public class AuthRestController {

    private final UserService userService;

    public AuthRestController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest loginRequest) {
        if (loginRequest.getUsername() == null || loginRequest.getPassword() == null) {
            return ResponseEntity.badRequest()
                .body(Map.of("message", "Username and password are required"));
        }

        boolean valid = userService.validateCredentials(
            loginRequest.getUsername(), 
            loginRequest.getPassword()
        );

        if (valid) {
            User user = userService.findByUsername(loginRequest.getUsername());
            List<String> roles = user.getRoles().stream()
                .map(Role::getName)
                .collect(Collectors.toList());
            
            return ResponseEntity.ok(Map.of(
                "username", user.getUsername(),
                "roles", roles,
                "message", "Login successful"
            ));
        } else {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(Map.of("message", "Invalid username or password"));
        }
    }
}
```

#### LoginRequest DTO

```java
// Location: src/main/java/org/springframework/samples/petclinic/rest/dto/LoginRequest.java

public class LoginRequest {
    private String username;
    private String password;

    // Getters and Setters
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
}
```

### Security Configuration Changes

#### Update BasicAuthenticationConfig

```java
// Location: src/main/java/org/springframework/samples/petclinic/security/BasicAuthenticationConfig.java

@Bean
public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
    http
        .authorizeHttpRequests((authz) -> authz
            .requestMatchers("/api/auth/login").permitAll()  // Allow login without auth
            .anyRequest().authenticated()
        )
        .httpBasic()
        .and()
        .csrf().disable();
    return http.build();
}
```

#### Enable Security in application.properties

```properties
# Location: src/main/resources/application.properties

petclinic.security.enable=true
```

### Frontend Implementation

#### Login Page Component

```tsx
// Location: client/src/components/auth/LoginPage.tsx

import * as React from 'react';
import { browserHistory } from 'react-router';

interface LoginState {
  username: string;
  password: string;
  error: string;
  loading: boolean;
}

export default class LoginPage extends React.Component<{}, LoginState> {
  constructor(props) {
    super(props);
    this.state = {
      username: '',
      password: '',
      error: '',
      loading: false
    };
  }

  handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    this.setState({ loading: true, error: '' });

    try {
      const response = await fetch(`${__API_SERVER_URL__}/api/auth/login`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          username: this.state.username,
          password: this.state.password
        })
      });

      if (response.ok) {
        const data = await response.json();
        localStorage.setItem('auth_username', this.state.username);
        localStorage.setItem('auth_password', this.state.password);
        localStorage.setItem('auth_roles', JSON.stringify(data.roles));
        browserHistory.push('/');
      } else {
        const error = await response.json();
        this.setState({ error: error.message || 'Login failed' });
      }
    } catch (err) {
      this.setState({ error: 'Network error. Please try again.' });
    } finally {
      this.setState({ loading: false });
    }
  };

  render() {
    return (
      <div className="container">
        <h2>Login</h2>
        {this.state.error && (
          <div className="alert alert-danger">{this.state.error}</div>
        )}
        <form onSubmit={this.handleSubmit}>
          <div className="form-group">
            <label>Username</label>
            <input
              type="text"
              className="form-control"
              value={this.state.username}
              onChange={(e) => this.setState({ username: e.target.value })}
              required
            />
          </div>
          <div className="form-group">
            <label>Password</label>
            <input
              type="password"
              className="form-control"
              value={this.state.password}
              onChange={(e) => this.setState({ password: e.target.value })}
              required
            />
          </div>
          <button 
            type="submit" 
            className="btn btn-primary"
            disabled={this.state.loading}
          >
            {this.state.loading ? 'Logging in...' : 'Login'}
          </button>
        </form>
      </div>
    );
  }
}
```

#### Auth Utility Functions

```tsx
// Location: client/src/util/auth.ts

export const isLoggedIn = (): boolean => {
  return localStorage.getItem('auth_username') !== null;
};

export const getAuthHeader = (): string | null => {
  const username = localStorage.getItem('auth_username');
  const password = localStorage.getItem('auth_password');
  
  if (username && password) {
    const credentials = btoa(`${username}:${password}`);
    return `Basic ${credentials}`;
  }
  return null;
};

export const logout = (): void => {
  localStorage.removeItem('auth_username');
  localStorage.removeItem('auth_password');
  localStorage.removeItem('auth_roles');
};

export const getUsername = (): string | null => {
  return localStorage.getItem('auth_username');
};
```

#### Updated Menu Component

```tsx
// Location: client/src/components/Menu.tsx (modifications)

// Add login/logout button to navbar-right
{isLoggedIn() ? (
  <li>
    <a onClick={this.handleLogout} style={{ cursor: 'pointer' }}>
      Logout ({getUsername()})
    </a>
  </li>
) : (
  <MenuItem url='/login' title='login'>Login</MenuItem>
)}
```

#### API Fetch with Auth Header

```tsx
// Location: client/src/util/fetchWithAuth.ts

import { getAuthHeader } from './auth';

export const fetchWithAuth = async (url: string, options: RequestInit = {}) => {
  const authHeader = getAuthHeader();
  
  const headers = new Headers(options.headers || {});
  if (authHeader) {
    headers.set('Authorization', authHeader);
  }
  
  return fetch(url, {
    ...options,
    headers
  });
};
```

---

## Implementation Phases

### Phase 1: Repository Layer - Add findByUsername ✅ COMPLETED

**Objective**: Add method to retrieve user by username from database

**TDD Steps**:
| Step | Action | Result |
|------|--------|--------|
| 1.1 | Write unit test for `findByUsername` | ✅ Done |
| 1.2 | Run `.\mvnw.cmd test -Dtest=AbstractUserRepositoryTests` | ❌ RED |
| 1.3 | Add `findByUsername` to `UserRepository` interface | ✅ Done |
| 1.4 | Implement in `SpringDataUserRepository`, `JpaUserRepositoryImpl`, `JdbcUserRepositoryImpl` | ✅ Done |
| 1.5 | Run `.\mvnw.cmd test -Dtest=UserRepositorySpringDataJpaTests` | ✅ GREEN |

**Unit Tests Created**:
```java
// AbstractUserRepositoryTests.java
@Test void shouldFindUserByUsername()           // ✅ PASSED
@Test void shouldReturnNullForNonExistentUsername()  // ✅ PASSED
```

**Deliverables**:
- ✅ `UserRepository.java` - Added `findByUsername` method
- ✅ `SpringDataUserRepository.java` - Added query implementation
- ✅ `JpaUserRepositoryImpl.java` - Added implementation
- ✅ `JdbcUserRepositoryImpl.java` - Added implementation
- ✅ `AbstractUserRepositoryTests.java` - Unit tests
- ✅ `UserRepositorySpringDataJpaTests.java` - Test runner

**Completion Date**: 2026-02-03

---

### Phase 2: Service Layer - Add Credential Validation ✅ COMPLETED

**Objective**: Add service methods for user lookup and password validation

**TDD Steps**:
| Step | Action | Result |
|------|--------|--------|
| 2.1 | Write unit tests for credential validation | ✅ Done |
| 2.2 | Run `.\mvnw.cmd test -Dtest=AbstractUserServiceTests` | ❌ RED |
| 2.3 | Add methods to `UserService` interface | ✅ Done |
| 2.4 | Implement in `UserServiceImpl` | ✅ Done |
| 2.5 | Run `.\mvnw.cmd test -Dtest=AbstractUserServiceTests` | ✅ GREEN |

**Unit Tests Created**:
```java
// AbstractUserServiceTests.java
@Test void shouldFindUserByUsername()              // ✅ PASSED
@Test void shouldReturnTrueForValidCredentials()   // ✅ PASSED
@Test void shouldReturnFalseForInvalidUsername()   // ✅ PASSED
@Test void shouldReturnFalseForInvalidPassword()   // ✅ PASSED
```

**Deliverables**:
- ✅ `UserService.java` - Added `findByUsername` and `validateCredentials` methods
- ✅ `UserServiceImpl.java` - Implemented validation logic with `{noop}` prefix handling
- ✅ `AbstractUserServiceTests.java` - Unit tests

**Completion Date**: 2026-02-03

---

### Phase 3: API Layer - Create Login Endpoint ✅ COMPLETED

**Objective**: Create POST /api/auth/login endpoint

**TDD Steps**:
| Step | Action | Result |
|------|--------|--------|
| 3.1 | Write unit tests for login endpoint | ✅ Done |
| 3.2 | Run `.\mvnw.cmd test -Dtest=AuthRestControllerTests` | ❌ RED |
| 3.3 | Create `LoginRequest` DTO | ✅ Done |
| 3.4 | Create `AuthRestController` | ✅ Done |
| 3.5 | Run `.\mvnw.cmd test -Dtest=AuthRestControllerTests` | ✅ GREEN |

**Unit Tests Created**:
```java
// AuthRestControllerTests.java
@Test void testLoginSuccess()                    // ✅ PASSED
@Test void testLoginFailureInvalidUsername()     // ✅ PASSED
@Test void testLoginFailureInvalidPassword()     // ✅ PASSED
@Test void testLoginBadRequestMissingCredentials() // ✅ PASSED
```

**Deliverables**:
- ✅ `LoginRequest.java` - Request DTO with username/password fields
- ✅ `AuthRestController.java` - Login endpoint at `/api/auth/login`
- ✅ `AuthRestControllerTests.java` - 4 unit tests

**Manual Testing Results**:
```bash
# Login with valid credentials
curl -X POST http://localhost:9966/petclinic/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"admin"}'
# Response: {"username":"admin","roles":["ROLE_ADMIN","ROLE_OWNER_ADMIN","ROLE_VET_ADMIN"],"message":"Login successful"}
```

**Completion Date**: 2026-02-03

---

### Phase 4: Frontend - Login Page ✅ COMPLETED

**Objective**: Create login form UI in React frontend

**Tasks**:
| Step | Action | Status |
|------|--------|--------|
| 4.1 | Create `LoginPage.tsx` component | ✅ Done |
| 4.2 | Add login route to `configureRoutes.tsx` | ✅ Done |
| 4.3 | Create `auth.ts` utility functions | ✅ Done |
| 4.4 | Test login form submission | ✅ Done |
| 4.5 | Verify credentials stored in localStorage | ✅ Done |

**Deliverables**:
- ✅ `client/src/components/auth/LoginPage.tsx` - Login form with username/password fields
- ✅ `client/src/util/auth.ts` - Auth utilities (login, logout, isLoggedIn, getAuthHeader, etc.)
- ✅ `client/src/configureRoutes.tsx` - Added `/login` route

**Features Implemented**:
- Username and password input fields
- Form validation (empty fields)
- Error message display
- Redirect to home on successful login
- Credentials stored in localStorage

**Completion Date**: 2026-02-03

---

### Phase 5: Frontend - Menu Login/Logout Button ✅ COMPLETED

**Objective**: Add Login/Logout button to navigation menu

**Tasks**:
| Step | Action | Status |
|------|--------|--------|
| 5.1 | Update `Menu.tsx` with login/logout logic | ✅ Done |
| 5.2 | Show "Login" when logged out | ✅ Done |
| 5.3 | Show "Logout (username)" when logged in | ✅ Done |
| 5.4 | Implement logout handler to clear localStorage | ✅ Done |
| 5.5 | Test login/logout button state changes | ✅ Done |

**Deliverables**:
- ✅ `client/src/components/Menu.tsx` - Updated with conditional auth buttons

**Features Implemented**:
- Login button (glyphicon-log-in) when logged out
- Logout button (glyphicon-log-out) showing username when logged in
- Click logout clears localStorage and redirects to home
- State persists across page refreshes

**Completion Date**: 2026-02-03

---

### Phase 6: Frontend - API Authorization Headers ✅ COMPLETED

**Objective**: Add Basic Auth header to all API calls

**Tasks**:
| Step | Action | Status |
|------|--------|--------|
| 6.1 | Create `fetchWithAuth` utility in `util/index.tsx` | ✅ Done |
| 6.2 | Update all API calls to use `fetchWithAuth` | ✅ Done |
| 6.3 | Test API calls include Authorization header | ✅ Done |

**Deliverables**:
- ✅ `client/src/util/index.tsx` - Added `fetchWithAuth` and `createHeaders` functions
- ✅ Updated `submitForm` to include auth headers

**Files Updated to Use `fetchWithAuth`**:
| File | Status |
|------|--------|
| `components/owners/OwnersPage.tsx` | ✅ |
| `components/owners/FindOwnersPage.tsx` | ✅ |
| `components/owners/EditOwnerPage.tsx` | ✅ |
| `components/vets/VetsPage.tsx` | ✅ |
| `components/pets/EditPetPage.tsx` | ✅ |
| `components/pets/createPetEditorModel.ts` | ✅ |
| `components/visits/VisitsPage.tsx` | ✅ |
| `components/ErrorPage.tsx` | ✅ |

**Authorization Header Format**:
```
Authorization: Basic YWRtaW46YWRtaW4=
```
(Base64 encoded `admin:admin`)

**Completion Date**: 2026-02-03

---

### Phase 7: Backend - Enable Spring Security ✅ COMPLETED

**Objective**: Enable and configure Spring Security for Basic Auth

**Tasks**:
| Step | Action | Status |
|------|--------|--------|
| 7.1 | Update `BasicAuthenticationConfig.java` to allow login endpoint | ✅ Done |
| 7.2 | Set `petclinic.security.enable=true` in properties | ✅ Done |
| 7.3 | Test protected endpoints require authentication | ✅ Done |
| 7.4 | Test login endpoint is accessible without auth | ✅ Done |

**Deliverables**:
- ✅ `BasicAuthenticationConfig.java` - Updated with public/protected endpoint rules and CORS
- ✅ `application.properties` - Changed `petclinic.security.enable=true`

**Security Configuration**:
```java
// Public endpoints (no auth required)
.requestMatchers("/api/auth/**").permitAll()
.requestMatchers("/swagger-ui/**", "/v3/api-docs/**", "/swagger-resources/**").permitAll()

// Protected endpoints (authentication required)
.requestMatchers("/api/**").authenticated()
```

**Security Test Results**:
| Test | Endpoint | Auth | Expected | Result |
|------|----------|------|----------|--------|
| Test 1 | `GET /api/vets` | ❌ None | 401 | ✅ PASS |
| Test 2 | `GET /api/vets` | ✅ Basic | 200 | ✅ PASS |
| Test 3 | `POST /api/auth/login` | ❌ None | 200 | ✅ PASS |

**Completion Date**: 2026-02-03

---

### Phase 8: Documentation ✅ COMPLETED

**Objective**: Update documentation

**Tasks**:
| Step | Action | Status |
|------|--------|--------|
| 8.1 | Update `SIMPLE_AUTH_PRD.md` with implementation details | ✅ Done |
| 8.2 | Update all phase statuses to COMPLETED | ✅ Done |
| 8.3 | Document test results and success criteria | ✅ Done |

**Deliverables**:
- ✅ `SIMPLE_AUTH_PRD.md` - Updated with all phase completion status

**Completion Date**: 2026-02-03

---

## 🎉 Feature Complete

---

## Success Criteria

### Backend ✅ All Passed
- [x] `findByUsername` returns user with roles from database
- [x] `validateCredentials` returns true for valid credentials
- [x] `validateCredentials` returns false for invalid username
- [x] `validateCredentials` returns false for invalid password
- [x] POST /api/auth/login returns 200 with user data for valid credentials
- [x] POST /api/auth/login returns 401 for invalid credentials
- [x] POST /api/auth/login returns 400 for missing credentials
- [x] Login endpoint accessible without authentication
- [x] All other endpoints require authentication when security enabled

### Frontend ✅ All Passed
- [x] Login page renders with username/password fields
- [x] Login button visible in menu when logged out
- [x] Successful login stores credentials in localStorage
- [x] Successful login redirects to home page
- [x] Failed login shows error message
- [x] Logout button visible in menu when logged in
- [x] Logout clears localStorage
- [x] API calls include Authorization header when logged in

---

## File Locations

### Backend Files to Create/Modify
| File | Action | Description |
|------|--------|-------------|
| `repository/UserRepository.java` | Modify | Add `findByUsername` method |
| `repository/springdatajpa/SpringDataUserRepository.java` | Modify | Add query |
| `service/UserService.java` | Modify | Add new methods |
| `service/UserServiceImpl.java` | Modify | Implement validation |
| `rest/controller/AuthRestController.java` | Create | Login endpoint |
| `rest/dto/LoginRequest.java` | Create | Request DTO |
| `security/BasicAuthenticationConfig.java` | Modify | Allow login endpoint |
| `application.properties` | Modify | Enable security |

### Frontend Files to Create/Modify
| File | Action | Description |
|------|--------|-------------|
| `components/auth/LoginPage.tsx` | Create | Login form component |
| `util/auth.ts` | Create | Auth utility functions |
| `util/fetchWithAuth.ts` | Create | Auth fetch wrapper |
| `components/Menu.tsx` | Modify | Login/Logout buttons |
| `configureRoutes.tsx` | Modify | Add login route |

### Test Files to Create
| File | Description |
|------|-------------|
| `test/.../rest/controller/AuthRestControllerTests.java` | Login endpoint tests |

---

## Risks and Mitigation

### Security Risks
| Risk | Impact | Mitigation |
|------|--------|------------|
| Credentials in localStorage | High | Documented as phase 1 limitation; future JWT implementation planned |
| Plain text password comparison | Medium | Use `{noop}` prefix handling; future BCrypt migration |
| No HTTPS | High | Configure TLS for production deployment |

### Technical Risks
| Risk | Impact | Mitigation |
|------|--------|------------|
| CORS issues with auth headers | Medium | Update CORS config to allow Authorization header |
| Browser caching credentials | Low | Use proper cache headers |

---

## Future Enhancements

1. **JWT Token Authentication** - Replace localStorage with JWT tokens
2. **Secure HTTP-only Cookies** - Store tokens in secure cookies
3. **Password Encryption** - BCrypt password encoding
4. **Session Timeout** - Auto-logout after inactivity
5. **Remember Me** - Persistent login option
6. **Password Reset** - Self-service password recovery

---

## Dependencies

### Backend Dependencies (Existing)
- Spring Boot Starter Security
- Spring Security Web
- Spring Security Config
- Spring Data JPA

### Frontend Dependencies (Existing)
- React 15.x
- React Router 2.x
- whatwg-fetch (for Fetch API)

---

## Current Status

**Last Updated**: 2026-02-03
**Current Phase**: All Phases Complete
**Status**: ✅ **COMPLETED**
**Implementation Date**: 2026-02-03

### Summary of Implementation
| Phase | Description | Status |
|-------|-------------|--------|
| Phase 1 | Repository Layer - findByUsername | ✅ Complete |
| Phase 2 | Service Layer - Credential Validation | ✅ Complete |
| Phase 3 | API Layer - Login Endpoint | ✅ Complete |
| Phase 4 | Frontend - Login Page | ✅ Complete |
| Phase 5 | Frontend - Menu Login/Logout | ✅ Complete |
| Phase 6 | Frontend - API Auth Headers | ✅ Complete |
| Phase 7 | Backend - Enable Security | ✅ Complete |
| Phase 8 | Documentation | ✅ Complete |

### Test Users
| Username | Password | Roles |
|----------|----------|-------|
| admin | admin | ROLE_ADMIN, ROLE_OWNER_ADMIN, ROLE_VET_ADMIN |

### How to Test
1. Start backend: `.\mvnw.cmd spring-boot:run`
2. Open browser: http://localhost:8080
3. Click "Login" in menu
4. Enter username: `admin`, password: `admin`
5. Verify "Logout (admin)" appears in menu
6. Navigate to pages - data should load with auth headers

---

## Notes for AI Agents

When implementing this PRD:
1. Follow TDD workflow: Write test → RED → Implement → GREEN → Manual test → WAIT
2. Update phase status markers as work progresses
3. Mark success criteria as complete when features work
4. Add troubleshooting entries when bugs are found and fixed
5. WAIT for user confirmation after each phase before proceeding
6. Use `.\mvnw.cmd test -Dtest=TestClassName` to run tests on Windows

