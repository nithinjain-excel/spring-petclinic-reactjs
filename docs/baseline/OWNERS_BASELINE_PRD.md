# Owners - Baseline PRD

| Version | Date | Author | Status |
|---------|------|--------|--------|
| 1.1 | 2026-01-17 | AI Assistant | Baseline |

> **Change Log:** v1.1 - Added reference to centralized Database Migration Strategy in ARCHITECTURE.md

---

## Overview

This document outlines the current baseline implementation of the Owners domain in the Spring PetClinic application. Owners are pet owners who bring their pets to the clinic for veterinary services. The system provides full CRUD operations for owner management with a React-based frontend interface.

---

## Business Requirements

### Current State (Baseline)

#### Owner Management
- Owners can be created with personal and contact information
- Owners can be searched by last name (partial match)
- Owners can view and update their profile information
- Owners can be deleted from the system
- All owner operations require `ROLE_OWNER_ADMIN` authorization (when security enabled)

#### Data Requirements
- First name and last name are required
- Address and city are required
- Telephone is required and must be numeric (max 10 digits)
- Each owner can have multiple pets associated

#### Relationships
- Owner → Pets: One-to-Many (owner owns multiple pets)
- Pet → Visits: One-to-Many (cascaded through pet management)

---

## Technical Requirements

### Database Schema

#### Owners Table

```sql
-- Location: src/main/resources/db/hsqldb/initDB.sql

CREATE TABLE owners (
  id         INTEGER IDENTITY PRIMARY KEY,
  first_name VARCHAR(30),
  last_name  VARCHAR_IGNORECASE(30),
  address    VARCHAR(255),
  city       VARCHAR(80),
  telephone  VARCHAR(20)
);

CREATE INDEX owners_last_name ON owners (last_name);
```

| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| id | INTEGER | PK, AUTO | Unique owner identifier |
| first_name | VARCHAR(30) | - | Owner's first name |
| last_name | VARCHAR_IGNORECASE(30) | INDEXED | Owner's last name (case-insensitive) |
| address | VARCHAR(255) | - | Street address |
| city | VARCHAR(80) | - | City of residence |
| telephone | VARCHAR(20) | - | Contact phone number |

#### Sample Data

```sql
-- Location: src/main/resources/db/hsqldb/populateDB.sql

INSERT INTO owners VALUES (1, 'George', 'Franklin', '110 W. Liberty St.', 'Madison', '6085551023');
INSERT INTO owners VALUES (2, 'Betty', 'Davis', '638 Cardinal Ave.', 'Sun Prairie', '6085551749');
INSERT INTO owners VALUES (3, 'Eduardo', 'Rodriquez', '2693 Commerce St.', 'McFarland', '6085558763');
INSERT INTO owners VALUES (4, 'Harold', 'Davis', '563 Friendly St.', 'Windsor', '6085553198');
INSERT INTO owners VALUES (5, 'Peter', 'McTavish', '2387 S. Fair Way', 'Madison', '6085552765');
INSERT INTO owners VALUES (6, 'Jean', 'Coleman', '105 N. Lake St.', 'Monona', '6085552654');
INSERT INTO owners VALUES (7, 'Jeff', 'Black', '1450 Oak Blvd.', 'Monona', '6085555387');
INSERT INTO owners VALUES (8, 'Maria', 'Escobito', '345 Maple St.', 'Madison', '6085557683');
INSERT INTO owners VALUES (9, 'David', 'Schroeder', '2749 Blackhawk Trail', 'Madison', '6085559435');
INSERT INTO owners VALUES (10, 'Carlos', 'Estaban', '2335 Independence La.', 'Waunakee', '6085555487');
```

#### Entity Relationships

```
┌──────────────────┐         ┌──────────────────┐         ┌──────────────────┐
│     owners       │         │      pets        │         │     visits       │
├──────────────────┤         ├──────────────────┤         ├──────────────────┤
│ id (PK)          │◄────────│ owner_id (FK)    │         │ pet_id (FK)      │────►│
│ first_name       │    1:N  │ id (PK)          │◄────────│ id (PK)          │ 1:N
│ last_name        │         │ name             │    1:N  │ date             │
│ address          │         │ birth_date       │         │ description      │
│ city             │         │ type_id (FK)     │         └──────────────────┘
│ telephone        │         └──────────────────┘
└──────────────────┘
```

---

### Domain Model Layer

#### BaseEntity (Parent Class)

```java
// Location: src/main/java/org/springframework/samples/petclinic/model/BaseEntity.java

@MappedSuperclass
public class BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    protected Integer id;

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    
    @JsonIgnore
    public boolean isNew() { return this.id == null; }
}
```

#### Person (Parent Class)

```java
// Location: src/main/java/org/springframework/samples/petclinic/model/Person.java

@MappedSuperclass
public class Person extends BaseEntity {

    @Column(name = "first_name")
    @NotEmpty
    protected String firstName;

    @Column(name = "last_name")
    @NotEmpty
    protected String lastName;
    
    // Getters and setters
}
```

#### Owner Entity

```java
// Location: src/main/java/org/springframework/samples/petclinic/model/Owner.java

@Entity
@Table(name = "owners")
public class Owner extends Person {
    
    @Column(name = "address")
    @NotEmpty
    private String address;

    @Column(name = "city")
    @NotEmpty
    private String city;

    @Column(name = "telephone")
    @NotEmpty
    @Digits(fraction = 0, integer = 10)
    private String telephone;

    @OneToMany(cascade = CascadeType.ALL, mappedBy = "owner", fetch = FetchType.EAGER)
    private Set<Pet> pets;
    
    // Helper methods
    public void addPet(Pet pet) {
        getPetsInternal().add(pet);
        pet.setOwner(this);
    }
    
    public Pet getPet(String name) { /* ... */ }
    public Pet getPet(String name, boolean ignoreNew) { /* ... */ }
}
```

**Inheritance Hierarchy:**
```
BaseEntity
    └── Person
        └── Owner
```

**Validation Annotations:**
| Field | Annotation | Description |
|-------|------------|-------------|
| firstName | `@NotEmpty` | Inherited from Person |
| lastName | `@NotEmpty` | Inherited from Person |
| address | `@NotEmpty` | Must not be empty |
| city | `@NotEmpty` | Must not be empty |
| telephone | `@NotEmpty`, `@Digits(fraction=0, integer=10)` | Numeric, max 10 digits |

---

### Repository Layer

#### OwnerRepository Interface

```java
// Location: src/main/java/org/springframework/samples/petclinic/repository/OwnerRepository.java

public interface OwnerRepository {
    
    /**
     * Find owners by last name (starts with)
     */
    Collection<Owner> findByLastName(String lastName) throws DataAccessException;
    
    /**
     * Find owner by ID
     */
    Owner findById(int id) throws DataAccessException;
    
    /**
     * Save (insert or update) owner
     */
    void save(Owner owner) throws DataAccessException;
    
    /**
     * Get all owners
     */
    Collection<Owner> findAll() throws DataAccessException;
    
    /**
     * Delete owner
     */
    void delete(Owner owner) throws DataAccessException;
}
```

#### Spring Data JPA Implementation

```java
// Location: src/main/java/org/springframework/samples/petclinic/repository/springdatajpa/SpringDataOwnerRepository.java

@Profile("spring-data-jpa")
public interface SpringDataOwnerRepository extends OwnerRepository, Repository<Owner, Integer> {

    @Override
    @Query("SELECT DISTINCT owner FROM Owner owner left join fetch owner.pets WHERE owner.lastName LIKE :lastName%")
    Collection<Owner> findByLastName(@Param("lastName") String lastName);

    @Override
    @Query("SELECT owner FROM Owner owner left join fetch owner.pets WHERE owner.id =:id")
    Owner findById(@Param("id") int id);
}
```

**Query Features:**
| Method | Query Type | Feature |
|--------|------------|---------|
| `findByLastName` | JPQL | Partial match with `LIKE`, eager fetches pets |
| `findById` | JPQL | Eager fetches pets |
| `findAll` | Spring Data | Inherited from Repository |
| `save` | Spring Data | Inherited from Repository |
| `delete` | Spring Data | Inherited from Repository |

---

### Service Layer

#### ClinicService Interface (Owner Methods)

```java
// Location: src/main/java/org/springframework/samples/petclinic/service/ClinicService.java

public interface ClinicService {
    
    Owner findOwnerById(int id) throws DataAccessException;
    
    Collection<Owner> findAllOwners() throws DataAccessException;
    
    void saveOwner(Owner owner) throws DataAccessException;
    
    void deleteOwner(Owner owner) throws DataAccessException;
    
    Collection<Owner> findOwnerByLastName(String lastName) throws DataAccessException;
}
```

#### ClinicServiceImpl (Owner Methods)

```java
// Location: src/main/java/org/springframework/samples/petclinic/service/ClinicServiceImpl.java

@Service
public class ClinicServiceImpl implements ClinicService {

    @Autowired
    private OwnerRepository ownerRepository;

    @Override
    @Transactional(readOnly = true)
    public Owner findOwnerById(int id) throws DataAccessException {
        Owner owner = null;
        try {
            owner = ownerRepository.findById(id);
        } catch (ObjectRetrievalFailureException | EmptyResultDataAccessException e) {
            return null;
        }
        return owner;
    }

    @Override
    @Transactional(readOnly = true)
    public Collection<Owner> findAllOwners() throws DataAccessException {
        return ownerRepository.findAll();
    }

    @Override
    @Transactional
    public void saveOwner(Owner owner) throws DataAccessException {
        ownerRepository.save(owner);
    }

    @Override
    @Transactional
    public void deleteOwner(Owner owner) throws DataAccessException {
        ownerRepository.delete(owner);
    }

    @Override
    @Transactional(readOnly = true)
    public Collection<Owner> findOwnerByLastName(String lastName) throws DataAccessException {
        return ownerRepository.findByLastName(lastName);
    }
}
```

**Transaction Configuration:**
| Method | Transaction | Description |
|--------|-------------|-------------|
| `findOwnerById` | Read-only | Single owner lookup |
| `findAllOwners` | Read-only | All owners retrieval |
| `saveOwner` | Read-write | Insert or update |
| `deleteOwner` | Read-write | Delete owner (cascades to pets) |
| `findOwnerByLastName` | Read-only | Search by last name |

---

### Mapper Layer

#### OwnerMapper

```java
// Location: src/main/java/org/springframework/samples/petclinic/mapper/OwnerMapper.java

@Mapper(uses = PetMapper.class)
public interface OwnerMapper {
    
    OwnerDto toOwnerDto(Owner owner);
    
    Owner toOwner(OwnerDto ownerDto);
    
    Owner toOwner(OwnerFieldsDto ownerDto);
    
    List<OwnerDto> toOwnerDtoCollection(Collection<Owner> ownerCollection);
    
    Collection<Owner> toOwners(Collection<OwnerDto> ownerDtos);
}
```

**MapStruct Configuration:**
- Uses `PetMapper` for nested pet conversion
- Bidirectional mapping between Entity and DTOs
- Collection mapping support

---

### API Endpoints

#### OwnerRestController

```java
// Location: src/main/java/org/springframework/samples/petclinic/rest/controller/OwnerRestController.java

@RestController
@CrossOrigin(exposedHeaders = "errors, content-type")
@RequestMapping("/api")
public class OwnerRestController implements OwnersApi {
    // ... implementation
}
```

#### API Summary Table

| Method | Endpoint | Authorization | Description |
|--------|----------|---------------|-------------|
| GET | `/api/owners` | ROLE_OWNER_ADMIN | List all owners or filter by lastName |
| GET | `/api/owners/{ownerId}` | ROLE_OWNER_ADMIN | Get owner by ID |
| POST | `/api/owners` | ROLE_OWNER_ADMIN | Create new owner |
| PUT | `/api/owners/{ownerId}` | ROLE_OWNER_ADMIN | Update owner |
| DELETE | `/api/owners/{ownerId}` | ROLE_OWNER_ADMIN | Delete owner |
| POST | `/api/owners/{ownerId}/pets` | ROLE_OWNER_ADMIN | Add pet to owner |
| GET | `/api/owners/{ownerId}/pets/{petId}` | ROLE_OWNER_ADMIN | Get specific pet of owner |
| POST | `/api/owners/{ownerId}/pets/{petId}/visits` | ROLE_OWNER_ADMIN | Add visit for pet |

---

#### GET /api/owners - List Owners

```java
@PreAuthorize("hasRole(@roles.OWNER_ADMIN)")
@Override
public ResponseEntity<List<OwnerDto>> listOwners(String lastName) {
    Collection<Owner> owners;
    if (lastName != null) {
        owners = this.clinicService.findOwnerByLastName(lastName);
    } else {
        owners = this.clinicService.findAllOwners();
    }
    if (owners.isEmpty()) {
        return new ResponseEntity<>(HttpStatus.NOT_FOUND);
    }
    return new ResponseEntity<>(ownerMapper.toOwnerDtoCollection(owners), HttpStatus.OK);
}
```

**Query Parameters:**
| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| lastName | String | No | Filter by last name (partial match) |

**Response (200 OK):**
```json
[
  {
    "id": 1,
    "firstName": "George",
    "lastName": "Franklin",
    "address": "110 W. Liberty St.",
    "city": "Madison",
    "telephone": "6085551023",
    "pets": [
      {
        "id": 1,
        "name": "Leo",
        "birthDate": "2010-09-07",
        "type": { "id": 1, "name": "cat" }
      }
    ]
  }
]
```

**Response Codes:**
| Status | Description |
|--------|-------------|
| 200 OK | Owners found |
| 404 Not Found | No owners match criteria |
| 401 Unauthorized | Not authenticated (when security enabled) |
| 403 Forbidden | User lacks ROLE_OWNER_ADMIN |

---

#### GET /api/owners/{ownerId} - Get Owner

```java
@PreAuthorize("hasRole(@roles.OWNER_ADMIN)")
@Override
public ResponseEntity<OwnerDto> getOwner(Integer ownerId) {
    Owner owner = this.clinicService.findOwnerById(ownerId);
    if (owner == null) {
        return new ResponseEntity<>(HttpStatus.NOT_FOUND);
    }
    return new ResponseEntity<>(ownerMapper.toOwnerDto(owner), HttpStatus.OK);
}
```

**Response Codes:**
| Status | Description |
|--------|-------------|
| 200 OK | Owner found |
| 404 Not Found | Owner does not exist |

---

#### POST /api/owners - Create Owner

```java
@PreAuthorize("hasRole(@roles.OWNER_ADMIN)")
@Override
public ResponseEntity<OwnerDto> addOwner(OwnerFieldsDto ownerFieldsDto) {
    HttpHeaders headers = new HttpHeaders();
    Owner owner = ownerMapper.toOwner(ownerFieldsDto);
    this.clinicService.saveOwner(owner);
    OwnerDto ownerDto = ownerMapper.toOwnerDto(owner);
    headers.setLocation(UriComponentsBuilder.newInstance()
        .path("/api/owners/{id}").buildAndExpand(owner.getId()).toUri());
    return new ResponseEntity<>(ownerDto, headers, HttpStatus.CREATED);
}
```

**Request Body:**
```json
{
  "firstName": "John",
  "lastName": "Doe",
  "address": "123 Main St.",
  "city": "Springfield",
  "telephone": "5551234567"
}
```

**Response (201 Created):**
- Returns created owner with assigned ID
- Location header set to `/api/owners/{newId}`

---

#### PUT /api/owners/{ownerId} - Update Owner

```java
@PreAuthorize("hasRole(@roles.OWNER_ADMIN)")
@Override
public ResponseEntity<OwnerDto> updateOwner(Integer ownerId, OwnerFieldsDto ownerFieldsDto) {
    Owner currentOwner = this.clinicService.findOwnerById(ownerId);
    if (currentOwner == null) {
        return new ResponseEntity<>(HttpStatus.NOT_FOUND);
    }
    currentOwner.setAddress(ownerFieldsDto.getAddress());
    currentOwner.setCity(ownerFieldsDto.getCity());
    currentOwner.setFirstName(ownerFieldsDto.getFirstName());
    currentOwner.setLastName(ownerFieldsDto.getLastName());
    currentOwner.setTelephone(ownerFieldsDto.getTelephone());
    this.clinicService.saveOwner(currentOwner);
    return new ResponseEntity<>(ownerMapper.toOwnerDto(currentOwner), HttpStatus.NO_CONTENT);
}
```

**Response Codes:**
| Status | Description |
|--------|-------------|
| 204 No Content | Owner updated successfully |
| 404 Not Found | Owner does not exist |

---

#### DELETE /api/owners/{ownerId} - Delete Owner

```java
@PreAuthorize("hasRole(@roles.OWNER_ADMIN)")
@Transactional
@Override
public ResponseEntity<OwnerDto> deleteOwner(Integer ownerId) {
    Owner owner = this.clinicService.findOwnerById(ownerId);
    if (owner == null) {
        return new ResponseEntity<>(HttpStatus.NOT_FOUND);
    }
    this.clinicService.deleteOwner(owner);
    return new ResponseEntity<>(HttpStatus.NO_CONTENT);
}
```

**Note:** Delete cascades to associated pets and visits.

---

### Frontend Implementation

#### Component Structure

```
client/src/components/owners/
├── FindOwnersPage.tsx      # Search form and results list
├── OwnersPage.tsx          # Owner detail view
├── NewOwnerPage.tsx        # Create new owner form
├── EditOwnerPage.tsx       # Edit existing owner form
├── OwnerEditor.tsx         # Reusable owner form component
├── OwnerInformation.tsx    # Owner details display
├── OwnersTable.tsx         # Table listing owners
└── PetsTable.tsx           # Table listing owner's pets
```

#### TypeScript Interfaces

```typescript
// Location: client/src/types/index.ts

interface IBaseEntity {
  id: number;
  isNew: boolean;
}

interface IPerson extends IBaseEntity {
  firstName: string;
  lastName: string;
}

export interface IOwner extends IPerson {
  address: string;
  city: string;
  telephone: string;
  pets: IPet[];
}
```

---

#### FindOwnersPage Component

```typescript
// Location: client/src/components/owners/FindOwnersPage.tsx

export default class FindOwnersPage extends React.Component<IFindOwnersPageProps, IFindOwnersPageState> {
  
  fetchData(filter: string) {
    const query = filter ? encodeURIComponent(filter) : '';
    const requestUrl = url('api/owners?lastName=' + query);

    fetch(requestUrl)
      .then(response => response.json())
      .then(owners => { this.setState({ owners }); });
  }
  
  // Renders search form + OwnersTable + "Add Owner" link
}
```

**Features:**
- Search by last name (partial match)
- URL query parameter sync (`?lastName=...`)
- Results displayed in `OwnersTable`
- Link to create new owner

---

#### OwnersPage Component

```typescript
// Location: client/src/components/owners/OwnersPage.tsx

export default class OwnersPage extends React.Component<IOwnersPageProps, IOwnerPageState> {

  componentDidMount() {
    const { params } = this.props;

    if (params && params.ownerId) {
      const fetchUrl = url(`/api/owner/${params.ownerId}`);
      fetch(fetchUrl)
        .then(response => response.json())
        .then(owner => this.setState({ owner }));
    }
  }
  
  // Renders OwnerInformation + PetsTable
}
```

**Note:** URL has a bug - uses `/api/owner/` but backend expects `/api/owners/`

---

#### OwnerEditor Component

```typescript
// Location: client/src/components/owners/OwnerEditor.tsx

export default class OwnerEditor extends React.Component<IOwnerEditorProps, IOwnerEditorState> {

  onSubmit(event) {
    const { owner } = this.state;

    const url = owner.isNew ? '/api/owners' : '/api/owners/' + owner.id;
    submitForm(owner.isNew ? 'POST' : 'PUT', url, owner, (status, response) => {
      if (status === 200 || status === 201) {
        const newOwner = response as IOwner;
        this.context.router.push({ pathname: '/owners/' + newOwner.id });
      } else {
        this.setState({ error: response });
      }
    });
  }
}
```

**Form Fields:**

| Field | Validation | Constraint |
|-------|------------|------------|
| First Name | NotEmpty | Required |
| Last Name | NotEmpty | Required |
| Address | NotEmpty | Required |
| City | NotEmpty | Required |
| Telephone | Digits(10) | Numeric, max 10 digits |

---

#### Frontend Routes

```typescript
// Location: client/src/configureRoutes.tsx

<Route path='/owners/list' component={FindOwnersPage} />
<Route path='/owners/new' component={NewOwnerPage} />
<Route path='/owners/:ownerId(\d+)' component={OwnersPage} />
<Route path='/owners/:ownerId(\d+)/edit' component={EditOwnerPage} />
```

| Route | Component | Purpose |
|-------|-----------|---------|
| `/owners/list` | FindOwnersPage | Search owners |
| `/owners/new` | NewOwnerPage | Create owner |
| `/owners/:ownerId` | OwnersPage | View owner details |
| `/owners/:ownerId/edit` | EditOwnerPage | Edit owner |

---

## Implementation Phases

### Phase 1: Database Layer - ✅ BASELINE COMPLETE

**Objective**: Owner data storage

**Existing Components**:
1. ✅ `owners` table with all required fields
2. ✅ Index on `last_name` for search optimization
3. ✅ 10 sample owners seeded

**Files**:
- `src/main/resources/db/hsqldb/initDB.sql`
- `src/main/resources/db/hsqldb/populateDB.sql`

---

### Phase 2: Domain Model Layer - ✅ BASELINE COMPLETE

**Objective**: JPA entity for owners

**Existing Components**:
1. ✅ BaseEntity with id and isNew
2. ✅ Person with firstName/lastName
3. ✅ Owner entity with address/city/telephone/pets
4. ✅ Validation annotations
5. ✅ Pet collection relationship

**Files**:
- `src/main/java/.../model/BaseEntity.java`
- `src/main/java/.../model/Person.java`
- `src/main/java/.../model/Owner.java`

---

### Phase 3: Repository Layer - ✅ BASELINE COMPLETE

**Objective**: Data access for owners

**Existing Components**:
1. ✅ OwnerRepository interface
2. ✅ SpringDataOwnerRepository implementation
3. ✅ Custom JPQL queries with eager fetching

**Files**:
- `src/main/java/.../repository/OwnerRepository.java`
- `src/main/java/.../repository/springdatajpa/SpringDataOwnerRepository.java`

---

### Phase 4: Service Layer - ✅ BASELINE COMPLETE

**Objective**: Owner business logic

**Existing Components**:
1. ✅ ClinicService interface with owner methods
2. ✅ ClinicServiceImpl with transaction management
3. ✅ Exception handling for not found scenarios

**Files**:
- `src/main/java/.../service/ClinicService.java`
- `src/main/java/.../service/ClinicServiceImpl.java`

---

### Phase 5: API Layer - ✅ BASELINE COMPLETE

**Objective**: REST endpoints for owners

**Existing Components**:
1. ✅ OwnerRestController with CRUD operations
2. ✅ OwnerMapper for DTO conversion
3. ✅ @PreAuthorize security on all endpoints
4. ✅ Nested endpoints for pets and visits

**Files**:
- `src/main/java/.../rest/controller/OwnerRestController.java`
- `src/main/java/.../mapper/OwnerMapper.java`

---

### Phase 6: Frontend Implementation - ✅ BASELINE COMPLETE

**Objective**: Owner management UI

**Existing Components**:
1. ✅ FindOwnersPage - search functionality
2. ✅ OwnersPage - owner detail view
3. ✅ NewOwnerPage - create owner
4. ✅ EditOwnerPage - edit owner
5. ✅ OwnerEditor - reusable form
6. ✅ OwnersTable - list display
7. ✅ PetsTable - owner's pets display
8. ✅ Client-side validation

**Files**:
- `client/src/components/owners/*.tsx`
- `client/src/types/index.ts`

---

## Known Issues

### Frontend API URL Bug

**Location**: `client/src/components/owners/OwnersPage.tsx`

```typescript
// Current (Bug):
const fetchUrl = url(`/api/owner/${params.ownerId}`);

// Should be:
const fetchUrl = url(`/api/owners/${params.ownerId}`);
```

The endpoint uses singular `/api/owner/` but the backend expects plural `/api/owners/`.

### Edit Owner Page Same Bug

**Location**: `client/src/components/owners/EditOwnerPage.tsx`

```typescript
// Current (Bug):
const fetchUrl = url(`/api/owner/${params.ownerId}`);

// Should be:
const fetchUrl = url(`/api/owners/${params.ownerId}`);
```

---

## Success Criteria

### Backend (Baseline - Complete)
- [x] Owners table exists with proper schema
- [x] Owner entity maps to database
- [x] CRUD operations work via repository
- [x] Service layer provides transaction management
- [x] REST API exposes all owner operations
- [x] Role-based security on endpoints
- [x] Search by last name works
- [x] Owner-pet relationship maintained

### Frontend (Baseline - Complete with Issues)
- [x] Search owners page renders
- [x] Owner details page renders
- [x] Create owner form works
- [x] Edit owner form works
- [x] Form validation works
- [ ] API URLs are correct (known bug)
- [x] Navigation between pages works

---

## Database Migration

> **📖 Reference**: See [ARCHITECTURE.md - Database Migration Strategy](../ARCHITECTURE.md#database-migration-strategy) for the complete migration documentation applicable to all domains.

**Key Points for Owners:**
- Customer data requires careful migration planning
- Potential future columns: email, profile_image
- FK relationship to `pets` table must be maintained during migrations

---

## Dependencies

### Backend Dependencies
- Spring Data JPA
- Jakarta Validation API
- MapStruct

### Frontend Dependencies
- React 15.x
- React Router
- TypeScript

---

## Future Enhancements

1. **Pagination** - Add pagination to owner list for scalability
2. **Advanced Search** - Search by multiple fields
3. **Sorting** - Sort results by name, city, etc.
4. **Bulk Operations** - Delete multiple owners
5. **Export** - Export owner data to CSV/PDF
6. **Owner Portal** - Self-service owner access

---

## Current Status

**Last Updated**: 2026-01-16
**Current Phase**: Baseline Documentation
**Status**: ✅ BASELINE COMPLETE
**Known Issues**: Frontend API URL bug (singular vs plural)
**Next Steps**: Fix frontend URL bugs (if required)

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

