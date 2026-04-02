# SmartMail Pro

SmartMail Pro is an AI-powered marketing information system designed to help businesses create, personalize, manage, and track email marketing campaigns more effectively. The platform combines subscriber management, dynamic segmentation, AI-assisted content generation, visual email editing, campaign orchestration, and engagement analytics in one integrated system.

Its main goal is to solve a common problem in digital marketing: generic mass emails often result in low engagement, poor conversion rates, and higher unsubscribe rates. SmartMail Pro addresses this by allowing administrators to create targeted, persuasive, and data-driven email campaigns based on subscriber behavior and segmentation rules.

---

## Table of Contents

- [Project Overview](#project-overview)
- [Objectives](#objectives)
- [Key Features](#key-features)
- [System Modules](#system-modules)
- [User Workflow](#user-workflow)
- [Architecture](#architecture)
- [Technology Stack](#technology-stack)
- [Data Model Overview](#data-model-overview)
- [Security](#security)
- [Analytics and Tracking](#analytics-and-tracking)
- [AI Integration](#ai-integration)
- [Campaign Lifecycle](#campaign-lifecycle)
- [UI and User Experience](#ui-and-user-experience)
- [Development Roadmap](#development-roadmap)
- [Expected Deliverables](#expected-deliverables)
- [Future Improvements](#future-improvements)
- [Project Summary](#project-summary)

---

## Project Overview

SmartMail Pro is a full-stack web application built to modernize newsletter and advertising campaign management. It acts as a centralized platform where administrators can:

- authenticate securely using Google Sign-In,
- manage subscriber data,
- create dynamic audience segments,
- generate email content using AI,
- refine content through a visual editor,
- launch campaigns via Gmail,
- and monitor campaign performance using click and open analytics.

The system is designed not only as a mailing tool but also as a marketing operations platform. It integrates CRM-style subscriber handling, rule-based segmentation, AI creativity tools, and measurable campaign outcomes.

---

## Objectives

The project is built around the following core objectives:

### 1. Automate persuasive email creation
SmartMail Pro uses generative AI through the Gemini API to help users create marketing emails and newsletter templates in HTML format. This reduces the manual effort required to design engaging campaigns.

### 2. Enable precision targeting
The platform includes a dynamic segmentation engine that allows administrators to define audiences based on subscriber attributes, engagement level, geography, purchasing behavior, and other criteria.

### 3. Provide measurable campaign performance
Campaign results are tracked through email open monitoring and click redirection logs. This gives administrators visibility into how their campaigns perform.

### 4. Secure external integrations
Authentication and delivery rely on Google technologies:
- Google SSO for administrator login
- Gmail OAuth for sending campaigns
- secure storage of API credentials and tokens

---

## Key Features

### AI-Powered Template Generation
- Generate marketing emails using Gemini
- Produce persuasive newsletter and promotional content
- Store prompt history alongside generated templates
- Support HTML email structure for real-world campaign use

### Subscriber Management
- Import subscriber lists from CSV or Excel files
- Create, edit, update, and manage subscriber profiles
- Store engagement-related metrics
- Filter and search subscribers easily

### Dynamic Segmentation
- Build reusable segments using logical rules
- Combine criteria with `AND` / `OR`
- Segment users by city, activity, engagement, purchase behavior, and more
- Maintain segment logic dynamically as subscriber data evolves

### Visual Email Editing Studio
- Edit generated templates through a WYSIWYG interface
- Insert and format text and images visually
- Preview emails before sending
- Save and reuse edited templates

### Campaign Orchestration
- Create, configure, schedule, and dispatch campaigns
- Associate each campaign with a segment and template
- Track campaign state and history
- Edit or cancel scheduled campaigns

### Dashboard and Analytics
- Monitor KPIs such as:
  - open rate
  - click-through rate
  - conversion-related engagement
- View recent campaign history
- Access summary cards and performance indicators

### Secure Profile and API Settings
- Manage Google account authentication
- Configure Gemini API integration
- Configure Gmail OAuth connection
- Store sensitive integration data securely in the Vault

---

## System Modules

SmartMail Pro is structured around several major modules.

### 1. Authentication and Profile Module
This module handles secure administrator login and account settings.

**Main responsibilities:**
- Google Sign-In authentication
- user profile display
- integration setup for Gemini and Gmail
- token and API key management through a secure Vault

---

### 2. Dashboard Module
The dashboard acts as the operational entry point of the system.

**Main responsibilities:**
- display marketing KPIs
- show campaign summaries
- present recent activity
- provide fast access to key modules

---

### 3. Subscriber Management Module
This module works as a lightweight CRM for campaign targeting.

**Main responsibilities:**
- import subscribers from files
- create and update subscriber records
- store custom subscriber attributes
- track engagement level
- search and filter subscriber lists

---

### 4. Segmentation Module
The segmentation engine is one of the most important modules in the platform.

**Main responsibilities:**
- create logical audience rules
- build reusable and dynamic segments
- use rule combinations based on subscriber attributes
- support AI-suggested segment ideas

**Example segmentation criteria:**
- city
- engagement score
- purchase count
- order history
- email opens
- inactivity risk

---

### 5. Template Library Module
The template library stores reusable email content.

**Main responsibilities:**
- save generated templates
- preview templates
- edit or delete templates
- manage HTML content and styling
- keep prompt history linked to generated content

---

### 6. AI Studio Module
The AI Studio combines generative AI with visual editing tools.

**Main responsibilities:**
- generate content from user prompts
- support conversational prompting
- provide a live preview area
- allow text formatting and image insertion
- refine generated emails visually before campaign launch

---

### 7. Campaign Module
This module governs the campaign lifecycle from creation to delivery.

**Main responsibilities:**
- create campaigns
- select audience segments
- attach email templates
- configure dispatch details
- send through Gmail integration
- track campaign status and history

---

## User Workflow

The platform follows a logical workflow for administrators:

1. **Log in using Google SSO**
2. **Configure API integrations** in the profile/settings area
3. **Import and manage subscribers**
4. **Create one or more segments**
5. **Generate an email template using AI**
6. **Refine the email in the visual editor**
7. **Create a campaign**
8. **Assign the target segment and template**
9. **Launch or schedule the campaign**
10. **Track opens, clicks, and engagement metrics**

This workflow reflects a real marketing operation pipeline, from audience preparation to performance analysis.

---

## Architecture

SmartMail Pro follows a full-stack web architecture composed of a modern frontend, a robust backend, and a relational database.

### Frontend
The frontend is designed as a single-page application with an admin dashboard experience.

**Responsibilities:**
- render dashboards, forms, editors, and tables
- manage routing between major sections
- provide real-time interactivity
- support visual template editing

### Backend
The backend handles business logic, security, data processing, integrations, and analytics.

**Responsibilities:**
- authentication and authorization
- subscriber management
- segment rule processing
- campaign execution
- AI integration
- Gmail dispatch
- tracking and analytics logging

### Database
The database stores all business entities and links them together.

**Responsibilities:**
- persist users and settings
- store subscribers and attributes
- save segment rules
- keep templates and prompt history
- register campaigns
- log opens and clicks

---

## Technology Stack

### Frontend
- **Next.js**
- **React**
- **Tailwind CSS**
- **TipTap** for rich text / visual editing

### Backend
- **Java Spring Boot**
- **Spring MVC**
- **Spring Data JPA**
- **Spring Security**
- **OAuth2 / JWT**

### Database
- **MySQL 8.0**

### DevOps / Deployment
- **Docker**
- **docker-compose**

### Integrations
- **Google SSO**
- **Gmail OAuth**
- **Gemini API**

### Documentation and Testing
- **JUnit 5**
- **Swagger / OpenAPI**
- **Postman**

---

## Data Model Overview

The system is centered around several core entities.

### Administrator User
Represents the authenticated platform user.

**Contains:**
- Google account identity
- profile information
- access and integration metadata

### Vault
Stores sensitive integration data securely.

**Contains:**
- Gemini API credentials or tokens
- Gmail OAuth tokens
- encrypted secrets

### Subscriber
Represents a marketing recipient.

**Contains:**
- contact information
- location or city
- engagement score
- purchase or activity data
- flexible custom attributes

### Segment
Represents a dynamic targeting rule set.

**Contains:**
- segment name
- rule logic
- JSON or structured condition definitions
- logical operators

### Template
Represents a saved email design.

**Contains:**
- HTML content
- inline CSS
- prompt history
- title and metadata

### Campaign
Represents a dispatchable marketing action.

**Contains:**
- campaign name
- selected segment
- selected template
- status
- scheduling or dispatch metadata

### Engagement Log
Stores interaction events after sending.

**Contains:**
- open events
- click events
- timestamps
- campaign linkage
- subscriber linkage

---

## Security

Security is a core requirement of SmartMail Pro.

### Authentication
- administrator login is handled through **Google OAuth 2.0**
- the platform does not rely on traditional email/password login

### Authorization
- internal API access is secured using **JWT**
- authenticated sessions are protected through Spring Security

### Secret Management
- external API credentials must be encrypted at rest
- access tokens are stored securely in the Vault
- sensitive information is never exposed in the UI

### Secure Delivery
- Gmail campaign sending uses OAuth-based authorization
- no insecure SMTP credential storage is required in the traditional sense

---

## Analytics and Tracking

SmartMail Pro includes campaign analytics features to measure real impact.

### Email Open Tracking
The system can include an invisible tracking pixel in outbound emails to detect when recipients open the message.

### Click Tracking
Links in campaigns are rewritten through a backend redirection route. When a recipient clicks a link:
1. the click is logged,
2. engagement data is stored,
3. the user is redirected to the original target URL.

### KPI Monitoring
Tracked data can be used to calculate:
- open rate
- click-through rate
- engagement performance
- campaign-level comparisons

---

## AI Integration

AI is central to SmartMail Pro’s value proposition.

### Gemini Usage
Gemini is used to:
- generate persuasive marketing copy
- produce HTML-ready email content
- assist with campaign ideation
- potentially suggest useful segment strategies

### AI Studio Behavior
Inside the AI Studio, the user can:
- submit prompts in natural language
- receive generated content
- preview results immediately
- refine output visually
- store templates for future reuse

This allows the system to reduce manual creative effort while keeping human control over the final output.

---

## Campaign Lifecycle

The campaign process is designed as a structured multi-step workflow.

### Step 1: Configuration
The administrator creates a campaign and defines the main parameters:
- campaign name
- target segment
- selected audience
- basic settings

### Step 2: Studio
The user enters the creative stage:
- generate content using AI
- edit content visually
- preview the final design

### Step 3: Dispatch
The campaign is prepared for sending:
- connect through Gmail OAuth
- confirm recipients through the selected segment
- send or schedule the email campaign

This staged structure improves usability and reflects real marketing team workflows.

---

## UI and User Experience

The prototype demonstrates a clear admin dashboard experience with multiple dedicated views.

### Main Screens
- Dashboard
- Subscribers
- Segments
- Templates
- Campaigns
- Profile & Settings

### UI Characteristics
- card-based dashboard layout
- subscriber tables and filters
- rule builders for segmentation
- template previews
- campaign status badges
- progress-driven campaign flow
- live email preview in the editor

### Regional and Commercial Positioning
The prototype suggests that SmartMail Pro is suitable for real-world commercial use, especially in Arabic-speaking and Gulf-region e-commerce contexts. Promotional examples and messaging styles indicate a focus on persuasive, localized marketing communication.

---

## Development Roadmap

The project can be organized into a sequence of micro-sprints.

### Sprint 1
- project initialization
- overall architecture setup
- Google SSO implementation

### Sprint 2
- frontend layout and navigation
- profile and settings pages
- secure Vault setup

### Sprint 3
- subscriber CRM development
- import and CRUD operations

### Sprint 4
- segmentation engine implementation
- rule builder and dynamic logic

### Sprint 5
- template library CRUD
- Gemini integration for content generation

### Sprint 6
- WYSIWYG editor integration
- AI Studio completion

### Sprint 7
- Gmail campaign sending
- dispatch workflow integration

### Sprint 8
- click/open tracking
- analytics dashboard completion
- testing, documentation, and presentation preparation

---

## Expected Deliverables

The final project is expected to include:

- full source code
- UML diagrams
- relational data model
- Swagger/OpenAPI documentation
- Postman collection
- unit test reports
- Docker-based deployment instructions
- technical documentation
- final presentation/demo assets

---

## Future Improvements

Although the current scope is already substantial, SmartMail Pro can be extended further in future versions.

### Potential Enhancements
- campaign scheduling automation
- A/B testing support
- advanced conversion analytics
- recommendation-based segment suggestions
- richer image generation and asset management
- multilingual template generation
- unsubscribe and compliance management
- role-based access for teams
- advanced reporting exports

---

## Project Summary

SmartMail Pro is a complete AI-enhanced email marketing platform that combines:

- secure Google-based authentication,
- subscriber CRM capabilities,
- rule-based segmentation,
- AI-generated content,
- visual email editing,
- Gmail-based campaign delivery,
- and measurable campaign analytics.

It is not just a mailing tool, but a full marketing workflow system designed to help administrators create more relevant, engaging, and effective email campaigns from a single platform.

By combining data, automation, AI, and performance tracking, SmartMail Pro provides a practical and modern solution for personalized digital marketing.

---

## Notes

This README describes the **project concept, scope, architecture, and expected functionality**.  
If this repository already contains implementation files, you may later add:

- installation instructions,
- environment variables,
- API endpoint documentation,
- database migration steps,
- and repository-specific usage commands.
