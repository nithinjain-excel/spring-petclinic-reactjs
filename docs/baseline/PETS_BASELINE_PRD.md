# Pets - Baseline PRD

| Version | Date | Author | Status |
|---------|------|--------|--------|
| 1.1 | 2026-01-17 | AI Assistant | Baseline |

> **Change Log:** v1.1 - Added reference to centralized Database Migration Strategy in ARCHITECTURE.md

---

## Overview

This document outlines the current baseline implementation of the Pets domain in the Spring PetClinic application. Pets are the animals owned by pet owners and treated by veterinarians at the clinic. The system provides full CRUD operations for pet management with a React-based frontend interface.

---

## Business Requirements

### Current State (Baseline)

#### Pet Management
- Pets can be created with name, birth date, and type
- Pets are always associated with an owner
- Pets can be updated (name, birth date, type)
- Pets can be deleted from the system
- All pet operations require `ROLE_OWNER_ADMIN` authorization (when security enabled)

#### Data Requirements
- Pet name is required
- Birth date is stored as a date
- Pet type is required (cat, dog, lizard, snake, bird, hamster)
- Owner association is mandatory

#### Relationships
- Owner → Pet: Many-to-One (pet belongs to one owner)
- Pet → PetType: Many-to-One (pet has one type)
- Pet → Visits: One-to-Many (pet can have multiple visits)

---

## Technical Requirements

### Database Schema

#### Types Table (Pet Types)

```sql
-- Location: src/main/resources/db/hsqldb/initDB.sql

CREATE TABLE types (
  id   INTEGER IDENTITY PRIMARY KEY,
  name VARCHAR(80)
);
CREATE INDEX types_name ON types (name);
```

| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| id | INTEGER | PK, AUTO | Unique type identifier |
| name | VARCHAR(80) | INDEXED | Type name (cat, dog, etc.) |

#### Pets Table

```sql
-- Location: src/main/resources/db/hsqldb/initDB.sql

CREATE TABLE pets (
  id         INTEGER IDENTITY PRIMARY KEY,
  name       VARCHAR(30),
  birth_date DATE,
  type_id    INTEGER NOT NULL,
  owner_id   INTEGER NOT NULL
);

ALTER TABLE pets ADD CONSTRAINT fk_pets_owners 
  FOREIGN KEY (owner_id) REFERENCES owners (id);
ALTER TABLE pets ADD CONSTRAINT fk_pets_types 
  FOREIGN KEY (type_id) REFERENCES types (id);
  
CREATE INDEX pets_name ON pets (name);
```

| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| id | INTEGER | PK, AUTO | Unique pet identifier |
| name | VARCHAR(30) | INDEXED | Pet's name |
| birth_date | DATE | - | Pet's birth date |
| type_id | INTEGER | FK → types.id, NOT NULL | Pet type reference |
| owner_id | INTEGER | FK → owners.id, NOT NULL | Owner reference |

#### Sample Data

```sql
-- Location: src/main/resources/db/hsqldb/populateDB.sql

-- Pet Types
INSERT INTO types VALUES (1, 'cat');
INSERT INTO types VALUES (2, 'dog');
INSERT INTO types VALUES (3, 'lizard');
INSERT INTO types VALUES (4, 'snake');
INSERT INTO types VALUES (5, 'bird');
INSERT INTO types VALUES (6, 'hamster');

-- Pets
INSERT INTO pets VALUES (1, 'Leo', '2010-09-07', 1, 1);
INSERT INTO pets VALUES (2, 'Basil', '2012-08-06', 6, 2);
INSERT INTO pets VALUES (3, 'Rosy', '2011-04-17', 2, 3);
INSERT INTO pets VALUES (4, 'Jewel', '2010-03-07', 2, 3);
INSERT INTO pets VALUES (5, 'Iggy', '2010-11-30', 3, 4);
INSERT INTO pets VALUES (6, 'George', '2010-01-20', 4, 5);
INSERT INTO pets VALUES (7, 'Samantha', '2012-09-04', 1, 6);
INSERT INTO pets VALUES (8, 'Max', '2012-09-04', 1, 6);
INSERT INTO pets VALUES (9, 'Lucky', '2011-08-06', 5, 7);
INSERT INTO pets VALUES (10, 'Mulligan', '2007-02-24', 2, 8);
INSERT INTO pets VALUES (11, 'Freddy', '2010-03-09', 5, 9);
INSERT INTO pets VALUES (12, 'Lucky', '2010-06-24', 2, 10);
INSERT INTO pets VALUES (13, 'Sly', '2012-06-08', 1, 10);
```

#### Entity Relationships

```
┌──────────────┐       ┌──────────────┐       ┌──────────────┐
│    owners    │       │     pets     │       │    types     │
├──────────────┤       ├──────────────┤       ├──────────────┤
│ id (PK)      │◄──────│ owner_id (FK)│       │ id (PK)      │
│ first_name   │  1:N  │ id (PK)      │  N:1  │ name         │
│ last_name    │       │ name         │──────►│              │
│ ...          │       │ birth_date   │       └──────────────┘
└──────────────┘       │ type_id (FK) │
                       └──────────────┘
                              │
                              │ 1:N
                              ▼
                       ┌──────────────┐
                       │    visits    │
                       ├──────────────┤
                       │ id (PK)      │
                       │ pet_id (FK)  │
                       │ visit_date   │
                       │ description  │
                       └──────────────┘
```

---

### Domain Model Layer

#### NamedEntity (Parent Class)

```java
// Location: src/main/java/org/springframework/samples/petclinic/model/NamedEntity.java

@MappedSuperclass
public class NamedEntity extends BaseEntity {

    @Column(name = "name")
    @NotEmpty
    private String name;

    public String getName() { return this.name; }
    public void setName(String name) { this.name = name; }
}
```

#### PetType Entity

```java
// Location: src/main/java/org/springframework/samples/petclinic/model/PetType.java

@Entity
@Table(name = "types")
public class PetType extends NamedEntity {
    // Inherits id and name from NamedEntity
}
```

#### Pet Entity

```java
// Location: src/main/java/org/springframework/samples/petclinic/model/Pet.java

@Entity
@Table(name = "pets")
public class Pet extends NamedEntity {

    @Column(name = "birth_date", columnDefinition = "DATE")
    private LocalDate birthDate;

    @ManyToOne
    @JoinColumn(name = "type_id")
    private PetType type;

    @ManyToOne
    @JoinColumn(name = "owner_id")
    private Owner owner;

    @OneToMany(cascade = CascadeType.ALL, mappedBy = "pet", fetch = FetchType.EAGER)
    private Set<Visit> visits;

    public void addVisit(Visit visit) {
        getVisitsInternal().add(visit);
        visit.setPet(this);
    }
}
```

**Inheritance Hierarchy:**
```
BaseEntity
    └── NamedEntity
        ├── Pet
        └── PetType
```

**Relationships:**
| Relationship | Type | Fetch | Cascade |
|--------------|------|-------|---------|
| Pet → Owner | ManyToOne | Default (LAZY) | None |
| Pet → PetType | ManyToOne | Default (LAZY) | None |
| Pet → Visits | OneToMany | EAGER | ALL |

---

### Repository Layer

#### PetRepository Interface

```java
// Location: src/main/java/org/springframework/samples/petclinic/repository/PetRepository.java

public interface PetRepository {
    
    /**
     * Retrieve all pet types
     */
    List<PetType> findPetTypes() throws DataAccessException;
    
    /**
     * Find pet by ID
     */
    Pet findById(int id) throws DataAccessException;
    
    /**
     * Save (insert or update) pet
     */
    void save(Pet pet) throws DataAccessException;
    
    /**
     * Get all pets
     */
    Collection<Pet> findAll() throws DataAccessException;
    
    /**
     * Delete pet
     */
    void delete(Pet pet) throws DataAccessException;
}
```

#### PetTypeRepository Interface

```java
// Location: src/main/java/org/springframework/samples/petclinic/repository/PetTypeRepository.java

public interface PetTypeRepository {
    PetType findById(int id) throws DataAccessException;
    PetType findByName(String name) throws DataAccessException;
    Collection<PetType> findAll() throws DataAccessException;
    void save(PetType petType) throws DataAccessException;
    void delete(PetType petType) throws DataAccessException;
}
```

---

### Service Layer

#### ClinicService Interface (Pet Methods)

```java
// Location: src/main/java/org/springframework/samples/petclinic/service/ClinicService.java

public interface ClinicService {
    Pet findPetById(int id) throws DataAccessException;
    Collection<Pet> findAllPets() throws DataAccessException;
    void savePet(Pet pet) throws DataAccessException;
    void deletePet(Pet pet) throws DataAccessException;
    
    PetType findPetTypeById(int petTypeId);
    Collection<PetType> findAllPetTypes() throws DataAccessException;
    Collection<PetType> findPetTypes() throws DataAccessException;
    void savePetType(PetType petType) throws DataAccessException;
    void deletePetType(PetType petType) throws DataAccessException;
    PetType findPetTypeByName(String name) throws DataAccessException;
}
```

#### ClinicServiceImpl (Pet Methods)

```java
// Location: src/main/java/org/springframework/samples/petclinic/service/ClinicServiceImpl.java

@Service
public class ClinicServiceImpl implements ClinicService {

    @Override
    @Transactional(readOnly = true)
    public Pet findPetById(int id) throws DataAccessException {
        Pet pet = null;
        try {
            pet = petRepository.findById(id);
        } catch (ObjectRetrievalFailureException | EmptyResultDataAccessException e) {
            return null;
        }
        return pet;
    }

    @Override
    @Transactional(readOnly = true)
    public Collection<Pet> findAllPets() throws DataAccessException {
        return petRepository.findAll();
    }

    @Override
    @Transactional
    public void savePet(Pet pet) throws DataAccessException {
        petRepository.save(pet);
    }

    @Override
    @Transactional
    public void deletePet(Pet pet) throws DataAccessException {
        petRepository.delete(pet);
    }
}
```

**Transaction Configuration:**
| Method | Transaction | Description |
|--------|-------------|-------------|
| `findPetById` | Read-only | Single pet lookup |
| `findAllPets` | Read-only | All pets retrieval |
| `savePet` | Read-write | Insert or update |
| `deletePet` | Read-write | Delete pet (cascades to visits) |

---

### Mapper Layer

#### PetMapper

```java
// Location: src/main/java/org/springframework/samples/petclinic/mapper/PetMapper.java

@Mapper
public interface PetMapper {

    @Mapping(source = "owner.id", target = "ownerId")
    PetDto toPetDto(Pet pet);

    Collection<PetDto> toPetsDto(Collection<Pet> pets);
    Collection<Pet> toPets(Collection<PetDto> pets);

    Pet toPet(PetDto petDto);
    Pet toPet(PetFieldsDto petFieldsDto);

    PetTypeDto toPetTypeDto(PetType petType);
    PetType toPetType(PetTypeDto petTypeDto);
    Collection<PetTypeDto> toPetTypeDtos(Collection<PetType> petTypes);
}
```

**Mapping Notes:**
- Owner ID is mapped from nested owner entity
- PetType mapping included for type conversion

---

### API Endpoints

#### PetRestController

```java
// Location: src/main/java/org/springframework/samples/petclinic/rest/controller/PetRestController.java

@RestController
@CrossOrigin(exposedHeaders = "errors, content-type")
@RequestMapping("api")
public class PetRestController implements PetsApi {
    // ... implementation
}
```

#### API Summary Table

| Method | Endpoint | Authorization | Description |
|--------|----------|---------------|-------------|
| GET | `/api/pets` | ROLE_OWNER_ADMIN | List all pets |
| GET | `/api/pets/{petId}` | ROLE_OWNER_ADMIN | Get pet by ID |
| POST | `/api/pets` | ROLE_OWNER_ADMIN | Create new pet |
| PUT | `/api/pets/{petId}` | ROLE_OWNER_ADMIN | Update pet |
| DELETE | `/api/pets/{petId}` | ROLE_OWNER_ADMIN | Delete pet |

#### Additional Endpoints (via OwnerRestController)

| Method | Endpoint | Authorization | Description |
|--------|----------|---------------|-------------|
| POST | `/api/owners/{ownerId}/pets` | ROLE_OWNER_ADMIN | Add pet to owner |
| GET | `/api/owners/{ownerId}/pets/{petId}` | ROLE_OWNER_ADMIN | Get owner's specific pet |

---

#### GET /api/pets - List Pets

```java
@PreAuthorize("hasRole(@roles.OWNER_ADMIN)")
@Override
public ResponseEntity<List<PetDto>> listPets() {
    List<PetDto> pets = new ArrayList<>(petMapper.toPetsDto(this.clinicService.findAllPets()));
    if (pets.isEmpty()) {
        return new ResponseEntity<>(HttpStatus.NOT_FOUND);
    }
    return new ResponseEntity<>(pets, HttpStatus.OK);
}
```

**Response (200 OK):**
```json
[
  {
    "id": 1,
    "name": "Leo",
    "birthDate": "2010-09-07",
    "type": { "id": 1, "name": "cat" },
    "ownerId": 1,
    "visits": []
  }
]
```

---

#### GET /api/pets/{petId} - Get Pet

```java
@PreAuthorize("hasRole(@roles.OWNER_ADMIN)")
@Override
public ResponseEntity<PetDto> getPet(Integer petId) {
    PetDto pet = petMapper.toPetDto(this.clinicService.findPetById(petId));
    if (pet == null) {
        return new ResponseEntity<>(HttpStatus.NOT_FOUND);
    }
    return new ResponseEntity<>(pet, HttpStatus.OK);
}
```

---

#### POST /api/pets - Create Pet

```java
@PreAuthorize("hasRole(@roles.OWNER_ADMIN)")
@Override
public ResponseEntity<PetDto> addPet(PetDto petDto) {
    this.clinicService.savePet(petMapper.toPet(petDto));
    return new ResponseEntity<>(petDto, HttpStatus.OK);
}
```

**Request Body:**
```json
{
  "name": "Buddy",
  "birthDate": "2020-05-15",
  "type": { "id": 2, "name": "dog" },
  "ownerId": 1
}
```

---

#### PUT /api/pets/{petId} - Update Pet

```java
@PreAuthorize("hasRole(@roles.OWNER_ADMIN)")
@Override
public ResponseEntity<PetDto> updatePet(Integer petId, PetDto petDto) {
    Pet currentPet = this.clinicService.findPetById(petId);
    if (currentPet == null) {
        return new ResponseEntity<>(HttpStatus.NOT_FOUND);
    }
    currentPet.setBirthDate(petDto.getBirthDate());
    currentPet.setName(petDto.getName());
    currentPet.setType(petMapper.toPetType(petDto.getType()));
    this.clinicService.savePet(currentPet);
    return new ResponseEntity<>(petMapper.toPetDto(currentPet), HttpStatus.NO_CONTENT);
}
```

---

#### DELETE /api/pets/{petId} - Delete Pet

```java
@PreAuthorize("hasRole(@roles.OWNER_ADMIN)")
@Override
public ResponseEntity<PetDto> deletePet(Integer petId) {
    Pet pet = this.clinicService.findPetById(petId);
    if (pet == null) {
        return new ResponseEntity<>(HttpStatus.NOT_FOUND);
    }
    this.clinicService.deletePet(pet);
    return new ResponseEntity<>(HttpStatus.NO_CONTENT);
}
```

**Note:** Delete cascades to associated visits.

---

### Frontend Implementation

#### Component Structure

```
client/src/components/pets/
├── NewPetPage.tsx           # Create new pet form
├── EditPetPage.tsx          # Edit existing pet form
├── PetEditor.tsx            # Reusable pet form component
├── createPetEditorModel.ts  # Pet editor data model factory
└── LoadingPanel.tsx         # Loading state component
```

#### TypeScript Interfaces

```typescript
// Location: client/src/types/index.ts

export interface IPetType extends INamedEntity {
}

export type IPetTypeId = number;

export interface IPet extends INamedEntity {
  birthDate: Date;
  type: IPetType;
  visits: IVisit[];
}

export interface IEditablePet extends INamedEntity {
  birthDate?: string;
  typeId?: IPetTypeId;
}

export interface IPetRequest {
  name: string;
  birthDate?: string;
  typeId: IPetTypeId;
}
```

---

#### PetEditor Component

```typescript
// Location: client/src/components/pets/PetEditor.tsx

export default class PetEditor extends React.Component<IPetEditorProps, IPetEditorState> {

  onSubmit(event) {
    const { owner } = this.props;
    const { editablePet } = this.state;

    const request: IPetRequest = {
      birthDate: editablePet.birthDate,
      name: editablePet.name,
      typeId: editablePet.typeId
    };

    const url = editablePet.isNew 
      ? '/api/owners/' + owner.id + '/pets' 
      : '/api/owners/' + owner.id + '/pets/' + editablePet.id;
      
    submitForm(editablePet.isNew ? 'POST' : 'PUT', url, request, (status, response) => {
      if (status === 204) {
        this.context.router.push({ pathname: '/owners/' + owner.id });
      } else {
        this.setState({ error: response });
      }
    });
  }
}
```

**Form Fields:**
| Field | Component | Description |
|-------|-----------|-------------|
| Owner | Display only | Shows owner name |
| Name | Input | Pet name |
| Birth date | DateInput | Date picker |
| Type | SelectInput | Pet type dropdown |

---

#### Frontend Routes

```typescript
// Location: client/src/configureRoutes.tsx

<Route path='/owners/:ownerId(\d+)/pets/new' component={NewPetPage} />
<Route path='/owners/:ownerId(\d+)/pets/:petId(\d+)/edit' component={EditPetPage} />
```

| Route | Component | Purpose |
|-------|-----------|---------|
| `/owners/:ownerId/pets/new` | NewPetPage | Create pet for owner |
| `/owners/:ownerId/pets/:petId/edit` | EditPetPage | Edit specific pet |

---

## Implementation Phases

### Phase 1: Database Layer - ✅ BASELINE COMPLETE

**Existing Components**:
1. ✅ `types` table for pet types
2. ✅ `pets` table with FK constraints
3. ✅ 6 pet types seeded
4. ✅ 13 sample pets seeded

**Files**:
- `src/main/resources/db/hsqldb/initDB.sql`
- `src/main/resources/db/hsqldb/populateDB.sql`

---

### Phase 2: Domain Model Layer - ✅ BASELINE COMPLETE

**Existing Components**:
1. ✅ NamedEntity with name field
2. ✅ PetType entity
3. ✅ Pet entity with all relationships
4. ✅ Visit collection relationship

**Files**:
- `src/main/java/.../model/NamedEntity.java`
- `src/main/java/.../model/PetType.java`
- `src/main/java/.../model/Pet.java`

---

### Phase 3: Repository Layer - ✅ BASELINE COMPLETE

**Existing Components**:
1. ✅ PetRepository interface
2. ✅ PetTypeRepository interface
3. ✅ Spring Data JPA implementations

**Files**:
- `src/main/java/.../repository/PetRepository.java`
- `src/main/java/.../repository/PetTypeRepository.java`

---

### Phase 4: Service Layer - ✅ BASELINE COMPLETE

**Existing Components**:
1. ✅ ClinicService interface with pet methods
2. ✅ ClinicServiceImpl with transaction management

**Files**:
- `src/main/java/.../service/ClinicService.java`
- `src/main/java/.../service/ClinicServiceImpl.java`

---

### Phase 5: API Layer - ✅ BASELINE COMPLETE

**Existing Components**:
1. ✅ PetRestController with CRUD operations
2. ✅ PetMapper for DTO conversion
3. ✅ @PreAuthorize security on all endpoints

**Files**:
- `src/main/java/.../rest/controller/PetRestController.java`
- `src/main/java/.../mapper/PetMapper.java`

---

### Phase 6: Frontend Implementation - ✅ BASELINE COMPLETE

**Existing Components**:
1. ✅ NewPetPage - create pet form
2. ✅ EditPetPage - edit pet form
3. ✅ PetEditor - reusable form component
4. ✅ Pet types dropdown
5. ✅ Date picker for birth date

**Files**:
- `client/src/components/pets/*.tsx`
- `client/src/types/index.ts`

---

## Success Criteria

### Backend (Baseline - Complete)
- [x] Types table exists with pet types
- [x] Pets table exists with proper schema
- [x] Pet entity maps to database
- [x] PetType entity maps to database
- [x] CRUD operations work via repository
- [x] Service layer provides transaction management
- [x] REST API exposes all pet operations
- [x] Role-based security on endpoints
- [x] Pet-owner relationship maintained
- [x] Pet-type relationship maintained

### Frontend (Baseline - Complete)
- [x] Create pet form renders
- [x] Edit pet form renders
- [x] Pet types load in dropdown
- [x] Date picker works
- [x] Form submission works
- [x] Navigation after save works

---

## Database Migration

> **📖 Reference**: See [ARCHITECTURE.md - Database Migration Strategy](../ARCHITECTURE.md#database-migration-strategy) for the complete migration documentation applicable to all domains.

**Key Points for Pets:**
- Reference data (`types` table) should be managed separately from transactional data
- FK constraints to `owners` and `types` must be maintained
- Potential future columns: weight, microchip_id, photo_url

---

## Dependencies

### Backend Dependencies
- Spring Data JPA
- MapStruct

### Frontend Dependencies
- React 15.x
- React Router
- TypeScript

---

## Future Enhancements

1. **Pet Search** - Search pets by name or type
2. **Pet Photos** - Upload and display pet images
3. **Medical History** - Track medications and conditions
4. **Weight Tracking** - Record weight over time
5. **Vaccination Records** - Track vaccination schedule

---

## Current Status

**Last Updated**: 2026-01-16
**Current Phase**: Baseline Documentation
**Status**: ✅ BASELINE COMPLETE
**Next Steps**: No immediate action required

---

## Notes for AI Agents

When updating this PRD:
1. Update phase status markers as work progresses
2. Add implementation details under each phase as code is written
3. Mark success criteria as complete when features work
4. Update "Current Status" section at the top after changes
5. Maintain metadata version and date on updates

