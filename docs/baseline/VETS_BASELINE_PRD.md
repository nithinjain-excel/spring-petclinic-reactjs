# Vets (Veterinarians) - Baseline PRD

| Version | Date | Author | Status |
|---------|------|--------|--------|
| 1.1 | 2026-01-17 | AI Assistant | Baseline |

> **Change Log:** v1.1 - Added reference to centralized Database Migration Strategy in ARCHITECTURE.md

---

## Overview

This document outlines the current baseline implementation of the Veterinarians (Vets) domain in the Spring PetClinic application. Vets are the medical professionals who provide care for pets at the clinic. Each vet can have multiple specialties (e.g., radiology, surgery, dentistry). The system provides full CRUD operations for vet management with a React-based frontend interface.

---

## Business Requirements

### Current State (Baseline)

#### Vet Management
- Vets can be created with first name, last name, and specialties
- Vets can have zero or more specialties
- Vets can be updated (name and specialties)
- Vets can be deleted from the system
- All vet operations require `ROLE_VET_ADMIN` authorization (when security enabled)

#### Specialty Management
- Specialties can be created independently
- Specialties can be assigned to multiple vets
- Available specialties: radiology, surgery, dentistry

#### Data Requirements
- First name and last name are required (inherited from Person)
- Specialties are optional (many-to-many relationship)

---

## Technical Requirements

### Database Schema

#### Vets Table

```sql
-- Location: src/main/resources/db/hsqldb/initDB.sql

CREATE TABLE vets (
  id         INTEGER IDENTITY PRIMARY KEY,
  first_name VARCHAR(30),
  last_name  VARCHAR(30)
);
CREATE INDEX vets_last_name ON vets (last_name);
```

| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| id | INTEGER | PK, AUTO | Unique vet identifier |
| first_name | VARCHAR(30) | - | Vet's first name |
| last_name | VARCHAR(30) | INDEXED | Vet's last name |

#### Specialties Table

```sql
-- Location: src/main/resources/db/hsqldb/initDB.sql

CREATE TABLE specialties (
  id   INTEGER IDENTITY PRIMARY KEY,
  name VARCHAR(80)
);
CREATE INDEX specialties_name ON specialties (name);
```

| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| id | INTEGER | PK, AUTO | Unique specialty identifier |
| name | VARCHAR(80) | INDEXED | Specialty name |

#### Vet_Specialties Junction Table

```sql
-- Location: src/main/resources/db/hsqldb/initDB.sql

CREATE TABLE vet_specialties (
  vet_id       INTEGER NOT NULL,
  specialty_id INTEGER NOT NULL
);

ALTER TABLE vet_specialties ADD CONSTRAINT fk_vet_specialties_vets 
  FOREIGN KEY (vet_id) REFERENCES vets (id);
ALTER TABLE vet_specialties ADD CONSTRAINT fk_vet_specialties_specialties 
  FOREIGN KEY (specialty_id) REFERENCES specialties (id);
```

| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| vet_id | INTEGER | FK → vets.id, NOT NULL | Vet reference |
| specialty_id | INTEGER | FK → specialties.id, NOT NULL | Specialty reference |

#### Sample Data

```sql
-- Location: src/main/resources/db/hsqldb/populateDB.sql

-- Vets
INSERT INTO vets VALUES (1, 'James', 'Carter');
INSERT INTO vets VALUES (2, 'Helen', 'Leary');
INSERT INTO vets VALUES (3, 'Linda', 'Douglas');
INSERT INTO vets VALUES (4, 'Rafael', 'Ortega');
INSERT INTO vets VALUES (5, 'Henry', 'Stevens');
INSERT INTO vets VALUES (6, 'Sharon', 'Jenkins');

-- Specialties
INSERT INTO specialties VALUES (1, 'radiology');
INSERT INTO specialties VALUES (2, 'surgery');
INSERT INTO specialties VALUES (3, 'dentistry');

-- Vet-Specialty Assignments
INSERT INTO vet_specialties VALUES (2, 1);  -- Helen Leary: radiology
INSERT INTO vet_specialties VALUES (3, 2);  -- Linda Douglas: surgery
INSERT INTO vet_specialties VALUES (3, 3);  -- Linda Douglas: dentistry
INSERT INTO vet_specialties VALUES (4, 2);  -- Rafael Ortega: surgery
INSERT INTO vet_specialties VALUES (5, 1);  -- Henry Stevens: radiology
-- Note: James Carter (1) and Sharon Jenkins (6) have no specialties
```

#### Entity Relationships

```
┌──────────────────┐         ┌────────────────────┐         ┌──────────────────┐
│      vets        │         │  vet_specialties   │         │   specialties    │
├──────────────────┤         ├────────────────────┤         ├──────────────────┤
│ id (PK)          │◄────────│ vet_id (FK)        │         │ id (PK)          │
│ first_name       │    1:N  │ specialty_id (FK)  │────────►│ name             │
│ last_name        │         └────────────────────┘    N:1  └──────────────────┘
└──────────────────┘
```

**Many-to-Many Relationship:**
- One Vet can have many Specialties
- One Specialty can belong to many Vets

---

### Domain Model Layer

#### Specialty Entity

```java
// Location: src/main/java/org/springframework/samples/petclinic/model/Specialty.java

@Entity
@Table(name = "specialties")
public class Specialty extends NamedEntity {
    // Inherits id and name from NamedEntity
}
```

#### Vet Entity

```java
// Location: src/main/java/org/springframework/samples/petclinic/model/Vet.java

@Entity
@Table(name = "vets")
public class Vet extends Person {

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(name = "vet_specialties", 
        joinColumns = @JoinColumn(name = "vet_id"),
        inverseJoinColumns = @JoinColumn(name = "specialty_id"))
    private Set<Specialty> specialties;

    @JsonIgnore
    protected Set<Specialty> getSpecialtiesInternal() {
        if (this.specialties == null) {
            this.specialties = new HashSet<>();
        }
        return this.specialties;
    }

    public List<Specialty> getSpecialties() {
        List<Specialty> sortedSpecs = new ArrayList<>(getSpecialtiesInternal());
        PropertyComparator.sort(sortedSpecs, new MutableSortDefinition("name", true, true));
        return Collections.unmodifiableList(sortedSpecs);
    }

    public void setSpecialties(List<Specialty> specialties) {
        this.specialties = new HashSet<>(specialties);
    }

    @JsonIgnore
    public int getNrOfSpecialties() {
        return getSpecialtiesInternal().size();
    }

    public void addSpecialty(Specialty specialty) {
        getSpecialtiesInternal().add(specialty);
    }

    public void clearSpecialties() {
        getSpecialtiesInternal().clear();
    }
}
```

**Inheritance Hierarchy:**
```
BaseEntity
    └── Person
        ├── Owner
        └── Vet
    └── NamedEntity
        └── Specialty
```

**Key Features:**
| Feature | Description |
|---------|-------------|
| EAGER fetch | Specialties loaded with vet |
| Sorted list | Specialties sorted by name |
| Helper methods | add, clear, count specialties |
| @JsonIgnore | Internal methods hidden from JSON |

---

### Repository Layer

#### VetRepository Interface

```java
// Location: src/main/java/org/springframework/samples/petclinic/repository/VetRepository.java

public interface VetRepository {
    
    /**
     * Retrieve all vets
     */
    Collection<Vet> findAll() throws DataAccessException;
    
    /**
     * Find vet by ID
     */
    Vet findById(int id) throws DataAccessException;
    
    /**
     * Save (insert or update) vet
     */
    void save(Vet vet) throws DataAccessException;
    
    /**
     * Delete vet
     */
    void delete(Vet vet) throws DataAccessException;
}
```

#### SpecialtyRepository Interface

```java
// Location: src/main/java/org/springframework/samples/petclinic/repository/SpecialtyRepository.java

public interface SpecialtyRepository {
    Specialty findById(int id) throws DataAccessException;
    Collection<Specialty> findAll() throws DataAccessException;
    void save(Specialty specialty) throws DataAccessException;
    void delete(Specialty specialty) throws DataAccessException;
    List<Specialty> findSpecialtiesByNameIn(Set<String> names) throws DataAccessException;
}
```

---

### Service Layer

#### ClinicService Interface (Vet Methods)

```java
// Location: src/main/java/org/springframework/samples/petclinic/service/ClinicService.java

public interface ClinicService {
    Vet findVetById(int id) throws DataAccessException;
    Collection<Vet> findVets() throws DataAccessException;
    Collection<Vet> findAllVets() throws DataAccessException;
    void saveVet(Vet vet) throws DataAccessException;
    void deleteVet(Vet vet) throws DataAccessException;
    
    Specialty findSpecialtyById(int specialtyId);
    Collection<Specialty> findAllSpecialties() throws DataAccessException;
    void saveSpecialty(Specialty specialty) throws DataAccessException;
    void deleteSpecialty(Specialty specialty) throws DataAccessException;
    List<Specialty> findSpecialtiesByNameIn(Set<String> names) throws DataAccessException;
}
```

#### ClinicServiceImpl (Vet Methods)

```java
// Location: src/main/java/org/springframework/samples/petclinic/service/ClinicServiceImpl.java

@Service
public class ClinicServiceImpl implements ClinicService {

    @Override
    @Transactional(readOnly = true)
    public Vet findVetById(int id) throws DataAccessException {
        Vet vet = null;
        try {
            vet = vetRepository.findById(id);
        } catch (ObjectRetrievalFailureException | EmptyResultDataAccessException e) {
            return null;
        }
        return vet;
    }

    @Override
    @Transactional(readOnly = true)
    public Collection<Vet> findAllVets() throws DataAccessException {
        return vetRepository.findAll();
    }

    @Override
    @Transactional
    public void saveVet(Vet vet) throws DataAccessException {
        vetRepository.save(vet);
    }

    @Override
    @Transactional
    public void deleteVet(Vet vet) throws DataAccessException {
        vetRepository.delete(vet);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Specialty> findSpecialtiesByNameIn(Set<String> names) {
        List<Specialty> specialties = new ArrayList<>();
        try {
            specialties = specialtyRepository.findSpecialtiesByNameIn(names);
        } catch (ObjectRetrievalFailureException | EmptyResultDataAccessException e) {
            return specialties;
        }
        return specialties;
    }
}
```

**Transaction Configuration:**
| Method | Transaction | Description |
|--------|-------------|-------------|
| `findVetById` | Read-only | Single vet lookup |
| `findAllVets` | Read-only | All vets retrieval |
| `saveVet` | Read-write | Insert or update |
| `deleteVet` | Read-write | Delete vet |
| `findSpecialtiesByNameIn` | Read-only | Find specialties by names |

---

### Mapper Layer

#### VetMapper

```java
// Location: src/main/java/org/springframework/samples/petclinic/mapper/VetMapper.java

@Mapper(uses = SpecialtyMapper.class)
public interface VetMapper {
    Vet toVet(VetDto vetDto);
    Vet toVet(VetFieldsDto vetFieldsDto);
    VetDto toVetDto(Vet vet);
    Collection<VetDto> toVetDtos(Collection<Vet> vets);
}
```

#### SpecialtyMapper

```java
// Location: src/main/java/org/springframework/samples/petclinic/mapper/SpecialtyMapper.java

@Mapper
public interface SpecialtyMapper {
    Specialty toSpecialty(SpecialtyDto specialtyDto);
    SpecialtyDto toSpecialtyDto(Specialty specialty);
    List<SpecialtyDto> toSpecialtyDtos(Collection<Specialty> specialties);
    List<Specialty> toSpecialtys(List<SpecialtyDto> specialties);
}
```

**MapStruct Notes:**
- VetMapper uses SpecialtyMapper for nested specialty conversion
- Bidirectional mapping between Entity and DTOs

---

### API Endpoints

#### VetRestController

```java
// Location: src/main/java/org/springframework/samples/petclinic/rest/controller/VetRestController.java

@RestController
@CrossOrigin(exposedHeaders = "errors, content-type")
@RequestMapping("api")
public class VetRestController implements VetsApi {
    // ... implementation
}
```

#### API Summary Table

| Method | Endpoint | Authorization | Description |
|--------|----------|---------------|-------------|
| GET | `/api/vets` | ROLE_VET_ADMIN | List all vets |
| GET | `/api/vets/{vetId}` | ROLE_VET_ADMIN | Get vet by ID |
| POST | `/api/vets` | ROLE_VET_ADMIN | Create new vet |
| PUT | `/api/vets/{vetId}` | ROLE_VET_ADMIN | Update vet |
| DELETE | `/api/vets/{vetId}` | ROLE_VET_ADMIN | Delete vet |

#### Specialty Endpoints

| Method | Endpoint | Authorization | Description |
|--------|----------|---------------|-------------|
| GET | `/api/specialties` | ROLE_VET_ADMIN | List all specialties |
| GET | `/api/specialties/{specialtyId}` | ROLE_VET_ADMIN | Get specialty by ID |
| POST | `/api/specialties` | ROLE_VET_ADMIN | Create specialty |
| PUT | `/api/specialties/{specialtyId}` | ROLE_VET_ADMIN | Update specialty |
| DELETE | `/api/specialties/{specialtyId}` | ROLE_VET_ADMIN | Delete specialty |

---

#### GET /api/vets - List Vets

```java
@PreAuthorize("hasRole(@roles.VET_ADMIN)")
@Override
public ResponseEntity<List<VetDto>> listVets() {
    List<VetDto> vets = new ArrayList<>();
    vets.addAll(vetMapper.toVetDtos(this.clinicService.findAllVets()));
    if (vets.isEmpty()) {
        return new ResponseEntity<>(HttpStatus.NOT_FOUND);
    }
    return new ResponseEntity<>(vets, HttpStatus.OK);
}
```

**Response (200 OK):**
```json
[
  {
    "id": 1,
    "firstName": "James",
    "lastName": "Carter",
    "specialties": []
  },
  {
    "id": 2,
    "firstName": "Helen",
    "lastName": "Leary",
    "specialties": [
      { "id": 1, "name": "radiology" }
    ]
  },
  {
    "id": 3,
    "firstName": "Linda",
    "lastName": "Douglas",
    "specialties": [
      { "id": 2, "name": "surgery" },
      { "id": 3, "name": "dentistry" }
    ]
  }
]
```

---

#### POST /api/vets - Create Vet

```java
@PreAuthorize("hasRole(@roles.VET_ADMIN)")
@Override
public ResponseEntity<VetDto> addVet(VetDto vetDto) {
    HttpHeaders headers = new HttpHeaders();
    Vet vet = vetMapper.toVet(vetDto);
    if(vet.getNrOfSpecialties() > 0){
        // Resolve specialties by name from database
        List<Specialty> vetSpecialities = this.clinicService.findSpecialtiesByNameIn(
            vet.getSpecialties().stream().map(Specialty::getName).collect(Collectors.toSet())
        );
        vet.setSpecialties(vetSpecialities);
    }
    this.clinicService.saveVet(vet);
    headers.setLocation(UriComponentsBuilder.newInstance()
        .path("/api/vets/{id}").buildAndExpand(vet.getId()).toUri());
    return new ResponseEntity<>(vetMapper.toVetDto(vet), headers, HttpStatus.CREATED);
}
```

**Request Body:**
```json
{
  "firstName": "John",
  "lastName": "Doe",
  "specialties": [
    { "name": "surgery" }
  ]
}
```

**Note:** Specialties are resolved by name, not ID. Existing specialties are looked up and linked.

---

#### PUT /api/vets/{vetId} - Update Vet

```java
@PreAuthorize("hasRole(@roles.VET_ADMIN)")
@Override
public ResponseEntity<VetDto> updateVet(Integer vetId, VetDto vetDto) {
    Vet currentVet = this.clinicService.findVetById(vetId);
    if (currentVet == null) {
        return new ResponseEntity<>(HttpStatus.NOT_FOUND);
    }
    currentVet.setFirstName(vetDto.getFirstName());
    currentVet.setLastName(vetDto.getLastName());
    currentVet.clearSpecialties();
    for (Specialty spec : specialtyMapper.toSpecialtys(vetDto.getSpecialties())) {
        currentVet.addSpecialty(spec);
    }
    if(currentVet.getNrOfSpecialties() > 0){
        List<Specialty> vetSpecialities = this.clinicService.findSpecialtiesByNameIn(
            currentVet.getSpecialties().stream().map(Specialty::getName).collect(Collectors.toSet())
        );
        currentVet.setSpecialties(vetSpecialities);
    }
    this.clinicService.saveVet(currentVet);
    return new ResponseEntity<>(vetMapper.toVetDto(currentVet), HttpStatus.NO_CONTENT);
}
```

**Note:** Specialties are cleared and re-added during update.

---

#### DELETE /api/vets/{vetId} - Delete Vet

```java
@PreAuthorize("hasRole(@roles.VET_ADMIN)")
@Transactional
@Override
public ResponseEntity<VetDto> deleteVet(Integer vetId) {
    Vet vet = this.clinicService.findVetById(vetId);
    if (vet == null) {
        return new ResponseEntity<>(HttpStatus.NOT_FOUND);
    }
    this.clinicService.deleteVet(vet);
    return new ResponseEntity<>(HttpStatus.NO_CONTENT);
}
```

---

### Frontend Implementation

#### Component Structure

```
client/src/components/vets/
└── VetsPage.tsx    # Display list of veterinarians
```

#### TypeScript Interfaces

```typescript
// Location: client/src/types/index.ts

export interface ISpecialty extends INamedEntity {
}

export interface IVet extends IPerson {
  specialties: ISpecialty[];
}
```

---

#### VetsPage Component

```typescript
// Location: client/src/components/vets/VetsPage.tsx

export default class VetsPage extends React.Component<void, IVetsPageState> {
  constructor() {
    super();
    this.state = { vets: [] };
  }

  componentDidMount() {
    const requestUrl = url('api/vets');

    fetch(requestUrl)
      .then(response => response.json())
      .then(vets => { this.setState({ vets }); });
  }

  render() {
    const { vets } = this.state;

    return (
      <span>
        <h2>Veterinarians</h2>
        <table className='table table-striped'>
          <thead>
            <tr>
              <th>Name</th>
              <th>Specialties</th>
            </tr>
          </thead>
          <tbody>
            {vets.map(vet => (
              <tr key={vet.id}>
                <td>{vet.firstName} {vet.lastName}</td>
                <td>{vet.specialties.length > 0 
                  ? vet.specialties.map(s => s.name).join(', ') 
                  : 'none'}</td>
              </tr>
            ))}
          </tbody>
        </table>
      </span>
    );
  }
}
```

**Features:**
- Read-only table display
- Shows vet name and specialties
- Comma-separated specialty list
- "none" displayed if no specialties

---

#### Frontend Routes

```typescript
// Location: client/src/configureRoutes.tsx

<Route path='/vets' component={VetsPage} />
```

| Route | Component | Purpose |
|-------|-----------|---------|
| `/vets` | VetsPage | Display vet list |

---

## Known Issues / Gaps

### Frontend Limitations

| Issue | Description | Impact |
|-------|-------------|--------|
| Read-only UI | No create/edit/delete for vets in frontend | Admin via API only |
| No specialty management | Cannot add/edit specialties via UI | Admin via API only |
| No vet details page | No individual vet profile view | Limited UX |

---

## Implementation Phases

### Phase 1: Database Layer - ✅ BASELINE COMPLETE

**Existing Components**:
1. ✅ `vets` table
2. ✅ `specialties` table
3. ✅ `vet_specialties` junction table
4. ✅ Foreign key constraints
5. ✅ 6 vets and 3 specialties seeded

**Files**:
- `src/main/resources/db/hsqldb/initDB.sql`
- `src/main/resources/db/hsqldb/populateDB.sql`

---

### Phase 2: Domain Model Layer - ✅ BASELINE COMPLETE

**Existing Components**:
1. ✅ Specialty entity
2. ✅ Vet entity with ManyToMany relationship
3. ✅ JoinTable configuration

**Files**:
- `src/main/java/.../model/Specialty.java`
- `src/main/java/.../model/Vet.java`

---

### Phase 3: Repository Layer - ✅ BASELINE COMPLETE

**Existing Components**:
1. ✅ VetRepository interface
2. ✅ SpecialtyRepository interface
3. ✅ Spring Data JPA implementations
4. ✅ findSpecialtiesByNameIn query

**Files**:
- `src/main/java/.../repository/VetRepository.java`
- `src/main/java/.../repository/SpecialtyRepository.java`

---

### Phase 4: Service Layer - ✅ BASELINE COMPLETE

**Existing Components**:
1. ✅ ClinicService interface with vet methods
2. ✅ ClinicServiceImpl with transaction management

**Files**:
- `src/main/java/.../service/ClinicService.java`
- `src/main/java/.../service/ClinicServiceImpl.java`

---

### Phase 5: API Layer - ✅ BASELINE COMPLETE

**Existing Components**:
1. ✅ VetRestController with CRUD operations
2. ✅ SpecialtyRestController with CRUD operations
3. ✅ VetMapper and SpecialtyMapper
4. ✅ @PreAuthorize security on all endpoints

**Files**:
- `src/main/java/.../rest/controller/VetRestController.java`
- `src/main/java/.../rest/controller/SpecialtyRestController.java`
- `src/main/java/.../mapper/VetMapper.java`
- `src/main/java/.../mapper/SpecialtyMapper.java`

---

### Phase 6: Frontend Implementation - ⚠️ PARTIAL

**Existing Components**:
1. ✅ VetsPage - read-only list display
2. ❌ No create vet form
3. ❌ No edit vet form
4. ❌ No delete vet function
5. ❌ No specialty management UI

**Files**:
- `client/src/components/vets/VetsPage.tsx`

---

## Success Criteria

### Backend (Baseline - Complete)
- [x] Vets table exists with proper schema
- [x] Specialties table exists
- [x] Junction table for many-to-many
- [x] Vet entity maps to database
- [x] Specialty entity maps to database
- [x] CRUD operations work via repository
- [x] Service layer provides transaction management
- [x] REST API exposes all vet operations
- [x] REST API exposes all specialty operations
- [x] Role-based security on endpoints
- [x] Vet-specialty relationship maintained

### Frontend (Partial)
- [x] Vets list page renders
- [x] Specialties displayed per vet
- [ ] Create vet form renders
- [ ] Edit vet form renders
- [ ] Delete vet function works
- [ ] Specialty management UI

---

## Database Migration

> **📖 Reference**: See [ARCHITECTURE.md - Database Migration Strategy](../ARCHITECTURE.md#database-migration-strategy) for the complete migration documentation applicable to all domains.

**Key Points for Vets:**
- Many-to-many relationship (`vet_specialties`) requires careful migration ordering
- Reference data (`specialties` table) should be managed separately
- Junction table changes need proper FK constraint handling
- Potential future columns: license_number, hire_date, working_hours

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

1. **Vet CRUD in Frontend** - Add create/edit/delete UI
2. **Specialty Management UI** - Add specialty CRUD
3. **Vet Schedule** - Appointment scheduling
4. **Vet Profile Page** - Individual vet details
5. **Search Vets by Specialty** - Filter vets by specialty
6. **Vet Availability** - Track working hours

---

## Current Status

**Last Updated**: 2026-01-16
**Current Phase**: Baseline Documentation
**Status**: ✅ BACKEND COMPLETE, ⚠️ FRONTEND PARTIAL
**Known Issues**: Frontend is read-only, no CRUD operations
**Next Steps**: Add frontend CRUD for vets (if required)

---

## Notes for AI Agents

When updating this PRD:
1. Update phase status markers as work progresses
2. Add implementation details under each phase as code is written
3. Mark success criteria as complete when features work
4. Update "Current Status" section at the top after changes
5. Maintain metadata version and date on updates

