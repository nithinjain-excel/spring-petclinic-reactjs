# Visits - Baseline PRD

| Version | Date | Author | Status |
|---------|------|--------|--------|
| 1.1 | 2026-01-17 | AI Assistant | Baseline |

> **Change Log:** v1.1 - Added reference to centralized Database Migration Strategy in ARCHITECTURE.md

---

## Overview

This document outlines the current baseline implementation of the Visits domain in the Spring PetClinic application. Visits represent appointments or check-ups where pets receive veterinary care. Each visit records the date and description of the service provided. The system provides full CRUD operations for visit management with a React-based frontend interface.

---

## Business Requirements

### Current State (Baseline)

#### Visit Management
- Visits can be created for a specific pet
- Visits record the date and description of the appointment
- Visits can be updated (date and description)
- Visits can be deleted from the system
- All visit operations require `ROLE_OWNER_ADMIN` authorization (when security enabled)

#### Data Requirements
- Visit date defaults to current date
- Description is required (medical notes, procedure description)
- Pet association is mandatory

#### Relationships
- Pet → Visit: One-to-Many (pet can have multiple visits)
- Visit → Pet: Many-to-One (each visit belongs to one pet)

---

## Technical Requirements

### Database Schema

#### Visits Table

```sql
-- Location: src/main/resources/db/hsqldb/initDB.sql

CREATE TABLE visits (
  id          INTEGER IDENTITY PRIMARY KEY,
  pet_id      INTEGER NOT NULL,
  visit_date  DATE,
  description VARCHAR(255)
);

ALTER TABLE visits ADD CONSTRAINT fk_visits_pets 
  FOREIGN KEY (pet_id) REFERENCES pets (id);
  
CREATE INDEX visits_pet_id ON visits (pet_id);
```

| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| id | INTEGER | PK, AUTO | Unique visit identifier |
| pet_id | INTEGER | FK → pets.id, NOT NULL, INDEXED | Pet reference |
| visit_date | DATE | - | Date of the visit |
| description | VARCHAR(255) | - | Visit description/notes |

#### Sample Data

```sql
-- Location: src/main/resources/db/hsqldb/populateDB.sql

INSERT INTO visits VALUES (1, 7, '2013-01-01', 'rabies shot');
INSERT INTO visits VALUES (2, 8, '2013-01-02', 'rabies shot');
INSERT INTO visits VALUES (3, 8, '2013-01-03', 'neutered');
INSERT INTO visits VALUES (4, 7, '2013-01-04', 'spayed');
```

#### Entity Relationships

```
┌──────────────────┐         ┌──────────────────┐
│      pets        │         │     visits       │
├──────────────────┤         ├──────────────────┤
│ id (PK)          │◄────────│ pet_id (FK)      │
│ name             │    1:N  │ id (PK)          │
│ birth_date       │         │ visit_date       │
│ type_id (FK)     │         │ description      │
│ owner_id (FK)    │         └──────────────────┘
└──────────────────┘
          │
          │ N:1
          ▼
┌──────────────────┐
│     owners       │
├──────────────────┤
│ id (PK)          │
│ first_name       │
│ last_name        │
│ ...              │
└──────────────────┘
```

**Relationship Chain:** Owner → Pets → Visits

---

### Domain Model Layer

#### Visit Entity

```java
// Location: src/main/java/org/springframework/samples/petclinic/model/Visit.java

@Entity
@Table(name = "visits")
public class Visit extends BaseEntity {

    @Column(name = "visit_date", columnDefinition = "DATE")
    private LocalDate date;

    @NotEmpty
    @Column(name = "description")
    private String description;

    @ManyToOne
    @JoinColumn(name = "pet_id")
    private Pet pet;

    /**
     * Creates a new instance of Visit for the current date
     */
    public Visit() {
        this.date = LocalDate.now();
    }

    // Getters and setters
    public LocalDate getDate() { return this.date; }
    public void setDate(LocalDate date) { this.date = date; }
    
    public String getDescription() { return this.description; }
    public void setDescription(String description) { this.description = description; }
    
    public Pet getPet() { return this.pet; }
    public void setPet(Pet pet) { this.pet = pet; }
}
```

**Inheritance Hierarchy:**
```
BaseEntity
    └── Visit
```

**Key Features:**
| Feature | Description |
|---------|-------------|
| Default date | Constructor sets date to `LocalDate.now()` |
| @NotEmpty | Description is required |
| LocalDate | Uses Java 8 date API |
| @ManyToOne | Links to Pet entity |

**Validation Annotations:**
| Field | Annotation | Description |
|-------|------------|-------------|
| description | `@NotEmpty` | Must not be empty |

---

### Repository Layer

#### VisitRepository Interface

```java
// Location: src/main/java/org/springframework/samples/petclinic/repository/VisitRepository.java

public interface VisitRepository {
    
    /**
     * Save (insert or update) visit
     */
    void save(Visit visit) throws DataAccessException;
    
    /**
     * Find visits by pet ID
     */
    List<Visit> findByPetId(Integer petId);
    
    /**
     * Find visit by ID
     */
    Visit findById(int id) throws DataAccessException;
    
    /**
     * Get all visits
     */
    Collection<Visit> findAll() throws DataAccessException;
    
    /**
     * Delete visit
     */
    void delete(Visit visit) throws DataAccessException;
}
```

**Query Methods:**
| Method | Query Type | Description |
|--------|------------|-------------|
| `findByPetId` | Spring Data | Find all visits for a specific pet |
| `findById` | Spring Data | Find single visit by ID |
| `findAll` | Spring Data | Retrieve all visits |

---

### Service Layer

#### ClinicService Interface (Visit Methods)

```java
// Location: src/main/java/org/springframework/samples/petclinic/service/ClinicService.java

public interface ClinicService {
    Collection<Visit> findVisitsByPetId(int petId);
    Visit findVisitById(int visitId) throws DataAccessException;
    Collection<Visit> findAllVisits() throws DataAccessException;
    void saveVisit(Visit visit) throws DataAccessException;
    void deleteVisit(Visit visit) throws DataAccessException;
}
```

#### ClinicServiceImpl (Visit Methods)

```java
// Location: src/main/java/org/springframework/samples/petclinic/service/ClinicServiceImpl.java

@Service
public class ClinicServiceImpl implements ClinicService {

    @Override
    @Transactional(readOnly = true)
    public Visit findVisitById(int visitId) throws DataAccessException {
        Visit visit = null;
        try {
            visit = visitRepository.findById(visitId);
        } catch (ObjectRetrievalFailureException | EmptyResultDataAccessException e) {
            return null;
        }
        return visit;
    }

    @Override
    @Transactional(readOnly = true)
    public Collection<Visit> findAllVisits() throws DataAccessException {
        return visitRepository.findAll();
    }

    @Override
    @Transactional
    public void saveVisit(Visit visit) throws DataAccessException {
        visitRepository.save(visit);
    }

    @Override
    @Transactional
    public void deleteVisit(Visit visit) throws DataAccessException {
        visitRepository.delete(visit);
    }

    @Override
    @Transactional(readOnly = true)
    public Collection<Visit> findVisitsByPetId(int petId) {
        return visitRepository.findByPetId(petId);
    }
}
```

**Transaction Configuration:**
| Method | Transaction | Description |
|--------|-------------|-------------|
| `findVisitById` | Read-only | Single visit lookup |
| `findAllVisits` | Read-only | All visits retrieval |
| `saveVisit` | Read-write | Insert or update |
| `deleteVisit` | Read-write | Delete visit |
| `findVisitsByPetId` | Read-only | Visits for specific pet |

---

### Mapper Layer

#### VisitMapper

```java
// Location: src/main/java/org/springframework/samples/petclinic/mapper/VisitMapper.java

@Mapper(uses = PetMapper.class)
public interface VisitMapper {
    
    Visit toVisit(VisitDto visitDto);
    
    Visit toVisit(VisitFieldsDto visitFieldsDto);
    
    @Mapping(source = "pet.id", target = "petId")
    VisitDto toVisitDto(Visit visit);
    
    Collection<VisitDto> toVisitsDto(Collection<Visit> visits);
}
```

**Mapping Notes:**
- Pet ID extracted from nested pet entity
- Uses PetMapper for pet-related conversions
- VisitFieldsDto used for create/update requests

---

### API Endpoints

#### VisitRestController

```java
// Location: src/main/java/org/springframework/samples/petclinic/rest/controller/VisitRestController.java

@RestController
@CrossOrigin(exposedHeaders = "errors, content-type")
@RequestMapping("api")
public class VisitRestController implements VisitsApi {
    // ... implementation
}
```

#### API Summary Table

| Method | Endpoint | Authorization | Description |
|--------|----------|---------------|-------------|
| GET | `/api/visits` | ROLE_OWNER_ADMIN | List all visits |
| GET | `/api/visits/{visitId}` | ROLE_OWNER_ADMIN | Get visit by ID |
| POST | `/api/visits` | ROLE_OWNER_ADMIN | Create new visit |
| PUT | `/api/visits/{visitId}` | ROLE_OWNER_ADMIN | Update visit |
| DELETE | `/api/visits/{visitId}` | ROLE_OWNER_ADMIN | Delete visit |

#### Additional Endpoint (via OwnerRestController)

| Method | Endpoint | Authorization | Description |
|--------|----------|---------------|-------------|
| POST | `/api/owners/{ownerId}/pets/{petId}/visits` | ROLE_OWNER_ADMIN | Add visit for owner's pet |

---

#### GET /api/visits - List Visits

```java
@PreAuthorize("hasRole(@roles.OWNER_ADMIN)")
@Override
public ResponseEntity<List<VisitDto>> listVisits() {
    List<Visit> visits = new ArrayList<>(this.clinicService.findAllVisits());
    if (visits.isEmpty()) {
        return new ResponseEntity<>(HttpStatus.NOT_FOUND);
    }
    return new ResponseEntity<>(new ArrayList<>(visitMapper.toVisitsDto(visits)), HttpStatus.OK);
}
```

**Response (200 OK):**
```json
[
  {
    "id": 1,
    "date": "2013-01-01",
    "description": "rabies shot",
    "petId": 7
  },
  {
    "id": 2,
    "date": "2013-01-02",
    "description": "rabies shot",
    "petId": 8
  }
]
```

---

#### GET /api/visits/{visitId} - Get Visit

```java
@PreAuthorize("hasRole(@roles.OWNER_ADMIN)")
@Override
public ResponseEntity<VisitDto> getVisit(Integer visitId) {
    Visit visit = this.clinicService.findVisitById(visitId);
    if (visit == null) {
        return new ResponseEntity<>(HttpStatus.NOT_FOUND);
    }
    return new ResponseEntity<>(visitMapper.toVisitDto(visit), HttpStatus.OK);
}
```

---

#### POST /api/visits - Create Visit

```java
@PreAuthorize("hasRole(@roles.OWNER_ADMIN)")
@Override
public ResponseEntity<VisitDto> addVisit(VisitDto visitDto) {
    HttpHeaders headers = new HttpHeaders();
    Visit visit = visitMapper.toVisit(visitDto);
    this.clinicService.saveVisit(visit);
    visitDto = visitMapper.toVisitDto(visit);
    headers.setLocation(UriComponentsBuilder.newInstance()
        .path("/api/visits/{id}").buildAndExpand(visit.getId()).toUri());
    return new ResponseEntity<>(visitDto, headers, HttpStatus.CREATED);
}
```

**Request Body:**
```json
{
  "date": "2026-01-16",
  "description": "Annual checkup",
  "petId": 1
}
```

---

#### PUT /api/visits/{visitId} - Update Visit

```java
@PreAuthorize("hasRole(@roles.OWNER_ADMIN)")
@Override
public ResponseEntity<VisitDto> updateVisit(Integer visitId, VisitDto visitDto) {
    Visit currentVisit = this.clinicService.findVisitById(visitId);
    if (currentVisit == null) {
        return new ResponseEntity<>(HttpStatus.NOT_FOUND);
    }
    currentVisit.setDate(visitDto.getDate());
    currentVisit.setDescription(visitDto.getDescription());
    this.clinicService.saveVisit(currentVisit);
    return new ResponseEntity<>(visitMapper.toVisitDto(currentVisit), HttpStatus.NO_CONTENT);
}
```

**Note:** Only date and description can be updated. Pet association cannot be changed.

---

#### DELETE /api/visits/{visitId} - Delete Visit

```java
@PreAuthorize("hasRole(@roles.OWNER_ADMIN)")
@Transactional
@Override
public ResponseEntity<VisitDto> deleteVisit(Integer visitId) {
    Visit visit = this.clinicService.findVisitById(visitId);
    if (visit == null) {
        return new ResponseEntity<>(HttpStatus.NOT_FOUND);
    }
    this.clinicService.deleteVisit(visit);
    return new ResponseEntity<>(HttpStatus.NO_CONTENT);
}
```

---

#### POST /api/owners/{ownerId}/pets/{petId}/visits - Add Visit (Nested)

```java
// Location: src/main/java/.../rest/controller/OwnerRestController.java

@PreAuthorize("hasRole(@roles.OWNER_ADMIN)")
@Override
public ResponseEntity<VisitDto> addVisitToOwner(Integer ownerId, Integer petId, VisitFieldsDto visitFieldsDto) {
    HttpHeaders headers = new HttpHeaders();
    Visit visit = visitMapper.toVisit(visitFieldsDto);
    Pet pet = new Pet();
    pet.setId(petId);
    visit.setPet(pet);
    this.clinicService.saveVisit(visit);
    VisitDto visitDto = visitMapper.toVisitDto(visit);
    headers.setLocation(UriComponentsBuilder.newInstance()
        .path("/api/visits/{id}").buildAndExpand(visit.getId()).toUri());
    return new ResponseEntity<>(visitDto, headers, HttpStatus.CREATED);
}
```

**Request Body (VisitFieldsDto):**
```json
{
  "date": "2026-01-16",
  "description": "Vaccination"
}
```

**Note:** Pet is associated via URL path parameter, not request body.

---

### Frontend Implementation

#### Component Structure

```
client/src/components/visits/
├── VisitsPage.tsx    # Add new visit form
└── PetDetails.tsx    # Pet info display component
```

#### TypeScript Interfaces

```typescript
// Location: client/src/types/index.ts

export interface IVisit extends IBaseEntity {
  date: Date;
  description: string;
}
```

---

#### VisitsPage Component

```typescript
// Location: client/src/components/visits/VisitsPage.tsx

interface IVisitsPageProps {
  params: {
    ownerId: string,
    petId: string
  };
}

interface IVisitsPageState {
  visit?: IVisit;
  owner?: IOwner;
  error?: IError;
}

export default class VisitsPage extends React.Component<IVisitsPageProps, IVisitsPageState> {

  componentDidMount() {
    const { params } = this.props;

    if (params && params.ownerId) {
      fetch(url(`/api/owner/${params.ownerId}`))
        .then(response => response.json())
        .then(owner => this.setState({
          owner: owner,
          visit: { id: null, isNew: true, date: null, description: '' }
        }));
    }
  }

  onSubmit(event) {
    event.preventDefault();

    const petId = this.props.params.petId;
    const { owner, visit } = this.state;

    const request = {
      date: visit.date,
      description: visit.description
    };

    const url = '/api/owners/' + owner.id + '/pets/' + petId + '/visits';
    submitForm('POST', url, request, (status, response) => {
      if (status === 204) {
        this.context.router.push({ pathname: '/owners/' + owner.id });
      } else {
        this.setState({ error: response });
      }
    });
  }

  render() {
    const { owner, error, visit } = this.state;
    const petId = this.props.params.petId;
    const pet = owner.pets.find(candidate => candidate.id.toString() === petId);

    return (
      <div>
        <h2>Visits</h2>
        <PetDetails owner={owner} pet={pet} />
        <form>
          <DateInput object={visit} label='Date' name='date' onChange={this.onInputChange} />
          <Input object={visit} constraint={NotEmpty} label='Description' name='description' onChange={this.onInputChange} />
          <button onClick={this.onSubmit}>Add Visit</button>
        </form>
      </div>
    );
  }
}
```

**Features:**
- Shows pet details before form
- Date picker for visit date
- Description text input (required)
- Redirects to owner page after save

**Known Bug:** Uses `/api/owner/` instead of `/api/owners/` to fetch owner data.

---

#### PetDetails Component

```typescript
// Location: client/src/components/visits/PetDetails.tsx

// Displays pet information:
// - Pet name
// - Birth date
// - Type
// - Owner name
```

---

#### Frontend Routes

```typescript
// Location: client/src/configureRoutes.tsx

<Route path='/owners/:ownerId(\d+)/pets/:petId(\d+)/visits' component={VisitsPage} />
```

| Route | Component | Purpose |
|-------|-----------|---------|
| `/owners/:ownerId/pets/:petId/visits` | VisitsPage | Add visit for pet |

---

## Known Issues / Gaps

### Frontend Limitations

| Issue | Description | Impact |
|-------|-------------|--------|
| API URL bug | Uses `/api/owner/` instead of `/api/owners/` | May cause 404 errors |
| No visit list | Cannot view all visits for a pet | Limited visibility |
| No edit visit | Cannot edit existing visits via UI | Admin via API only |
| No delete visit | Cannot delete visits via UI | Admin via API only |
| Create only | Frontend only supports adding new visits | Limited functionality |

---

## Implementation Phases

### Phase 1: Database Layer - ✅ BASELINE COMPLETE

**Existing Components**:
1. ✅ `visits` table with proper schema
2. ✅ Foreign key to pets table
3. ✅ Index on pet_id for query performance
4. ✅ 4 sample visits seeded

**Files**:
- `src/main/resources/db/hsqldb/initDB.sql`
- `src/main/resources/db/hsqldb/populateDB.sql`

---

### Phase 2: Domain Model Layer - ✅ BASELINE COMPLETE

**Existing Components**:
1. ✅ Visit entity with JPA annotations
2. ✅ LocalDate for visit date
3. ✅ ManyToOne relationship to Pet
4. ✅ Default constructor sets current date

**Files**:
- `src/main/java/.../model/Visit.java`

---

### Phase 3: Repository Layer - ✅ BASELINE COMPLETE

**Existing Components**:
1. ✅ VisitRepository interface
2. ✅ Spring Data JPA implementation
3. ✅ findByPetId query

**Files**:
- `src/main/java/.../repository/VisitRepository.java`

---

### Phase 4: Service Layer - ✅ BASELINE COMPLETE

**Existing Components**:
1. ✅ ClinicService interface with visit methods
2. ✅ ClinicServiceImpl with transaction management

**Files**:
- `src/main/java/.../service/ClinicService.java`
- `src/main/java/.../service/ClinicServiceImpl.java`

---

### Phase 5: API Layer - ✅ BASELINE COMPLETE

**Existing Components**:
1. ✅ VisitRestController with CRUD operations
2. ✅ VisitMapper for DTO conversion
3. ✅ @PreAuthorize security on all endpoints
4. ✅ Nested visit creation via OwnerRestController

**Files**:
- `src/main/java/.../rest/controller/VisitRestController.java`
- `src/main/java/.../rest/controller/OwnerRestController.java`
- `src/main/java/.../mapper/VisitMapper.java`

---

### Phase 6: Frontend Implementation - ⚠️ PARTIAL

**Existing Components**:
1. ✅ VisitsPage - create visit form
2. ✅ PetDetails - pet info display
3. ✅ Date input component
4. ❌ No visit list view
5. ❌ No edit visit functionality
6. ❌ No delete visit functionality

**Files**:
- `client/src/components/visits/VisitsPage.tsx`
- `client/src/components/visits/PetDetails.tsx`

---

## Success Criteria

### Backend (Baseline - Complete)
- [x] Visits table exists with proper schema
- [x] Visit entity maps to database
- [x] CRUD operations work via repository
- [x] Service layer provides transaction management
- [x] REST API exposes all visit operations
- [x] Nested visit creation works
- [x] Role-based security on endpoints
- [x] Visit-pet relationship maintained
- [x] findByPetId query works

### Frontend (Partial)
- [x] Add visit form renders
- [x] Pet details displayed
- [x] Date picker works
- [x] Form submission works
- [ ] Visit list view exists
- [ ] Edit visit form renders
- [ ] Delete visit function works

---

## Database Migration

> **📖 Reference**: See [ARCHITECTURE.md - Database Migration Strategy](../ARCHITECTURE.md#database-migration-strategy) for the complete migration documentation applicable to all domains.

**Key Points for Visits:**
- Historical transactional data - visit records should be immutable
- Consider audit columns: created_at, updated_at, created_by
- Potential future columns: vet_id (vet assignment), cost (billing)
- May need soft delete (`deleted_at`) for compliance
- Large datasets may benefit from time-based partitioning

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

1. **Visit History View** - Display all visits for a pet
2. **Edit/Delete in Frontend** - Full CRUD in UI
3. **Visit Reminders** - Upcoming appointment notifications
4. **Vet Assignment** - Link visits to specific vets
5. **Visit Notes Attachments** - Upload documents/images
6. **Cost Tracking** - Track visit costs and billing
7. **Follow-up Scheduling** - Schedule follow-up appointments

---

## Current Status

**Last Updated**: 2026-01-16
**Current Phase**: Baseline Documentation
**Status**: ✅ BACKEND COMPLETE, ⚠️ FRONTEND PARTIAL
**Known Issues**: Frontend API URL bug, limited to create-only
**Next Steps**: Add frontend visit list and edit/delete (if required)

---

## Notes for AI Agents

When updating this PRD:
1. Update phase status markers as work progresses
2. Add implementation details under each phase as code is written
3. Mark success criteria as complete when features work
4. Update "Current Status" section at the top after changes
5. Maintain metadata version and date on updates

