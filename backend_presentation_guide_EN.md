# Backend Presentation Guide — SmartMail Pro
### Jury Presentation Script — Prof. JIBRAILI MALAK
> **Usage**: This is your personal script. Sections in *italics* are your speaking points. Code blocks are meant to be displayed on screen during the demo.

---

## 1. The Architectural Pitch (Opening Speech — 2 minutes)

> *Script to deliver while opening IntelliJ IDEA on the project structure:*

---

**"Our backend is a REST API built on Spring Boot 3 — the most widely adopted Java framework for enterprise and SaaS architectures. We chose this stack for three specific reasons:**

**First, robustness.** Spring Boot natively bundles an embedded Tomcat server, automatic SQL transaction management, and a dependency injection system — allowing us to focus on business logic rather than infrastructure plumbing.

**Second, productivity.** Thanks to Spring Data JPA, we don't write a single line of SQL for standard operations. The `JpaRepository` interface automatically generates `SELECT`, `INSERT`, `UPDATE`, and `DELETE` queries at runtime, derived directly from Java method names.

**Third, scalability.** Our architecture strictly follows the **3-Tier pattern** — Controller / Service / Repository — meaning each layer is independent, separately testable, and replaceable without impacting the others.

Our database is **MySQL 8**, with **8 JPA entities** mapped to **9 tables** (plus one join table). Communication with the Next.js frontend happens exclusively over HTTP REST, authenticated via the `X-User-Email` header."**

---

## 2. 3-Tier Architecture — Real Code Examples

> *Show the package structure in IntelliJ: `controller/`, `service/`, `repository/`*

```
com.example.backend/
├── controller/    ← Layer 1: HTTP Entry Points
├── service/       ← Layer 2: Business Logic
├── repository/    ← Layer 3: Data Access (JPA)
└── entity/        ← Data Model (ORM Mapping)
```

---

### 2.1 Layer 1 — Controllers (HTTP Entry Points)

> *Explain while showing `CampaignController.java`:*

**"The controller is the API's front door. It contains zero business logic — its only role is to receive the HTTP request, extract the parameters, and delegate to the service layer."**

```java
// CampaignController.java — Entry point for "Launch a Campaign"
@RestController                          // Spring identifies this class as an HTTP REST handler
@RequestMapping("/api/campaigns")        // Prefix for all routes in this controller
@CrossOrigin(origins = "http://localhost:3000") // Only our Next.js frontend is allowed

public class CampaignController {

    @Autowired private CampaignRepository campaignRepository;
    @Autowired private EnterpriseEmailService emailService;
    // ...

    @PostMapping("/launch")
    public ResponseEntity<?> launchCampaign(
            @RequestHeader("X-User-Email") String userEmail,  // Identifies the user
            @RequestBody Map<String, Object> payload) {       // JSON request body
        // ...
    }
}
```

> **Key points to highlight to the professor:**
> - `@RestController` — Spring auto-detects this class as an HTTP route handler
> - `@RequestHeader("X-User-Email")` — our multi-tenant isolation mechanism: **every request carries the user's identity**
> - `@RequestBody Map<String, Object>` — Spring automatically deserializes the JSON into a Java object
> - `ResponseEntity<?>` — we precisely control the HTTP status code returned (200, 400, 403...)

---

### 2.2 Layer 2 — Services (Business Logic)

> *Show `SegmentService.java` or `EnterpriseEmailService.java`*

**"The service layer isolates business logic from HTTP details. When `SegmentController` receives a segment creation request, it immediately delegates to `SegmentService.saveSegment()`. The controller has no idea how subscribers are filtered — that's the service's job."**

```java
// SegmentController.java — The controller simply delegates to the service
@PostMapping
public ResponseEntity<Segment> createSegment(
        @RequestBody Segment segment,
        @RequestHeader("X-User-Email") String userEmail) {

    // The controller ONLY delegates — zero logic here
    return ResponseEntity.ok(segmentService.saveSegment(segment, userEmail));
}
```

> **Why this separation?**
> - **Testability**: We can unit-test `SegmentService` with JUnit without starting an HTTP server
> - **Reusability**: `EnterpriseEmailService` is called by `CampaignController` AND by the scheduled campaign dispatcher
> - **Clarity**: A new developer immediately understands the role of each class

---

### 2.3 Layer 3 — Repositories (JPA Data Access)

> *Show `EmailInteractionRepository.java` — this is our most impressive example*

**"Here is the power of Spring Data JPA. This interface has no body — no implementation at all. Spring generates the SQL automatically at runtime, inferring queries directly from Java method names."**

```java
@Repository
public interface EmailInteractionRepository extends JpaRepository<EmailInteraction, Long> {

    // Spring generates: SELECT COUNT(*) FROM email_interactions
    //                   WHERE campaign_id=? AND subscriber_id=? AND interaction_type=?
    boolean existsByCampaignIdAndSubscriberIdAndInteractionType(
        Long campaignId, Long subscriberId, String interactionType);

    // Spring generates: SELECT COUNT(*) FROM email_interactions
    //                   WHERE campaign_id=? AND interaction_type=?
    long countByCampaignIdAndInteractionType(Long campaignId, String interactionType);

    // Spring generates: DELETE FROM email_interactions WHERE campaign_id=?
    void deleteByCampaignId(Long campaignId);

    // Custom JPQL query for advanced analytics
    @Query("SELECT e.subscriberId, COUNT(e) FROM EmailInteraction e " +
           "GROUP BY e.subscriberId ORDER BY COUNT(e) DESC")
    List<Object[]> findTopSubscriberIdsWithCount(Pageable pageable);
}
```

> **Key points to explain:**
> - The first 3 methods: **zero SQL written** — Spring reads the method name and generates the query
> - `findTopSubscriberIdsWithCount`: a custom **JPQL** query (Java Persistence Query Language) — object-oriented syntax operating on **Java entities**, not raw SQL tables
> - `Pageable pageable`: built-in pagination — `PageRequest.of(0, 5)` returns the top 5 results
> - **This query powers the "Top VIP Subscribers" widget on the Dashboard**

---

## 3. Database Modeling (ORM & Entities)

> *Open the MySQL database schema in MySQL Workbench or IntelliJ DataGrip*

**"Our 8 Java entities are mapped directly to 9 MySQL tables. Hibernate, the JPA engine, automatically generates and maintains the schema via `spring.jpa.hibernate.ddl-auto=update`."**

### 3.1 Entities and their Tables

| Java Entity | MySQL Table | Key Columns |
|-------------|-------------|-------------|
| `User` | `users` | `id`, `email` (UNIQUE), `created_at` |
| `Subscriber` | `subscribers` | `id`, `email`, `first_name`, `last_name`, `status`, `custom_attributes` (JSON), `user_id` (FK) |
| `Segment` | `segments` | `id`, `name`, `rules` (TEXT), `by_ai`, `user_id` (FK) |
| `Template` | `templates` | `id`, `name`, `html_content` (TEXT), `user_id` (FK) |
| `Campaign` | `campaigns` | `id`, `name`, `subject`, `user_email`, `status`, `segment_id` (FK), `template_id` (FK), `scheduled_at`, `sent_at` |
| `EmailInteraction` | `email_interactions` | `id`, `campaign_id`, `subscriber_id`, `interaction_type`, `target_url`, `timestamp` |
| `Media` | `media` | `id`, `file_name`, `file_url`, `file_type`, `user_id` (FK) |
| `Vault` | `vault` | `id`, `gemini_api_key_encrypted`, `gmail_oauth_token_encrypted`, `user_id` (FK, UNIQUE) |
| *(JPA join table)* | `segment_subscribers` | `segment_id` (FK), `subscriber_id` (FK) |

### 3.2 JPA Relationships — How Java Describes MySQL

**"We never write `CREATE TABLE` or `ALTER TABLE ADD FOREIGN KEY`. We declare relationships in Java using annotations, and Hibernate generates the corresponding SQL."**

```java
// ManyToOne: A Subscriber belongs to exactly one User
// Hibernate automatically creates the user_id FK column in the subscribers table
@ManyToOne(fetch = FetchType.LAZY)
@JoinColumn(name = "user_id", nullable = false)
@JsonIgnore   // Prevents infinite JSON serialization loops (User→Subscriber→User)
private User user;

// ManyToMany: A Segment contains many Subscribers, and vice-versa
// Hibernate automatically creates the segment_subscribers join table
@ManyToMany(fetch = FetchType.LAZY)
@JoinTable(
    name = "segment_subscribers",
    joinColumns = @JoinColumn(name = "segment_id"),
    inverseJoinColumns = @JoinColumn(name = "subscriber_id")
)
private Set<Subscriber> subscribers = new HashSet<>();

// OneToOne: A User has exactly one Vault (API key safe)
@OneToOne(fetch = FetchType.LAZY)
@JoinColumn(name = "user_id", unique = true)
private User user;
```

### 3.3 Focus: Campaign ↔ EmailInteraction

> *Explain the link between these two entities — it is the core of the tracking system*

**"The relationship between `Campaign` and `EmailInteraction` is intentionally loose. `EmailInteraction` stores just the numeric IDs (`campaign_id`, `subscriber_id`) without a declared JPA relationship. Why? Because interactions are created by `TrackingController`, which never needs to load the full `Campaign` object — it only writes a minimal record, optimized for high-frequency writes."**

```java
// EmailInteraction.java — Lightweight structure, optimized for high-frequency writes
@Entity
@Table(name = "email_interactions")
public class EmailInteraction {
    @Column(name = "campaign_id", nullable = false)
    private Long campaignId;          // Just the ID — no @ManyToOne to avoid unnecessary JPA loading

    @Column(name = "interaction_type", nullable = false)
    private String interactionType;   // "OPEN" or "CLICK"

    @Column(name = "target_url")
    private String targetUrl;         // Null for opens, populated for clicks
}
```

---

## 4. Data Flow Demonstration — Case Study: Deleting a Campaign

> *This is the most technically complete demo. Show the code on screen and walk through each step.*

### The Problem We Solved

**"When we first tried to delete a campaign, MySQL threw an error: `foreign key constraint fails`. The `email_interactions` table contains records referencing `campaign_id`. MySQL refuses to delete the parent while children exist."**

**"Here is how we solved this cleanly, using two advanced Spring concepts: `@Transactional` and manual cascade deletion."**

### Complete Flow — 7 Steps

```
Next.js Frontend                    Spring Boot Backend                    MySQL
────────────────────────────────────────────────────────────────────────────────
1. DELETE /api/campaigns/42    ──►  CampaignController.deleteCampaign()
   Header: X-User-Email: a@b.c          │
                                    2. campaignRepository.findById(42)  ──►  SELECT * FROM campaigns WHERE id=42
                                        │  ◄── Campaign object
                                    3. campaign.getUserEmail() == "a@b.c" ?
                                        │  NO  → return 403 FORBIDDEN
                                        │  YES → continue
                                    4. @Transactional starts an SQL transaction
                                        │
                                    5. interactionRepository              ──►  DELETE FROM email_interactions
                                          .deleteByCampaignId(42)                WHERE campaign_id=42
                                        │
                                    6. campaignRepository.delete(campaign) ──►  DELETE FROM campaigns
                                        │                                         WHERE id=42
                                    7. @Transactional COMMIT              ──►  Both DELETEs committed atomically
                                        │
                                        └── return 200 OK {"message": "Campaign deleted successfully"}
```

### Code to Display on Screen

```java
// CampaignController.java — Full delete method
@org.springframework.transaction.annotation.Transactional   // ← Guarantees atomicity of both DELETEs
@DeleteMapping("/{id}")
public ResponseEntity<?> deleteCampaign(
        @PathVariable Long id,
        @RequestHeader("X-User-Email") String userEmail) {   // ← Caller's identity
    try {
        // Step 1: Verify the campaign exists
        Campaign campaign = campaignRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Campaign not found in database."));

        // Step 2: Verify the caller is the owner (multi-tenant security)
        if (!campaign.getUserEmail().equals(userEmail)) {
            return ResponseEntity.status(403)
                .body(Map.of("message", "Unauthorized to delete this campaign."));
        }

        // Step 3: Purge tracking data BEFORE deleting the campaign
        //         (respects the MySQL foreign key constraint)
        interactionRepository.deleteByCampaignId(campaign.getId());

        // Step 4: Delete the campaign itself
        campaignRepository.delete(campaign);

        return ResponseEntity.ok(Map.of("message", "Campaign deleted successfully"));

    } catch (Exception e) {
        // If any error occurs inside @Transactional → automatic ROLLBACK of both operations
        return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
    }
}
```

### Line-by-Line Talking Points

| Element | What to say |
|---------|-------------|
| `@Transactional` | *"If either DELETE fails, both are rolled back — the database stays consistent"* |
| `@PathVariable Long id` | *"Spring extracts the `42` from the URL `/api/campaigns/42` automatically"* |
| `.orElseThrow(...)` | *"Java Optional pattern — no NullPointerException, a clean business exception instead"* |
| `status(403)` | *"We return the correct semantic HTTP code: 403 = Forbidden, not 500 = Server Error"* |
| `interactionRepository.deleteByCampaignId(id)` | *"Spring Data generates the DELETE SQL from the method name — zero SQL written by us"* |

---

## 5. Security and Best Practices

### 5.1 Authentication Strategy — X-User-Email Header

> *Explain while showing the browser Network tab (F12 > Network)*

**"Our security strategy relies on two complementary layers."**

**Layer 1 — Google OAuth 2.0 (Frontend)**: The user authenticates via Google. NextAuth.js manages the session and exposes `session.user.email` to all React components. The authenticated user's email is injected into every HTTP request:

```javascript
// Frontend extract — every fetch() carries the user's identity
const res = await fetch("http://localhost:8080/api/campaigns", {
    headers: {
        "X-User-Email": session.user.email  // Email verified by Google OAuth
    }
});
```

**Layer 2 — Data Isolation (Backend)**: Every Spring Boot endpoint reads this header and filters database records accordingly:

```java
// The backend NEVER returns another user's data
@GetMapping
public ResponseEntity<List<Campaign>> getCampaigns(
        @RequestHeader("X-User-Email") String userEmail) {
    return ResponseEntity.ok(
        campaignRepository.findByUserEmailOrderByCreatedAtDesc(userEmail)  // Strict user filter
    );
}
```

### 5.2 API Key Encryption — The Vault

**"Sensitive API keys (Gemini, Gmail) are never stored in plain text. Our `VaultService` encrypts them with AES-256 before the `INSERT` and decrypts them after the `SELECT`."**

```java
// Vault entity — sensitive fields are encrypted in the database
@Entity
public class Vault {
    private String geminiApiKeyEncrypted;        // AES-256 — unreadable without the decryption key
    private String gmailOauthTokenEncrypted;     // AES-256

    @OneToOne
    @JoinColumn(name = "user_id", unique = true) // One vault per user, never shared
    private User user;
}
```

### 5.3 HTTP Status Code Semantics — REST Best Practices

**"We never return a `200 OK` for an error. Our API follows HTTP conventions, making it predictable for any client."**

| Situation | Code Returned | Example in Our Code |
|-----------|---------------|---------------------|
| Success | `200 OK` | Campaign launched, subscriber deleted |
| Invalid request | `400 Bad Request` | Segment not found, missing field |
| Unauthorized | `403 Forbidden` | Attempt to delete another user's campaign |
| Not found | `404 Not Found` | Non-existent ID |
| Server error | `500` (avoided) | Caught by try/catch, converted to 400 with a message |

```java
// Consistent pattern across all our controllers
try {
    // ... business logic ...
    return ResponseEntity.ok(result);                               // 200
} catch (Exception e) {
    return ResponseEntity.badRequest()                              // 400
        .body(Map.of("message", e.getMessage()));                   // Human-readable, no stack trace
}
```

### 5.4 CORS Protection

**"Cross-Origin Resource Sharing is configured at the controller level. Only `http://localhost:3000` (our Next.js frontend) is allowed to call the API."**

```java
@CrossOrigin(origins = "http://localhost:3000")  // ← On EVERY business controller
public class CampaignController { ... }

// Justified exception: TrackingController allows all origins
// because tracking pixels are loaded by Gmail, Outlook, Apple Mail...
@CrossOrigin(origins = "*")
public class TrackingController { ... }
```

---

## 6. Bonus Demo — The Most Impressive Feature: Email Tracking

> *Save this demo for the end to make a strong final impression*

**"Email tracking is technically the most elegant feature in the project. Here's how it works:"**

### Open Tracking — The Spy Pixel

**"When we send an email, we inject an invisible `<img>` tag into the HTML, pointing to our server."**

```html
<!-- Automatically injected into every sent email -->
<img src="http://localhost:8080/api/track/open/42/17" width="1" height="1" style="display:none"/>
<!-- campaign_id=42, subscriber_id=17 -->
```

**"When the subscriber opens the email, their email client loads this image. Our Spring Boot server receives the GET request, records the open event in the database, and returns a 1×1 transparent GIF."**

```java
@GetMapping("/open/{campaignId}/{subscriberId}")
public ResponseEntity<byte[]> trackOpen(
        @PathVariable Long campaignId,
        @PathVariable Long subscriberId) {

    // Record the interaction in the database
    EmailInteraction interaction = new EmailInteraction();
    interaction.setInteractionType("OPEN");
    interactionRepository.save(interaction);

    // Return a 1×1 transparent GIF encoded in base64
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.IMAGE_GIF);
    headers.setCacheControl("no-cache, no-store, must-revalidate"); // Prevents browser caching
    return new ResponseEntity<>(TRANSPARENT_GIF, headers, HttpStatus.OK);
}
```

### Click Tracking — The 302 Redirect

**"Links inside emails don't point directly to the destination. They go through our server first, which records the click and immediately redirects the user."**

```java
@GetMapping("/click/{campaignId}/{subscriberId}")
public ResponseEntity<Void> trackClick(
        @PathVariable Long campaignId,
        @PathVariable Long subscriberId,
        @RequestParam("url") String targetUrl) {  // Final destination URL as a parameter

    // "Implied Open" logic: if a subscriber clicked, they must have opened
    boolean hasOpened = interactionRepository
        .existsByCampaignIdAndSubscriberIdAndInteractionType(campaignId, subscriberId, "OPEN");
    if (!hasOpened) {
        // Also create an OPEN interaction automatically
        interactionRepository.save(new EmailInteraction(campaignId, subscriberId, "OPEN", null));
    }

    // Record the CLICK
    interactionRepository.save(new EmailInteraction(campaignId, subscriberId, "CLICK", targetUrl));

    // Immediately redirect to the real destination
    HttpHeaders headers = new HttpHeaders();
    headers.setLocation(URI.create(targetUrl));
    return new ResponseEntity<>(headers, HttpStatus.FOUND); // 302 Redirect — transparent to the user
}
```

---

## 7. All 28 Endpoints — Quick Reference Table

| Controller | Method | URL | Description |
|-----------|--------|-----|-------------|
| **Campaign** | GET | `/api/campaigns` | List campaigns (sorted by date DESC) |
| | POST | `/api/campaigns/launch` | Immediate launch or scheduling |
| | GET | `/api/campaigns/{id}/stats` | KPIs (sent, opens, clicks) |
| | DELETE | `/api/campaigns/{id}` | Transactional delete |
| **Subscriber** | GET | `/api/subscribers` | List filtered by user |
| | POST | `/api/subscribers/import` | CSV multipart import |
| | DELETE | `/api/subscribers/{id}` | Delete with ownership check |
| | GET | `/api/subscribers/attributes` | Available `custom_attributes` JSON keys |
| | GET | `/api/subscribers/top-engaged` | Top 5 VIP by interactions (JPQL) |
| **Segment** | GET | `/api/segments` | List segments |
| | POST | `/api/segments` | Create with JSON rules |
| | PUT | `/api/segments/{id}` | Update |
| | DELETE | `/api/segments/{id}` | Delete |
| | GET | `/api/segments/{id}/subscribers` | Evaluate segment subscribers |
| **Template** | GET | `/api/templates` | List templates |
| | POST | `/api/templates` | Create |
| | PUT | `/api/templates/{id}` | Update |
| | DELETE | `/api/templates/{id}` | Delete |
| **AI** | GET | `/api/ai/suggest-segments` | AI segment suggestions |
| | POST | `/api/ai/generate-template` | LLM HTML generation |
| | POST | `/api/ai/refine-template` | Refine current template |
| | POST | `/api/ai/generate-image` | Image generation (Pollinations/Gemini) |
| | POST | `/api/ai/wizard-generate-template` | Wizard template + auto-save |
| **Tracking** | GET | `/api/track/open/{c}/{s}` | GIF pixel + INSERT OPEN interaction |
| | GET | `/api/track/click/{c}/{s}` | 302 Redirect + INSERT CLICK interaction |
| **Media** | POST | `/api/media/upload` | Image upload (multipart, max 10MB) |
| | GET | `/api/media` | User's media gallery |
| **Vault** | GET | `/api/vault` | Retrieve encrypted keys |
| | POST | `/api/vault` | Save with AES encryption |

---

## 8. Common Jury Questions — Prepared Answers

**Q: Why didn't you use Spring Security with JWT?**

> *"For this academic prototype, we took a pragmatic approach: authentication is fully delegated to Google OAuth 2.0 — an industry standard — and data isolation is enforced by systematically filtering on `X-User-Email` in every request. Migrating to JWT would be a natural next step for a production version. Our 3-tier architecture makes this migration non-destructive: we would simply add a Spring Security filter without modifying any controllers."*

**Q: How do you handle scheduled campaigns?**

> *"In `CampaignController.launchCampaign()`, if `scheduledAt` is a future date, we save the campaign with `SCHEDULED` status and stop. A Spring `@Scheduled` task — or an external job — would be responsible for polling campaigns whose `scheduledAt` has passed. For a production version, we would recommend RabbitMQ or Quartz Scheduler for more robust and reliable scheduling."*

**Q: What happens if the Resend API is unavailable during a send?**

> *"In the `CampaignController` send loop, each call to `emailService.sendCampaignEmail()` is wrapped in its own individual `try-catch`. If Resend fails for a specific subscriber, the error is logged (`System.err.println`) but processing continues for the remaining subscribers. At the end, if `sentCount == 0`, the campaign status is marked `FAILED`. This is a best-effort approach, acceptable for this stage of the project."*

**Q: How do you ensure a user cannot see another user's data?**

> *"The mechanism is two-fold. First, all JPA queries include a filter on `userEmail` or `user_id` — it is structurally impossible to return cross-user data using our repository methods. Second, for mutating operations (DELETE in particular), we explicitly verify ownership before any action:*
> ```java
> if (!campaign.getUserEmail().equals(userEmail)) {
>     return ResponseEntity.status(403).body(...);
> }
> ```
> *Even if an attacker guessed the ID of another user's campaign, our backend returns 403 and performs no modification."*

**Q: What does `FetchType.LAZY` mean and why did you use it?**

> *"`FetchType.LAZY` tells Hibernate not to load the related entity from the database until it is explicitly accessed in code. The alternative, `EAGER`, would load all related data automatically — for example, loading a `Campaign` would trigger an immediate SQL query to load its entire `Segment` with all its `Subscribers`. In our case, most API calls don't need the full object graph, so `LAZY` prevents unnecessary database queries and avoids performance degradation as data grows."*

---

*Document generated May 12, 2026 — SmartMail Pro Backend Presentation Guide (English Version)*
*Best used alongside IntelliJ IDEA (backend open) and MySQL Workbench (schema visible)*
