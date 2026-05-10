# Specifications Document DEV-3.5

## Personalized Newsletter and Advertising Campaign Platform

*(User segmentation, dynamic templates, performance tracking, and AI-based features)*

---

# 1. Project Overview

## 1.1 Context

In today’s digital landscape, generic marketing emails lead to high unsubscribe rates and low return on investment (ROI).

**SmartMail Pro** is a Marketing Information System (MIS) designed in an academic context to bridge the gap between static databases and personalized customer experiences.

By leveraging generative AI through a multi-model architecture, the platform automates the creation of high-quality content, visual asset generation, and precise audience targeting.

## 1.2 Objectives

- **Multi-AI Automation:**  
  Use an intelligent routing engine (Groq API for fast text generation, Gemini as fallback, and Pollinations/Gemini for image generation) to create persuasive HTML templates and graphic assets.

- **Precision Targeting:**  
  Implement a dynamic logic engine to isolate specific audience segments based on dynamic criteria, assisted by AI.

- **Performance Tracking:**  
  Develop a click-tracking system to measure real-time engagement (open rates and click-through rates).

- **Integration & Security:**  
  Ensure exclusive access through Google SSO and secure distribution via Gmail API or SMTP using the OAuth 2.0 protocol.

---

# 2. Functional Requirements

The platform is organized around key functional modules designed for smooth navigation through a **Single Page Application (SPA)** architecture:

- **Authentication & Profile:**  
  Secure login exclusively through Google SSO. Dedicated profile page to configure the encrypted “Vault” (Groq/Gemini API keys and OAuth access).

- **Dashboard:**  
  Real-time KPI scoreboard (click rate, open rate, conversions) and activity history.

- **Subscriber Management (CRM):**  
  Import tools (CSV/Excel), detailed CRUD views, and engagement score tracking per user.

- **Dynamic Segmentation:**  
  Interactive rule builder and AI-powered suggestion hub (LLaMA 3.1 via Groq) for instant database analysis.

- **Media Manager (New):**  
  Isolated local storage system allowing manual uploads or AI-generated images, directly integrable into templates.

- **AI Creative Studio & Templates:**  
  Advanced interface integrating:
  - an AI assistant (to generate and refine initial HTML code),
  - and the GrapesJS framework (professional drag-and-drop visual editor) allowing structural modifications without coding.

- **Campaign Orchestrator:**  
  Optimized 3-step workflow:
  
  1. Configuration / Segment selection
  2. Routing to the AI Studio for final design adjustments
  3. Scheduled or immediate sending

---

# 3. Data Requirements (Entities to Analyze)

To operate properly, the system must relationally manage the following entities:

- **Administrator Users:**  
  Identity data obtained through Google SSO login.

- **API Vault:**  
  Encrypted tokens used for communication with AI providers and Google services.

- **Subscriber Data:**  
  Identity, email, and dynamic attributes (JSON format) for targeting purposes.

- **Segmentation Logic:**  
  Saved filtering criteria dynamically linked to subscriber data.

- **Media Assets:**  
  Physical files stored on the server with user-related metadata.

- **Templates:**  
  Generated HTML code including source tracking (AI-generated or manual).

- **Campaign Records:**  
  Junction table orchestrating the targeted segment, selected template, and delivery status.

- **Engagement Logs:**  
  Transactional records of open-tracking pixels and redirected clicks.

---

# 4. Technical Specifications

## 4.1 Technology Stack

- **Frontend:**  
  Next.js (React), Tailwind CSS for styling, and GrapesJS for the drag-and-drop visual editor component.

- **Backend:**  
  Java Spring Boot (MVC architecture, Spring Data JPA, Spring Security for OAuth2 management).

- **Database:**  
  MySQL 8.0 to ensure relational integrity.

- **DevOps:**  
  Docker Compose for unified containerization of the database and application servers.

## 4.2 Core System Logic

- **Inline CSS Compiler:**  
  The GrapesJS editor automatically converts visual formatting into inline CSS styles for maximum compatibility with email clients.

- **Link Proxy (Click Tracking):**  
  Backend redirection service (`/track/click`) to intercept requests, log user activity, and transparently redirect the user.

---

# 5. Security and Quality Requirements

## Authentication & Authorization

- Full identity delegation through Google OAuth 2.0.
- Internal API route protection using JWT.

## Encryption & Key Security

- External API keys stored in the database are encrypted at rest.
- Strict use of environment variables (`application.properties`) to prevent leaks on remote repositories (GitHub Push Protection).

## Software Quality

### Unit Testing

- Business logic coverage using JUnit 5.

### Documentation

- Use of Swagger/OpenAPI to document endpoints.

---

# 6. Agile Planning (Micro-Sprints)

- **Sprint 1:**  
  Core architecture, MySQL containerization, and Google SSO.

- **Sprint 2:**  
  Frontend layout and Profile module development (secure API Vault).

- **Sprint 3:**  
  CRM core development (Subscriber CRUD, CSV parsing).

- **Sprint 4:**  
  Rule engine (UI Builder) and AI-powered Dynamic Segmentation (Groq).

- **Sprint 5:**  
  Media Engine (physical file storage and multi-provider image integration).

- **Sprint 6:**  
  Creative Studio: GrapesJS integration and Groq/Gemini-powered template generation.

- **Sprint 7:**  
  Campaign Orchestrator and email delivery engine implementation (JavaMailSender/Gmail API).

- **Sprint 8:**  
  Tracking engine (invisible pixel, URL redirection), Analytics Dashboard, and presentation preparation.

---

# 7. Expected Deliverables for the Defense

## Software

- Complete and functional source code (Frontend & Backend).

## Analysis Report

- UML diagrams (Use Case, Sequence, Class diagrams) and Logical Data Model (LDM).

## Testing Documentation

- Test reports and API validation.

## Documentation

- `README.md` file with Docker deployment instructions and Swagger setup.

---

# SmartMail Pro : System Architecture & Context

# 1. Project Overview

SmartMail Pro is an enterprise-grade, intelligent email marketing platform. It is designed to replace traditional static email systems by integrating dynamic data segmentation and a Multi-Provider AI Engine (Groq, Gemini, Pollinations) to automate audience targeting, HTML template creation, and multimedia generation.

The core philosophy of the application is **Data-Driven Automation via Generative AI**, combined with a **Multi-Tenant Architecture** to isolate resources per authenticated user.

---

# 2. Technical Stack

## Frontend Ecosystem

- Next.js (React)
- Tailwind CSS
- GrapesJS (Web/Email Visual Builder framework)

## Backend Ecosystem

- Java Spring Boot
- Spring Security (OAuth2)
- Spring Data JPA (Hibernate)
- RESTful API Architecture

## Database

- MySQL 8.0 (utilizing relational mapping and JSON processing for dynamic attributes)

## AI Infrastructure (Dual-Engine / Multi-Provider)

### Text & HTML Code

- Groq (LLaMA 3.1, used as the primary engine for ultra-fast, free processing)
- Google Gemini (1.5 Flash, used as the premium fallback)

### Image Generation

- Pollinations.ai (primary free engine)
- Gemini Flash Image (premium fallback)

---

# 3. Core Architectural Concepts

## 3.1 Multi-Tenancy & Security

### SSO & Isolation

All authentication is handled via Google SSO (OAuth2). There are no local passwords.

Every entity in the database (`Subscriber`, `Segment`, `Template`, `Media`, `Vault`) has a strict `@ManyToOne` relationship with a `User`.

The backend enforces user isolation via `X-User-Email` HTTP headers (validated against the session) to ensure a user can only read/write their own data.

### The Cryptographic Vault

Users store their personal third-party API keys (like Gemini) in a dedicated vault database table.

These keys are symmetrically encrypted at rest using a backend `EncryptionUtil` before saving, and decrypted in memory only when the API is called.

### Environment Protection

Critical infrastructure keys (like the overarching Groq API key) are stored in local `application.properties` environment variables and strictly excluded from version control to prevent GitHub scraping.

---

## 3.2 The Media Storage Engine (Local Hard Drive Pipeline)

The system does not rely on third-party cloud storage (like AWS S3) for media.

It utilizes a custom local storage pipeline:

### Database Entity (`Media`)

Tracks the file metadata, ownership, and the virtual URL.

### Physical Storage (`uploads/`)

A directory on the Spring Boot server's hard drive where the binary bytes (`byte[]` or `MultipartFile`) are saved using `java.nio.file.Files`.

### HTTP Serving

A `WebMvcConfigurer` resource handler intercepts requests to `/uploads/**` and serves the physical files directly to the Next.js frontend.

---

# 4. Application Modules & User Flow

## Page 1: Dashboard & Authentication

### Flow

User authenticates via NextAuth (Google). If they are new, the backend provisions a `User` record.

### Function

Displays high-level analytics (open rates, click rates, active campaigns).

---

## Page 2: Subscriber Management (CRM)

### Flow

Users can manually add subscribers or upload a CSV file.

### Function

Stores basic user data (`email`, `firstName`, `status`) and a dynamic `customAttributes` JSON payload.

Example:

```json
{
  "city": "Paris",
  "lifetimeValue": 500
}
```

---

## Page 3: The Segmentation Engine

### Flow

Users define logical rules to filter their CRM database.

### Manual Mode

A UI builder generates an array of rules.

Example:

- Column: `status`
- Operator: `=`
- Value: `VIP`

### AI Mode (Groq/Gemini)

The backend samples the actual user data schema (column names and sample values) and sends it to the AI prompt.

The AI returns a structured JSON array of newly invented, context-aware targeting segments, completely bypassing the need for the user to understand database querying.

---

## Page 4: The Master Template Studio (The Crown Jewel)

This page is a unified workstation combining three major systems:

### The Code/Design Interface

Powered by GrapesJS.

It provides a fully functional drag-and-drop canvas for building responsive email newsletters.

Users can:
- drag layout blocks,
- edit text inline,
- design without writing code.

### The Media Asset Bar

A horizontal, draggable gallery directly above the canvas.

It fetches the user's isolated images from the backend `uploads/` folder.

Users can drag images from this bar directly into the GrapesJS canvas.

### AI Image Generation

Features an embedded toggle switch (Pollinations vs. Gemini).

Users input a prompt, the AI generates the image bytes, the backend saves the file physically, and the UI live-updates the gallery bar.

### The AI Template Generator

Features an embedded toggle switch (Groq vs. Gemini).

Users input a prompt (e.g., `"A modern product launch email"`).

The AI generates raw, inline-styled HTML code.

The Next.js state immediately pushes that raw HTML into the GrapesJS canvas, allowing the user to visually edit the AI's output using drag-and-drop tools.

---

## Page 5: The Campaign Orchestrator

### Flow

The user selects a saved `Template`, attaches it to a saved `Segment`, and hits send.

### Function

The backend uses `JavaMailSender` (SMTP) to iterate through the filtered subscribers and dispatch the customized HTML payload.

---

## Page 6: The Analytics Tracker

### Flow

Every outbound email contains:
- an invisible tracking pixel,
- rewritten proxy URLs.

### Function

The backend intercepts `GET` requests to these proxies, logs the interaction (`opened`, `clicked`) against the specific campaign and subscriber, and transparently redirects the user to the final destination.