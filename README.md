# SmartMail Pro 🚀

SmartMail Pro is an enterprise-grade Marketing Information System (MIS) designed to bridge the gap between static databases and highly personalized customer experiences. Built with a decoupled **Next.js** and **Spring Boot** architecture, it leverages generative AI (**Google Gemini**) to automate the creation of persuasive email campaigns and features a dynamic SQL-based segmentation engine.

Developed as a 4th-year engineering academic project (IIR - DDSI).

## ✨ Key Features

* **Centralized Security:** Passwordless authentication exclusively via Google SSO (OAuth 2.0) with an encrypted API Vault for third-party keys.
* **Dynamic Segmentation:** Interactive rule builder that translates UI filters into raw, dynamic SQL queries for precise audience targeting.
* **AI Creative Studio:** A multimodal chat interface powered by the Gemini API, integrated with a WYSIWYG editor (TipTap) that automatically compiles visual formatting into inline CSS for cross-client email compatibility.
* **Campaign Orchestrator:** A streamlined 3-step workflow for configuring, designing, and securely dispatching emails using the Gmail API.
* **Real-Time Analytics:** Custom backend proxy (`/track/click`) to intercept, log, and redirect user interactions, tracking Open Rates and CTR in real-time.

## 🛠️ Tech Stack

* **Frontend:** Next.js (React), Tailwind CSS, TipTap, NextAuth.js
* **Backend:** Java Spring Boot, Spring Security (OAuth2 Resource Server), Spring Data JPA
* **Database:** MySQL 8.0
* **DevOps:** Docker & Docker-compose
* **Integrations:** Google Gemini API, Gmail API

## 🚀 Quick Start (Local Development)

1. Clone the repository.
2. Spin up the database:
   \`\`\`bash
   docker-compose up -d
   \`\`\`
3. Configure your `.env.local` in the `frontend` directory with your Google OAuth credentials.
4. Run the frontend:
   \`\`\`bash
   cd frontend
   npm run dev
   \`\`\`
5. Start the Spring Boot backend via your preferred IDE or Gradle wrapper.