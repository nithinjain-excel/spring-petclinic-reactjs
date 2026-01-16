# Spring PetClinic Application Architecture

| Version | Date | Author | Status |
|---------|------|--------|--------|
| 1.1 | 2026-01-16 | AI Assistant | Draft |

**Changes:** v1.0 - Initial documentation | v1.1 - Updated authentication section

## Overview

The Spring PetClinic is a full-stack web application demonstrating a React frontend with a Spring Boot backend. It follows a layered architecture pattern with clear separation of concerns.

---

## Technology Stack

### Backend
| Component | Technology | Version |
|-----------|------------|---------|
| Framework | Spring Boot | 3.2.1 |
| Language | Java | 17+ |
| Build Tool | Maven | 3.x (wrapper included) |
| API Documentation | OpenAPI/Swagger | 3.0 |
| Object Mapping | MapStruct | 1.4.1 |

### Frontend
| Component | Technology | Version |
|-----------|------------|---------|
| Framework | React | 15.x |
| Language | TypeScript | 4.9.x |
| Build Tool | Webpack | 5.94.0 |
| Styling | Less/Bootstrap | 3.3.7 |
| Routing | React Router | 2.7.x |

### Database Support
| Database | Profile Name | Configuration |
|----------|-------------|---------------|
| HSQLDB (default) | `hsqldb` | In-memory, auto-populated |
| MySQL | `mysql` | Persistent |
| PostgreSQL | `postgresql` | Persistent |

---

## Database Layer

### Schema Design

The database schema consists of the following tables:

```
┌─────────────────────────────────────────────────────────────────────────┐
│                           DATABASE SCHEMA                               │
├─────────────────────────────────────────────────────────────────────────┤
│                                                                         │
│  ┌──────────┐       ┌──────────┐       ┌──────────┐                    │
│  │  owners  │───────│   pets   │───────│  visits  │                    │
│  └──────────┘       └──────────┘       └──────────┘                    │
│       │                  │                                              │
│       │                  │                                              │
│       │             ┌──────────┐                                       │
│       │             │  types   │                                       │
│       │             └──────────┘                                       │
│                                                                         │
│  ┌──────────┐       ┌────────────────┐       ┌──────────────┐          │
│  │   vets   │───────│vet_specialties│───────│ specialties  │          │
│  └──────────┘       └────────────────┘       └──────────────┘          │
│                                                                         │
│  ┌──────────┐       ┌──────────┐                                       │
│  │  users   │───────│  roles   │                                       │
│  └──────────┘       └──────────┘                                       │
└─────────────────────────────────────────────────────────────────────────┘
```

### Tables Description

| Table | Description | Key Relationships |
|-------|-------------|-------------------|
| `owners` | Pet owner information (name, address, phone) | One-to-Many with `pets` |
| `pets` | Pet information (name, birth_date) | Many-to-One with `owners`, `types` |
| `types` | Pet types (cat, dog, lizard, etc.) | One-to-Many with `pets` |
| `visits` | Veterinary visit records | Many-to-One with `pets` |
| `vets` | Veterinarian information | Many-to-Many with `specialties` |
| `specialties` | Vet specializations (surgery, radiology, etc.) | Many-to-Many with `vets` |
| `vet_specialties` | Junction table for vets-specialties | FK to `vets`, `specialties` |
| `users` | Authentication users | One-to-Many with `roles` |
| `roles` | User roles for authorization | Many-to-One with `users` |

### Migration Scripts

Located in `src/main/resources/db/`:

```
db/
├── hsqldb/
│   ├── initDB.sql      # Schema creation
│   └── populateDB.sql  # Sample data
├── mysql/
│   ├── initDB.sql
│   ├── populateDB.sql
│   └── petclinic_db_setup_mysql.txt
└── postgresql/
    ├── initDB.sql
    ├── populateDB.sql
    └── petclinic_db_setup_postgresql.txt
```

### Database Connection

The application supports three repository layer implementations, configured via Spring profiles:

| Profile | Implementation | Description |
|---------|---------------|-------------|
| `spring-data-jpa` (default) | Spring Data JPA | Auto-generated repository methods |
| `jpa` | JPA/Hibernate | Manual EntityManager usage |
| `jdbc` | Spring JDBC | Direct SQL queries |

**Default Configuration** (`application.properties`):
```properties
spring.profiles.active=hsqldb,spring-data-jpa
```

---

## Backend Architecture

### Layered Architecture

```
┌─────────────────────────────────────────────────────────────────────────┐
│                         REST Controllers                                │
│  (OwnerRestController, PetRestController, VetRestController, etc.)     │
├─────────────────────────────────────────────────────────────────────────┤
│                              Mappers                                    │
│  (OwnerMapper, PetMapper, VetMapper - MapStruct)                       │
├─────────────────────────────────────────────────────────────────────────┤
│                          Service Layer                                  │
│  (ClinicService, UserService)                                          │
├─────────────────────────────────────────────────────────────────────────┤
│                         Repository Layer                                │
│  (OwnerRepository, PetRepository, VetRepository, etc.)                 │
├─────────────────────────────────────────────────────────────────────────┤
│                          Domain Models                                  │
│  (Owner, Pet, Vet, Visit, Specialty, PetType)                          │
├─────────────────────────────────────────────────────────────────────────┤
│                            Database                                     │
│  (HSQLDB / MySQL / PostgreSQL)                                         │
└─────────────────────────────────────────────────────────────────────────┘
```

### Package Structure

```
org.springframework.samples.petclinic/
├── PetClinicApplication.java      # Main entry point
├── config/
│   └── SwaggerConfig.java         # OpenAPI configuration
├── model/                          # Domain entities
│   ├── BaseEntity.java
│   ├── NamedEntity.java
│   ├── Person.java
│   ├── Owner.java
│   ├── Pet.java
│   ├── PetType.java
│   ├── Vet.java
│   ├── Specialty.java
│   ├── Visit.java
│   ├── User.java
│   └── Role.java
├── repository/                     # Data access layer
│   ├── OwnerRepository.java        # Interface
│   ├── PetRepository.java
│   ├── VetRepository.java
│   ├── VisitRepository.java
│   ├── PetTypeRepository.java
│   ├── SpecialtyRepository.java
│   ├── UserRepository.java
│   ├── springdatajpa/              # Spring Data JPA implementations
│   ├── jpa/                        # JPA implementations
│   └── jdbc/                       # JDBC implementations
├── service/                        # Business logic
│   ├── ClinicService.java          # Interface
│   ├── ClinicServiceImpl.java      # Implementation
│   ├── UserService.java
│   └── UserServiceImpl.java
├── rest/                           # REST API
│   ├── controller/
│   │   ├── OwnerRestController.java
│   │   ├── PetRestController.java
│   │   ├── VetRestController.java
│   │   ├── VisitRestController.java
│   │   ├── PetTypeRestController.java
│   │   ├── SpecialtyRestController.java
│   │   ├── UserRestController.java
│   │   └── RootRestController.java
│   └── advice/
│       └── ExceptionControllerAdvice.java
├── mapper/                         # DTO mappers (MapStruct)
│   ├── OwnerMapper.java
│   ├── PetMapper.java
│   ├── VetMapper.java
│   ├── VisitMapper.java
│   ├── PetTypeMapper.java
│   ├── SpecialtyMapper.java
│   └── UserMapper.java
├── security/                       # Security configuration
│   ├── WebSecurityConfig.java
│   ├── BasicAuthenticationConfig.java
│   ├── DisableSecurityConfig.java
│   └── Roles.java
└── util/
    ├── CallMonitoringAspect.java
    └── EntityUtils.java
```

### Repository Layer

**Interface Pattern**: All repositories define an interface that can have multiple implementations.

```java
// Interface
public interface OwnerRepository {
    Collection<Owner> findByLastName(String lastName);
    Owner findById(int id);
    void save(Owner owner);
    Collection<Owner> findAll();
    void delete(Owner owner);
}

// Spring Data JPA Implementation (active by default)
@Profile("spring-data-jpa")
public interface SpringDataOwnerRepository extends OwnerRepository, Repository<Owner, Integer> {
    @Query("SELECT DISTINCT owner FROM Owner owner left join fetch owner.pets WHERE owner.lastName LIKE :lastName%")
    Collection<Owner> findByLastName(@Param("lastName") String lastName);
}
```

### Service Layer

The `ClinicService` acts as a **facade** for all repository operations, providing:
- Transaction management (`@Transactional`)
- Unified entry point for controllers
- Exception handling

```java
@Service
public class ClinicServiceImpl implements ClinicService {
    private PetRepository petRepository;
    private VetRepository vetRepository;
    private OwnerRepository ownerRepository;
    private VisitRepository visitRepository;
    private SpecialtyRepository specialtyRepository;
    private PetTypeRepository petTypeRepository;
    
    // All CRUD operations with @Transactional
}
```

### Controller Layer (REST API)

Controllers implement OpenAPI-generated interfaces and use MapStruct for DTO conversion.

**API Base URL**: `http://localhost:9966/petclinic/api`

| Controller | Endpoint | Operations |
|------------|----------|------------|
| `OwnerRestController` | `/api/owners` | CRUD for owners, add pets, add visits |
| `PetRestController` | `/api/pets` | CRUD for pets |
| `VetRestController` | `/api/vets` | CRUD for veterinarians |
| `VisitRestController` | `/api/visits` | CRUD for visits |
| `PetTypeRestController` | `/api/pettypes` | CRUD for pet types |
| `SpecialtyRestController` | `/api/specialties` | CRUD for specialties |
| `UserRestController` | `/api/users` | User management |

### OpenAPI/Swagger

API contracts are defined in `src/main/resources/openapi.yml` and DTOs are auto-generated during build.

**Swagger UI**: `http://localhost:9966/petclinic/swagger-ui.html`

---

## Security & Authorization

### Current Authentication Status

> **⚠️ IMPORTANT**: This application does **NOT** have a frontend login page or user registration UI.

| Feature | Status | Description |
|---------|--------|-------------|
| **Login Page** | ❌ Not Implemented | No login UI in the React frontend |
| **Registration Page** | ❌ Not Implemented | No self-registration for users or vets |
| **Session Management** | ❌ Not Implemented | No session/token management in frontend |
| **API Authentication** | ✅ Available (disabled) | HTTP Basic Auth available but disabled by default |

### Frontend Navigation (No Auth)

The React frontend only has these menu items:
- **Home** - Welcome page
- **Find Owners** - Search and manage owners/pets
- **Veterinarians** - View list of vets
- **Error** - Test error handling

**There is no:**
- Login/Logout buttons
- User profile page
- Registration forms
- Protected routes in frontend

### Backend Security Configuration

The application supports two security modes controlled by `petclinic.security.enable`:

| Setting | Configuration Class | Behavior |
|---------|---------------------|----------|
| `false` (default) | `DisableSecurityConfig` | All endpoints publicly accessible |
| `true` | `BasicAuthenticationConfig` | HTTP Basic Auth required |

### When Security is Disabled (Default)

```properties
# application.properties
petclinic.security.enable=false
```

- All API endpoints are publicly accessible
- No authentication required
- CSRF protection disabled
- `@PreAuthorize` annotations are ignored
- Anyone can access all data and perform all operations

### When Security is Enabled

```properties
# application.properties
petclinic.security.enable=true
```

- HTTP Basic Authentication required for all endpoints
- Browser will show native login dialog (not a custom login page)
- Credentials validated against `users` table in database
- JDBC-based authentication using Spring Security

### Role-Based Access Control (RBAC)

When security is enabled, the following roles are defined in `Roles.java`:

| Role | Constant | Controller Access |
|------|----------|-------------------|
| `ROLE_OWNER_ADMIN` | `@roles.OWNER_ADMIN` | Owners, Pets, Visits |
| `ROLE_VET_ADMIN` | `@roles.VET_ADMIN` | Vets, Specialties |
| `ROLE_ADMIN` | `@roles.ADMIN` | Users (add new users) |

### Method-Level Security

Controllers use `@PreAuthorize` annotations (only effective when security is enabled):

```java
// OwnerRestController.java
@PreAuthorize("hasRole(@roles.OWNER_ADMIN)")
public ResponseEntity<List<OwnerDto>> listOwners(String lastName) { ... }

// VetRestController.java  
@PreAuthorize("hasRole(@roles.VET_ADMIN)")
public ResponseEntity<List<VetDto>> listVets() { ... }

// UserRestController.java
@PreAuthorize("hasRole(@roles.ADMIN)")
public ResponseEntity<UserDto> addUser(UserDto userDto) { ... }
```

### User Management

**User Creation**: Only via API (no registration page)
- Endpoint: `POST /api/users`
- Requires: `ROLE_ADMIN` role
- No self-registration capability

**UserRestController.java**:
```java
@PreAuthorize("hasRole(@roles.ADMIN)")
@Override
public ResponseEntity<UserDto> addUser(UserDto userDto) {
    User user = userMapper.toUser(userDto);
    this.userService.saveUser(user);
    return new ResponseEntity<>(userMapper.toUserDto(user), headers, HttpStatus.CREATED);
}
```

### Authentication Storage

Users and roles are stored in database tables:
- `users`: username, password (plain text with `{noop}` prefix), enabled flag
- `roles`: username, role (Many-to-One relationship)

**Default Admin User** (created by `populateDB.sql`):
```sql
INSERT INTO users(username,password,enabled) VALUES ('admin','{noop}admin', true);
INSERT INTO roles (username, role) VALUES ('admin', 'ROLE_OWNER_ADMIN');
INSERT INTO roles (username, role) VALUES ('admin', 'ROLE_VET_ADMIN');
INSERT INTO roles (username, role) VALUES ('admin', 'ROLE_ADMIN');
```

### Password Storage

> **⚠️ SECURITY WARNING**: Passwords are stored in plain text with `{noop}` prefix.
> This is NOT suitable for production use.

```
{noop}admin  →  Plain text password "admin"
```

For production, should use:
- `{bcrypt}` - BCrypt encoding
- `{pbkdf2}` - PBKDF2 encoding
- `{scrypt}` - SCrypt encoding

### CORS Configuration

```java
// WebSecurityConfig.java
CorsConfiguration configuration = new CorsConfiguration();
configuration.setAllowedOrigins(List.of("http://localhost:4444"));
configuration.setAllowedMethods(List.of("OPTIONS", "GET", "POST", "PUT"));
```

### Authentication Flow (When Enabled)

```
┌─────────────┐                    ┌──────────────────┐
│   Browser   │                    │  Spring Backend  │
└──────┬──────┘                    └────────┬─────────┘
       │                                    │
       │  1. Request to /api/owners         │
       │ ─────────────────────────────────► │
       │                                    │
       │  2. 401 Unauthorized               │
       │ ◄───────────────────────────────── │
       │                                    │
       │  3. Browser shows native           │
       │     login dialog                   │
       │                                    │
       │  4. User enters credentials        │
       │                                    │
       │  5. Request with Basic Auth header │
       │     Authorization: Basic base64    │
       │ ─────────────────────────────────► │
       │                                    │
       │  6. JDBC Authentication            │
       │     - Query users table            │
       │     - Query roles table            │
       │     - Check @PreAuthorize          │
       │                                    │
       │  7. 200 OK + Data                  │
       │ ◄───────────────────────────────── │
       │                                    │
```

### What's Missing for Full Authentication

To have complete authentication, the following would need to be implemented:

| Component | Current State | Required for Full Auth |
|-----------|---------------|------------------------|
| Login Page UI | ❌ Missing | React login form component |
| Registration Page | ❌ Missing | React registration form |
| JWT/Session Tokens | ❌ Missing | Token-based auth instead of Basic |
| Password Encryption | ❌ Plain text | BCrypt or similar |
| Logout Functionality | ❌ Missing | Session invalidation |
| Protected Routes | ❌ Missing | React Router guards |
| Auth Context/State | ❌ Missing | React Context for auth state |
| Refresh Tokens | ❌ Missing | Token refresh mechanism |

---

## Frontend Architecture

### Technology Overview

- **React 15.x** with **TypeScript**
- **React Router 2.x** for navigation
- **Bootstrap 3.x** for styling
- **Less** for CSS preprocessing
- **Webpack 5** for bundling

### Component Organization

```
client/src/
├── main.tsx                    # Application entry point
├── Root.tsx                    # Router provider
├── configureRoutes.tsx         # Route definitions
├── components/
│   ├── App.tsx                 # Main layout wrapper
│   ├── Menu.tsx                # Navigation menu
│   ├── WelcomePage.tsx         # Home page
│   ├── ErrorPage.tsx           # Error display
│   ├── NotFoundPage.tsx        # 404 page
│   ├── form/                   # Reusable form components
│   │   ├── Input.tsx
│   │   ├── SelectInput.tsx
│   │   ├── DateInput.tsx
│   │   ├── Constraints.ts
│   │   └── FieldFeedbackPanel.tsx
│   ├── owners/                 # Owner domain
│   │   ├── FindOwnersPage.tsx
│   │   ├── OwnersPage.tsx
│   │   ├── NewOwnerPage.tsx
│   │   ├── EditOwnerPage.tsx
│   │   ├── OwnerEditor.tsx
│   │   ├── OwnerInformation.tsx
│   │   ├── OwnersTable.tsx
│   │   └── PetsTable.tsx
│   ├── pets/                   # Pet domain
│   │   ├── NewPetPage.tsx
│   │   ├── EditPetPage.tsx
│   │   ├── PetEditor.tsx
│   │   ├── LoadingPanel.tsx
│   │   └── createPetEditorModel.ts
│   ├── vets/                   # Vet domain
│   │   └── VetsPage.tsx
│   └── visits/                 # Visit domain
│       ├── VisitsPage.tsx
│       └── PetDetails.tsx
├── styles/
│   ├── less/                   # Less stylesheets
│   └── fonts/                  # Custom fonts
├── types/                      # TypeScript type definitions
└── util/                       # Utility functions
```

### Domain-Based Component Structure

#### Owners Domain (`/owners/*`)

| Component | Route | Description |
|-----------|-------|-------------|
| `FindOwnersPage` | `/owners/list` | Search owners by last name |
| `OwnersPage` | `/owners/:ownerId` | View owner details with pets |
| `NewOwnerPage` | `/owners/new` | Create new owner |
| `EditOwnerPage` | `/owners/:ownerId/edit` | Edit owner details |
| `OwnerEditor` | - | Reusable owner form |
| `OwnerInformation` | - | Owner details display |
| `OwnersTable` | - | List of owners |
| `PetsTable` | - | Owner's pets list |

#### Pets Domain (`/owners/:ownerId/pets/*`)

| Component | Route | Description |
|-----------|-------|-------------|
| `NewPetPage` | `/owners/:ownerId/pets/new` | Add pet to owner |
| `EditPetPage` | `/owners/:ownerId/pets/:petId/edit` | Edit pet details |
| `PetEditor` | - | Reusable pet form |

#### Vets Domain (`/vets`)

| Component | Route | Description |
|-----------|-------|-------------|
| `VetsPage` | `/vets` | List all veterinarians with specialties |

#### Visits Domain

| Component | Route | Description |
|-----------|-------|-------------|
| `VisitsPage` | `/owners/:ownerId/pets/:petId/visits/new` | Add visit for pet |
| `PetDetails` | - | Pet information for visit context |

### Routing Configuration

```tsx
<Route component={App}>
  <Route path='/' component={WelcomePage} />
  <Route path='/owners/list' component={FindOwnersPage} />
  <Route path='/owners/new' component={NewOwnerPage} />
  <Route path='/owners/:ownerId/edit' component={EditOwnerPage} />
  <Route path='/owners/:ownerId/pets/:petId/edit' component={EditPetPage} />
  <Route path='/owners/:ownerId/pets/new' component={NewPetPage} />
  <Route path='/owners/:ownerId/pets/:petId/visits/new' component={VisitsPage} />
  <Route path='/owners/:ownerId' component={OwnersPage} />
  <Route path='/vets' component={VetsPage} />
  <Route path='/error' component={ErrorPage} />
  <Route path='*' component={NotFoundPage} />
</Route>
```

### API Integration

Frontend communicates with backend via REST API:
- **Base URL**: `http://localhost:9966/petclinic`
- **Configured in**: `webpack.config.js` → `__API_SERVER_URL__`
- **HTTP Client**: `whatwg-fetch` (Fetch API polyfill)

---

## Data Flow

```
┌─────────────┐     HTTP      ┌──────────────────┐
│   React     │ ──────────────│  REST Controller │
│  Frontend   │    JSON       │                  │
└─────────────┘               └────────┬─────────┘
                                       │
                              ┌────────▼─────────┐
                              │     Mapper       │
                              │   (MapStruct)    │
                              └────────┬─────────┘
                                       │
                              ┌────────▼─────────┐
                              │  ClinicService   │
                              │   (Facade)       │
                              └────────┬─────────┘
                                       │
                              ┌────────▼─────────┐
                              │   Repository     │
                              │ (Spring Data JPA)│
                              └────────┬─────────┘
                                       │
                              ┌────────▼─────────┐
                              │    Database      │
                              │   (HSQLDB)       │
                              └──────────────────┘
```

---

## Configuration Summary

### Application Properties

| Property | Value | Description |
|----------|-------|-------------|
| `server.port` | 9966 | Backend server port |
| `server.servlet.context-path` | /petclinic/ | API context path |
| `spring.profiles.active` | hsqldb,spring-data-jpa | Active profiles |
| `petclinic.security.enable` | false | Security enabled/disabled |
| `spring.jpa.open-in-view` | false | Lazy loading outside transaction |

### Frontend Configuration

| Config | File | Value |
|--------|------|-------|
| API URL | `webpack.config.js` | `http://localhost:9966/petclinic` |
| Dev Port | Environment `PORT` | 3000 (default) |

---

## Summary

The Spring PetClinic application demonstrates:

1. **Clean Architecture**: Separation between presentation, business logic, and data access
2. **Multiple Database Support**: Profile-based configuration for HSQLDB, MySQL, PostgreSQL
3. **Flexible Repository Layer**: Support for Spring Data JPA, JPA, and JDBC
4. **API-First Design**: OpenAPI specification with auto-generated DTOs
5. **Security Options**: Configurable authentication with role-based access control
6. **Modern Frontend**: React with TypeScript and domain-driven component organization

