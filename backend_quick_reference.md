# SmartMail Pro — Backend Quick Reference
### How It Works: File Structure & Request Flow

---

## Part 1: File Structure

```
backend/src/main/java/com/example/backend/
│
├── controller/                  ← HTTP layer — receives requests, returns responses
│   ├── AiController.java        │   POST /api/ai/generate-template
│   ├── CampaignController.java  │   GET|POST|DELETE /api/campaigns
│   ├── MediaController.java     │   POST /api/media/upload
│   ├── SegmentController.java   │   GET|POST|PUT|DELETE /api/segments
│   ├── SubscriberController.java│   GET|POST|DELETE /api/subscribers
│   ├── TemplateController.java  │   GET|POST|PUT|DELETE /api/templates
│   ├── TrackingController.java  │   GET /api/track/open | /api/track/click
│   ├── UserController.java      │   GET /api/users
│   └── VaultController.java     │   GET|POST /api/vault
│
├── service/                     ← Business logic layer — rules, processing, AI calls
│   ├── AiTemplateService.java   │   Calls Groq / Gemini to generate HTML
│   ├── AiImageService.java      │   Calls Pollinations / Gemini for images
│   ├── AiSegmentService.java    │   Calls LLM to suggest audience segments
│   ├── EnterpriseEmailService.java  Sends emails via Resend API + injects tracking pixels
│   ├── MediaService.java        │   Saves uploaded files to /uploads/ folder
│   ├── SegmentService.java      │   Filters subscribers by rules, manages segment CRUD
│   ├── SubscriberService.java   │   Parses CSV, handles subscriber lifecycle
│   ├── UserService.java         │   getOrCreateUser() — auto-registers on first login
│   └── VaultService.java        │   AES-256 encrypt/decrypt for API keys
│
├── repository/                  ← Data access layer — talks to MySQL via JPA
│   ├── CampaignRepository.java          findByUserEmailOrderByCreatedAtDesc()
│   ├── EmailInteractionRepository.java  countBy... | deleteByCampaignId() | @Query JPQL
│   ├── MediaRepository.java             findByUserEmail()
│   ├── SegmentRepository.java           findByUserEmail()
│   ├── SubscriberRepository.java        findByUserEmail()
│   ├── TemplateRepository.java          findByUserEmail()
│   ├── UserRepository.java              findByEmail()
│   └── VaultRepository.java            findByUserEmail()
│
├── entity/                      ← Java classes mapped 1:1 to MySQL tables
│   ├── User.java                │   TABLE: users          (id, email, created_at)
│   ├── Subscriber.java          │   TABLE: subscribers    (id, email, status, custom_attributes JSON, user_id FK)
│   ├── Segment.java             │   TABLE: segments       (id, name, rules TEXT, by_ai, user_id FK)
│   │                            │   JOIN TABLE: segment_subscribers (segment_id, subscriber_id)
│   ├── Template.java            │   TABLE: templates      (id, name, html_content TEXT, user_id FK)
│   ├── Campaign.java            │   TABLE: campaigns      (id, name, subject, status, segment_id FK, template_id FK)
│   ├── EmailInteraction.java    │   TABLE: email_interactions (id, campaign_id, subscriber_id, type, url, timestamp)
│   ├── Media.java               │   TABLE: media          (id, file_name, file_url, file_type, user_id FK)
│   └── Vault.java               │   TABLE: vault          (id, gemini_key_enc, gmail_token_enc, user_id FK UNIQUE)
│
├── dto/
│   └── CampaignStatsDTO.java    ← Data Transfer Object: wraps (totalSent, uniqueOpens, uniqueClicks)
│
└── resources/
    └── application.properties   ← MySQL URL, JPA config, multipart limits, API keys
```

---

## Part 2: Master Concept — The Request Flow

Every single feature in SmartMail Pro follows **the same 4-step path**:

```
[Next.js Frontend]  →  [Controller]  →  [Service]  →  [Repository]  →  [MySQL]
                                                                           ↓
[Next.js Frontend]  ←  [Controller]  ←  [Service]  ←  [Repository]  ←  [MySQL]
```

### Step-by-step with a real example: **"Launch a Campaign"**

---

```
STEP 1 — Frontend sends the request
──────────────────────────────────────────────────────────────────────────
  fetch("http://localhost:8080/api/campaigns/launch", {
      method: "POST",
      headers: {
          "Content-Type": "application/json",
          "X-User-Email": "alice@gmail.com"     ← user identity
      },
      body: JSON.stringify({
          name: "Summer Sale",
          subject: "🔥 50% off this weekend",
          segmentId: 3,
          templateId: 7,
          scheduledAt: null                      ← null = send immediately
      })
  })


STEP 2 — Controller receives and validates
──────────────────────────────────────────────────────────────────────────
  @PostMapping("/launch")
  public ResponseEntity<?> launchCampaign(
          @RequestHeader("X-User-Email") String userEmail,   ← "alice@gmail.com"
          @RequestBody Map<String, Object> payload) {        ← the JSON above

      // Extracts the IDs from the JSON payload
      Long segmentId  = Long.valueOf(payload.get("segmentId").toString());   // 3
      Long templateId = Long.valueOf(payload.get("templateId").toString());  // 7

      // Loads objects from database
      Segment  segment  = segmentRepository.findById(segmentId).orElseThrow();
      Template template = templateRepository.findById(templateId).orElseThrow();

      // Delegates to service layer
      emailService.sendCampaignEmail(...);
  }


STEP 3 — Service executes the business logic
──────────────────────────────────────────────────────────────────────────
  EnterpriseEmailService:
      1. Loops over every Subscriber in segment.getSubscribers()
      2. For each subscriber, injects the tracking pixel into the HTML:
             <img src="http://localhost:8080/api/track/open/42/17"/>
      3. Calls Resend API → sends the real email
      4. Returns sentCount (how many emails delivered successfully)


STEP 4 — Repository writes to MySQL
──────────────────────────────────────────────────────────────────────────
  campaignRepository.save(campaign);
  ↓
  Hibernate generates and executes:
      INSERT INTO campaigns (name, subject, user_email, segment_id, template_id, status, sent_at)
      VALUES ('Summer Sale', '🔥 50% off...', 'alice@gmail.com', 3, 7, 'SENT', NOW())


RESPONSE travels back up the same chain
──────────────────────────────────────────────────────────────────────────
  MySQL        → Repository  (returns saved Campaign object)
  Repository   → Service     (returns sentCount = 47)
  Service      → Controller  (returns success info)
  Controller   → Frontend    HTTP 200 OK
                              {"message": "Campaign launched! Sent to 47 recipients."}
```

---

### The Same Flow for Every Feature

| Feature | Controller | What the Service does | What the Repository writes |
|---------|-----------|----------------------|---------------------------|
| Import CSV | `POST /api/subscribers/import` | Parses CSV line by line, creates `Subscriber` objects | `subscriberRepository.save()` × N |
| Track Open | `GET /api/track/open/42/17` | Builds `EmailInteraction` object | `interactionRepository.save(OPEN)` |
| Track Click | `GET /api/track/click/42/17?url=...` | Checks implied open, builds CLICK record, sets redirect | `interactionRepository.save(CLICK)` → HTTP 302 |
| Generate AI Template | `POST /api/ai/generate-template` | Calls Groq/Gemini API with prompt, cleans response HTML | Nothing — returns HTML to frontend |
| Upload Image | `POST /api/media/upload` | Saves file to `/uploads/` folder, builds `Media` object | `mediaRepository.save()` |
| Delete Campaign | `DELETE /api/campaigns/42` | Verifies ownership, purges interactions, deletes campaign | `interactionRepository.deleteByCampaignId(42)` then `campaignRepository.delete()` |

---

### The One Rule That Never Breaks

Every repository call includes a **user filter**. It is structurally impossible for Alice to see Bob's data:

```java
// Every GET query is scoped to the authenticated user
campaignRepository.findByUserEmailOrderByCreatedAtDesc("alice@gmail.com");
//  ↓ Hibernate generates:
//  SELECT * FROM campaigns WHERE user_email = 'alice@gmail.com' ORDER BY created_at DESC

// Every DELETE verifies ownership before acting
if (!campaign.getUserEmail().equals(userEmail)) {
    return ResponseEntity.status(403).body("Unauthorized");
}
```

---

*SmartMail Pro — Backend Quick Reference | May 2026*
