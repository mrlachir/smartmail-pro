# SmartMail Pro 🚀

SmartMail Pro is an enterprise-grade Marketing Information System (MIS) designed to bridge the gap between static databases and highly personalized customer experiences. Built with a decoupled Next.js and Spring Boot architecture, it leverages generative AI (Google Gemini) to automate the creation of persuasive email campaigns and features a dynamic segmentation engine.

*Developed as a 4th-year engineering academic project (IIR - DDSI).*

## ✨ Key Features
* **Centralized Security:** Passwordless authentication exclusively via Google SSO (OAuth 2.0) with an encrypted API Vault for third-party keys.
* **Dynamic Segmentation:** Interactive rule builder that translates UI filters into raw, dynamic SQL queries for precise audience targeting based on custom CSV attributes.
* **AI Creative Studio:** A multimodal chat interface powered by the Gemini API, integrated with a WYSIWYG editor (TipTap) that automatically compiles visual formatting into inline CSS for cross-client email compatibility.
* **Campaign Orchestrator:** A streamlined 3-step workflow for configuring, designing, and securely dispatching emails using the Gmail API.
* **Real-Time Analytics:** Custom backend proxy (`/track/click`) to intercept, log, and redirect user interactions, tracking Open Rates and CTR in real-time.

## 🛠️ Tech Stack
* **Frontend:** Next.js (React), Tailwind CSS, TipTap, NextAuth.js
* **Backend:** Java Spring Boot (Java 17), Spring Security, Spring Data JPA
* **Database:** MySQL 8.0 (Docker)
* **Integrations:** Google SSO, Gemini AI, Gmail API

## 🚀 Quick Start (Local Development)
1. Clone the repository.
2. Spin up the database: `docker-compose up -d`
3. Configure your `.env.local` in the `frontend` directory with your Google OAuth credentials.
4. Run the frontend: `npm run dev`
5. Start the Spring Boot backend via your preferred IDE or Gradle wrapper: `.\gradlew bootRun`

## 📦 Sprint Progress & Features Implemented
* **[Sprint 1] Local Environment & Authentication:** Fully dockerized MySQL database linked to a Spring Boot backend and Next.js frontend. Secured via Google SSO (OAuth 2.0).
* **[Sprint 2] Encrypted API Vault:** Secure, database-backed vault for storing sensitive 3rd-party credentials using AES-128 encryption.
* **[Sprint 3] Subscriber Management:** Full CSV upload pipeline and CRUD operations for managing mailing lists.
* **[Sprint 4] Dynamic Segmentation Engine:** JSON-backed dynamic attribute storage allowing for multi-condition (AND logic), mathematical, and text-based filtering of subscribers.

## 📋 Project Backlog Status
- [x] US 1.1: Local Environment Setup (Docker/MySQL)
- [x] US 1.2: Google SSO Authentication
- [x] US 2.1: Encrypted API Vault Profile
- [x] US 3.1: CSV Contact Import
- [x] US 3.2: Subscriber CRUD Operations
- [x] US 4.1: Visual Rule Builder for Segmentation
- [x] US 4.2: AI-Suggested Segments
- [x] US 5.1: Template Management Grid
- [x] US 5.2: AI HTML Generation via Chat
- [x] US 6.1: AI Image Generation
- [x] US 6.2: WYSIWYG Editor
- [x] US 6.3: Inline CSS Compilation
- [ ] US 7.1: Campaign Configuration Workflow
- [ ] US 7.2: Gmail API Integration
- [ ] US 8.1: Open Rate Tracking (Invisible Pixel)
- [ ] US 8.2: Click-Through Rate Tracking
- [ ] US 8.3: Real-Time KPI Dashboard