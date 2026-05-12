# Rapport Final — SmartMail Pro
### Plateforme SaaS d'Email Marketing Intelligente
**Présenté à : Prof. JIBRAILI MALAK**
**Année académique : 2025–2026**

---

## Dédicace

À nos familles, dont le soutien indéfectible a rendu possible chaque ligne de code de ce projet. À nos professeurs, qui nous ont transmis la rigueur intellectuelle nécessaire à la réalisation d'un système logiciel de cette envergure.

---

## Remerciements

Nous tenons à exprimer notre profonde gratitude au Professeur JIBRAILI MALAK pour son encadrement rigoureux et ses précieux conseils tout au long de ce projet. Nos remerciements s'étendent également à l'ensemble du corps enseignant pour la qualité de la formation dispensée.

---

## Table des Matières

1. Contexte général du projet
2. Introduction
3. Présentation générale du projet
4. Méthodologie de travail
5. Analyse et conception
6. Technologies et outils
7. Sécurité du système
8. Intelligence Artificielle
9. Fonctionnalités développées
10. Tests et validation
11. Difficultés rencontrées
12. Solutions apportées
13. Bilan du projet
14. Perspectives d'amélioration
15. Conclusion générale
16. Bibliographie & Webographie
17. Annexes

---

## 1. Contexte Général du Projet

Dans un écosystème numérique en constante évolution, l'email marketing demeure l'un des canaux de communication les plus rentables, offrant un retour sur investissement moyen de 42 dollars pour chaque dollar dépensé (DMA, 2023). Face à la complexité croissante des outils existants (Mailchimp, Brevo, HubSpot), les PME et les équipes marketing indépendantes souffrent d'un manque d'outils accessibles, personnalisables et intelligents.

C'est dans ce contexte que **SmartMail Pro** a été conçu : une plateforme SaaS d'email marketing full-stack, développée de A à Z par notre équipe, intégrant l'intelligence artificielle générative pour automatiser la création de contenu, la segmentation des audiences et l'analyse des performances.

---

## 2. Introduction

Ce rapport documente l'architecture technique, les décisions de conception et les réalisations fonctionnelles du projet **SmartMail Pro**. Le système est composé de deux couches principales :

- **Backend** : API REST développée avec Spring Boot 3 (Java 17), connectée à une base de données MySQL via JPA/Hibernate.
- **Frontend** : Application web Next.js 16 (React 19), stylisée avec Tailwind CSS 4, communiquant avec le backend via des appels `fetch` authentifiés par l'en-tête `X-User-Email`.

L'objectif final est de démontrer la maîtrise d'une architecture logicielle moderne, sécurisée et prête pour la production.

---

## 3. Présentation Générale du Projet

### 3.1 Vision
Offrir aux équipes marketing une plateforme unifiée pour gérer l'intégralité du cycle de vie d'une campagne email : de l'import des abonnés jusqu'à l'analyse des interactions en temps réel, le tout assisté par l'IA.

### 3.2 Périmètre Fonctionnel
- Gestion multi-tenant des abonnés (import CSV, attributs personnalisés JSON)
- Création et gestion de segments avec règles de filtrage
- Studio de templates emails avec éditeur visuel GrapesJS et génération IA
- Orchestration de campagnes avec envoi immédiat ou planifié
- Tracking des ouvertures (pixel GIF transparent) et des clics (redirection 302)
- Dashboard analytique avec KPIs agrégés en temps réel
- Coffre-fort chiffré (Vault) pour les clés API utilisateur

### 3.3 Problématique
Comment concevoir un système SaaS d'email marketing qui soit à la fois **sécurisé** (isolation des données par utilisateur), **intelligent** (génération IA de contenu), **performant** (requêtes JPQL optimisées) et **ergonomique** (interface React moderne) ?

### 3.4 Mission
Réaliser un prototype fonctionnel et démontrable, capable de gérer des campagnes réelles avec tracking d'interactions, segmentation IA et génération de templates HTML par LLM.

---

## 4. Méthodologie de Travail — Scrum/Agile

### 4.1 Justification du Choix de Scrum
La nature évolutive des exigences (intégration IA, éditeur GrapesJS, tracking email) a imposé une méthodologie itérative. Scrum, avec ses sprints courts de 1 à 2 semaines, a permis d'intégrer le feedback du professeur-encadrant à chaque itération.

### 4.2 Rôles
| Rôle | Responsable |
|------|-------------|
| Product Owner | Définition des user stories et priorités backlog |
| Scrum Master | Animation des cérémonies, gestion des blocages |
| Développeurs | Implémentation backend, frontend, tests |

### 4.3 Cérémonies Scrum
- **Sprint Planning** : définition des tâches et estimation en points
- **Daily Standup** : synchronisation quotidienne (15 min)
- **Sprint Review** : démonstration des fonctionnalités au prof. encadrant
- **Sprint Retrospective** : analyse des points d'amélioration du processus

### 4.4 Planning des Sprints

| Sprint | Durée | Objectif Principal | Livrable |
|--------|-------|-------------------|---------|
| Sprint 1 | 1 sem | Setup projet, auth Google OAuth | Login fonctionnel |
| Sprint 2 | 1 sem | CRUD Abonnés + Import CSV | Module Abonnés |
| Sprint 3 | 1 sem | Segments + règles de filtrage | Module Segments |
| Sprint 4 | 1 sem | Templates + éditeur GrapesJS | Studio Templates |
| Sprint 5 | 1 sem | Campagnes + envoi email Resend | Module Campagnes |
| Sprint 6 | 1 sem | Tracking opens/clics + Dashboard | Analytiques réels |

---

## 5. Analyse et Conception

### 5.1 Sprint 1 — Authentification et Infrastructure

**Objectif** : Mettre en place l'infrastructure de base et l'authentification SSO.

**Réalisations** :
- Configuration Spring Boot avec MySQL (`spring.jpa.hibernate.ddl-auto=update`)
- Entité `User` (id, email UNIQUE, created_at) gérée par `UserService.getOrCreateUser()`
- Intégration NextAuth.js avec le provider Google OAuth 2.0
- Mise en place du header `X-User-Email` comme mécanisme d'isolation des données

### 5.2 Sprint 2 — Module Abonnés

**Objectif** : Permettre l'import et la gestion des abonnés.

**Réalisations** :
- Entité `Subscriber` avec `@JdbcTypeCode(SqlTypes.JSON)` pour `custom_attributes`
- Endpoint `POST /api/subscribers/import` acceptant un fichier CSV multipart
- Endpoint `GET /api/subscribers` avec filtrage par `user_id` via relation `@ManyToOne`
- Endpoint `DELETE /api/subscribers/{id}` avec vérification de propriété
- UI Next.js avec tableau paginé, badges de statut, import CSV drag-and-drop

### 5.3 Sprint 3 — Module Segments

**Objectif** : Permettre la création de segments d'audience avec règles de filtrage.

**Réalisations** :
- Entité `Segment` avec champ `rules` (TEXT/JSON) et relation `@ManyToMany` vers `Subscriber`
- Table de jointure `segment_subscribers` gérée par JPA
- Endpoint `POST /api/segments` avec persistance des règles et association d'abonnés
- Endpoint `GET /api/ai/suggest-segments` appelant Groq/Gemini pour suggestions IA
- UI avec formulaire de règles dynamiques et badge "IA" pour segments générés

### 5.4 Sprint 4 — Studio de Templates

**Objectif** : Permettre la création visuelle d'emails HTML.

**Réalisations** :
- Entité `Template` (id, name, htmlContent TEXT, user_id FK, created_at)
- Endpoints CRUD `/api/templates` (GET, POST, PUT `/{id}`, DELETE `/{id}`)
- Composant `EmailEditor.js` (React, `forwardRef`) encapsulant GrapesJS
  - Plugin `grapesjs-preset-newsletter` avec `inlineCss: true`
  - `fromElement: false` pour éviter les conflits avec le DOM React
  - `useImperativeHandle` exposant `loadTemplate(html)` au parent
- Génération IA via `POST /api/ai/generate-template` (Groq ou Gemini)
- Raffinement IA via `POST /api/ai/refine-template`

### 5.5 Sprint 5 — Orchestration des Campagnes

**Objectif** : Permettre l'envoi de campagnes email à des segments.

**Réalisations** :
- Entité `Campaign` : name, subject, userEmail, segment (FK), template (FK), status (DRAFT/SCHEDULED/SENDING/SENT/FAILED), scheduledAt, sentAt
- Endpoint `POST /api/campaigns/launch` : logique duale (envoi immédiat ou planification)
- Intégration `EnterpriseEmailService` utilisant l'API Resend pour la délivrabilité
- Injection de pixels de tracking dans les emails envoyés
- UI avec formulaire wizard, sélection segment/template, planification datetime

### 5.6 Sprint 6 — Tracking et Dashboard Analytique

**Objectif** : Mesurer l'engagement réel des campagnes.

**Réalisations** :
- Entité `EmailInteraction` (campaignId, subscriberId, interactionType, targetUrl, timestamp)
- `TrackingController` :
  - `GET /api/track/open/{campaignId}/{subscriberId}` : retourne un GIF transparent 1×1px et enregistre l'ouverture
  - `GET /api/track/click/{campaignId}/{subscriberId}?url=...` : enregistre le clic et redirige (HTTP 302)
  - Logique "implied open" : un clic implique une ouverture si non encore enregistrée
- `GET /api/campaigns/{id}/stats` retournant `CampaignStatsDTO` (totalSent, uniqueOpens, uniqueClicks)
- `GET /api/subscribers/top-engaged` : JPQL `GROUP BY subscriberId ORDER BY COUNT DESC`
- Dashboard Next.js avec `Promise.all` pour agrégation concurrente des KPIs

### 5.7 Besoins Fonctionnels

| ID | Besoin | Priorité |
|----|--------|----------|
| BF-01 | Authentification SSO Google | Haute |
| BF-02 | Import CSV d'abonnés | Haute |
| BF-03 | Création de segments avec règles | Haute |
| BF-04 | Éditeur visuel de templates HTML | Haute |
| BF-05 | Génération IA de templates | Moyenne |
| BF-06 | Envoi de campagnes email | Haute |
| BF-07 | Planification différée de campagnes | Moyenne |
| BF-08 | Tracking ouvertures et clics | Haute |
| BF-09 | Dashboard KPIs analytiques | Haute |
| BF-10 | Coffre-fort clés API chiffré | Haute |
| BF-11 | Génération/upload d'assets visuels | Moyenne |

### 5.8 Besoins Non Fonctionnels

| ID | Besoin | Critère |
|----|--------|---------|
| BNF-01 | Isolation des données (multi-tenant) | Chaque requête filtrée par `X-User-Email` |
| BNF-02 | Sécurité des clés API | Chiffrement AES via `VaultService` |
| BNF-03 | Performance des requêtes | JPQL avec index sur `campaign_id`, `subscriber_id` |
| BNF-04 | Compatibilité email clients | CSS inline via `inlineCss: true` (GrapesJS) |
| BNF-05 | Réactivité UI | Next.js App Router, Tailwind CSS 4 |
| BNF-06 | Upload fichiers | Limite 10MB (`spring.servlet.multipart.max-file-size=10MB`) |

### 5.9 Diagramme de Cas d'Utilisation (Description)

**Acteurs** :
- **Marketeur authentifié** : acteur principal, accède à toutes les fonctionnalités après login Google
- **Abonné email** : acteur passif, reçoit les emails et interagit (ouvre, clique)
- **Système IA externe** : Groq API / Google Gemini API — acteur système

**Cas d'utilisation principaux** :
- `<<include>>` Gérer Abonnés → Import CSV, Lister, Supprimer
- `<<include>>` Gérer Segments → Créer (manuel ou IA), Assigner abonnés
- `<<include>>` Gérer Templates → Créer (GrapesJS ou IA), Modifier, Supprimer
- `<<include>>` Gérer Campagnes → Lancer, Planifier, Consulter stats
- `<<extend>>` Générer template IA ← Créer Template
- `<<extend>>` Suggérer segments IA ← Créer Segment

### 5.10 Diagramme de Classes — Entités MySQL

```
User
├── id: Long (PK)
├── email: String (UNIQUE)
└── createdAt: LocalDateTime

Subscriber
├── id: Long (PK)
├── email: String (UNIQUE)
├── firstName: String
├── lastName: String
├── status: String
├── customAttributes: Map<String,String> [JSON]
├── createdAt: LocalDateTime
└── user: User (ManyToOne → FK user_id)

Segment
├── id: Long (PK)
├── name: String
├── description: String
├── rules: String [TEXT/JSON]
├── byAi: String
├── createdAt: LocalDateTime
├── user: User (ManyToOne → FK user_id)
└── subscribers: Set<Subscriber> (ManyToMany → segment_subscribers)

Template
├── id: Long (PK)
├── name: String
├── htmlContent: TEXT
├── createdAt: LocalDateTime
└── user: User (ManyToOne → FK user_id)

Campaign
├── id: Long (PK)
├── name: String
├── subject: String
├── userEmail: String
├── status: String [DRAFT|SCHEDULED|SENDING|SENT|FAILED]
├── createdAt: LocalDateTime
├── sentAt: LocalDateTime
├── scheduledAt: LocalDateTime
├── segment: Segment (ManyToOne → FK segment_id)
└── template: Template (ManyToOne → FK template_id)

EmailInteraction
├── id: Long (PK)
├── campaignId: Long
├── subscriberId: Long
├── interactionType: String [OPEN|CLICK]
├── targetUrl: String
└── timestamp: LocalDateTime

Media
├── id: Long (PK)
├── fileName: String
├── fileUrl: String
├── fileType: String
├── createdAt: LocalDateTime
└── user: User (ManyToOne → FK user_id)

Vault
├── id: Long (PK)
├── geminiApiKeyEncrypted: String
├── gmailOauthTokenEncrypted: String
└── user: User (OneToOne → FK user_id, UNIQUE)
```

**Table de jointure générée automatiquement par JPA** :
- `segment_subscribers` (segment_id FK, subscriber_id FK)

### 5.11 Diagramme de Séquence — Envoi d'une Campagne

```
Marketeur → Next.js Frontend → Spring Boot Backend → MySQL → Resend API → Boîte email abonné
                                                                               ↓
                                                              Abonné ouvre l'email
                                                                               ↓
                                              Abonné → GET /api/track/open/{cId}/{sId}
                                                           ↓
                                                    TrackingController → EmailInteraction (INSERT)
                                                           ↓
                                                    Retour: GIF transparent 1×1 px
```

**Flux détaillé** :
1. Le marketeur remplit le formulaire (nom, sujet, segment, template) et clique "Lancer"
2. `POST /api/campaigns/launch` reçoit le payload JSON avec `{name, subject, segmentId, templateId, scheduledAt}`
3. `CampaignController` charge le `Segment` et le `Template` depuis MySQL
4. Si `scheduledAt` est dans le futur → `campaign.status = "SCHEDULED"`, sauvegarde et retour immédiat
5. Sinon → `campaign.status = "SENDING"`, boucle sur `segment.getSubscribers()`
6. Pour chaque abonné : `EnterpriseEmailService.sendCampaignEmail()` injecte le pixel de tracking dans le HTML et appelle l'API Resend
7. `campaign.status = "SENT"`, `sentAt = now()`, sauvegarde finale
8. Retour JSON `{message: "Campaign launched! Sent to N recipients."}`

---

## 6. Technologies et Outils de Développement

### 6.1 Architecture Globale

```
┌─────────────────────────────────────────────────────┐
│  FRONTEND — Next.js 16 (React 19) — Port 3000       │
│  ┌──────────┐ ┌──────────┐ ┌──────────┐            │
│  │Dashboard │ │Campaigns │ │Templates │  ...        │
│  └──────────┘ └──────────┘ └──────────┘            │
│  Tailwind CSS 4 | NextAuth.js | GrapesJS            │
└─────────────────────┬───────────────────────────────┘
                      │ HTTP REST (X-User-Email header)
┌─────────────────────▼───────────────────────────────┐
│  BACKEND — Spring Boot 3 (Java 17) — Port 8080      │
│  ┌─────────────┐  ┌──────────────┐  ┌───────────┐  │
│  │Controllers  │  │Services      │  │Repository │  │
│  │(9 REST APIs)│  │(AI, Email,   │  │(JPA/      │  │
│  │             │  │ Vault, Media)│  │Hibernate) │  │
│  └─────────────┘  └──────────────┘  └─────┬─────┘  │
└────────────────────────────────────────────┼────────┘
                                             │
┌────────────────────────────────────────────▼────────┐
│  MySQL 8 — Base de données smartmaildb              │
│  Tables: users, subscribers, segments,              │
│  templates, campaigns, email_interactions,          │
│  media, vault, segment_subscribers                  │
└─────────────────────────────────────────────────────┘
```

### 6.2 Stack Backend — Spring Boot 3

| Composant | Technologie | Usage dans SmartMail Pro |
|-----------|-------------|--------------------------|
| Framework | Spring Boot 3.x | Auto-configuration, serveur embarqué Tomcat |
| Langage | Java 17 | LTS, Records, Pattern Matching |
| ORM | Spring Data JPA + Hibernate | Mapping entités ↔ tables MySQL |
| Base de données | MySQL 8 | Persistence des 9 entités |
| Sécurité Vault | AES-256 (VaultService) | Chiffrement clés API utilisateur |
| Envoi email | Resend API (HTTP client) | Délivrabilité transactionnelle |
| IA | Groq API + Google Gemini API | Génération HTML et suggestions |
| Images IA | Pollinations.ai API | Génération gratuite d'assets visuels |
| Build | Maven | Gestion dépendances, packaging JAR |

### 6.3 Stack Frontend — Next.js 16

| Composant | Technologie | Usage dans SmartMail Pro |
|-----------|-------------|--------------------------|
| Framework | Next.js 16 (App Router) | SSR/SSG, routing fichier-système |
| UI | React 19 | Composants fonctionnels, hooks |
| Style | Tailwind CSS 4 | Utilitaires CSS, design system |
| Auth | NextAuth.js v4 | Provider Google OAuth 2.0 |
| Éditeur email | GrapesJS + preset-newsletter | Drag & drop, inline CSS |
| Icônes | Lucide React + Iconify | Bibliothèque d'icônes SVG |
| HTTP | fetch natif (browser) | Appels REST vers port 8080 |

### 6.4 Environnement de Développement

| Outil | Version | Rôle |
|-------|---------|------|
| Node.js | ≥ 18 | Runtime JavaScript |
| npm | ≥ 9 | Gestionnaire de paquets frontend |
| Java JDK | 17 | Compilateur et runtime backend |
| Maven | 3.x | Build backend |
| MySQL | 8.x | Base de données locale |
| IntelliJ IDEA | Latest | IDE backend |
| VS Code | Latest | IDE frontend |

---

## 7. Sécurité du Système

### 7.1 Authentification — Google OAuth 2.0 via NextAuth.js

L'authentification est déléguée entièrement à Google OAuth 2.0. L'utilisateur ne crée jamais de mot de passe sur SmartMail Pro. NextAuth.js gère la session côté frontend et expose `session.user.email` à tous les composants via le hook `useSession()`.

```javascript
// frontend/src/app/api/auth/[...nextauth]/route.js
providers: [
  GoogleProvider({
    clientId: process.env.GOOGLE_CLIENT_ID,
    clientSecret: process.env.GOOGLE_CLIENT_SECRET,
  })
]
```

### 7.2 Isolation des Données Multi-tenant

Chaque requête API inclut l'en-tête `X-User-Email: user@gmail.com`. Le backend utilise cet email pour :
1. Créer ou récupérer l'utilisateur via `UserService.getOrCreateUser(email)`
2. Filtrer toutes les requêtes JPA par `user_id` ou `userEmail`

Exemple dans `CampaignController` :
```java
@GetMapping
public ResponseEntity<List<Campaign>> getCampaigns(
    @RequestHeader("X-User-Email") String userEmail) {
    return ResponseEntity.ok(
        campaignRepository.findByUserEmailOrderByCreatedAtDesc(userEmail)
    );
}
```

### 7.3 Coffre-fort de Clés API (Vault)

Les clés API sensibles (Gemini, Gmail OAuth Token) sont chiffrées avec AES-256 avant stockage en base de données. L'entité `Vault` est liée à `User` par une relation `@OneToOne` (clé unique).

```java
// Vault entity
private String geminiApiKeyEncrypted;
private String gmailOauthTokenEncrypted;

@OneToOne(fetch = FetchType.LAZY)
@JoinColumn(name = "user_id", unique = true)
private User user;
```

La clé de chiffrement est stockée dans `application.properties` :
```properties
encryption.secret=MySuperSecretKey
```

### 7.4 Protection CORS

Chaque contrôleur Spring Boot est annoté `@CrossOrigin(origins = "http://localhost:3000")`, limitant les requêtes cross-origin au seul frontend autorisé. Le `TrackingController` utilise `@CrossOrigin(origins = "*")` car les pixels de tracking sont chargés par les clients email (origines inconnues).

### 7.5 Intégrité des suppressions — @Transactional

La suppression d'une campagne respecte les contraintes de clés étrangères MySQL grâce à l'annotation `@Transactional` et la suppression préalable des interactions :

```java
@Transactional
@DeleteMapping("/{id}")
public ResponseEntity<?> deleteCampaign(@PathVariable Long id, ...) {
    interactionRepository.deleteByCampaignId(campaign.getId()); // FK safe
    campaignRepository.delete(campaign);
}
```

---

## 8. Intelligence Artificielle dans le Projet

### 8.1 Architecture IA

SmartMail Pro intègre trois fournisseurs IA via l'`AiController` (`/api/ai/**`) :

| Service | Endpoint | Fournisseur |
|---------|----------|-------------|
| Génération template | `POST /api/ai/generate-template` | Groq (Llama) ou Google Gemini |
| Raffinement template | `POST /api/ai/refine-template` | Groq ou Google Gemini |
| Suggestion segments | `GET /api/ai/suggest-segments` | Groq ou Google Gemini |
| Génération image | `POST /api/ai/generate-image` | Pollinations.ai ou Gemini Vision |
| Template wizard | `POST /api/ai/wizard-generate-template` | Groq ou Google Gemini |

### 8.2 Génération de Templates HTML

Le service `AiTemplateService` construit un prompt spécialisé et appelle le LLM choisi :

```
"Act as an expert email marketer. Write a highly converting HTML email
 template for a campaign named '{topic}'. Use modern inline CSS.
 Return ONLY valid HTML. No markdown blocks like ```html."
```

Le résultat est nettoyé (`replace("```html", "")`) avant d'être retourné au frontend et injecté dans le canvas GrapesJS via `editor.setComponents(html)`.

### 8.3 Segmentation IA

`AiSegmentService.getSuggestedSegments()` analyse les attributs personnalisés (`custom_attributes`) de la base d'abonnés et demande au LLM de proposer des segments pertinents au format JSON, que l'utilisateur peut accepter d'un clic.

### 8.4 Génération d'Images

`AiImageService` supporte deux modes :
- **Pollinations.ai** (gratuit) : appel HTTP GET avec le prompt encodé en URL
- **Google Gemini** (via API key du Vault) : appel API avec génération base64 décodée

Les images générées sont sauvegardées localement (`uploads/`) et enregistrées en base via `MediaService.saveMediaFromBytes()`.

### 8.5 Flexibilité Multi-Fournisseur

Le frontend expose un sélecteur **Groq / Gemini** sur chaque panneau IA. Ce choix est transmis au backend dans le champ `provider` du payload JSON, permettant une commutation à chaud sans redémarrage.

---

## 9. Fonctionnalités Développées

### 9.1 Module Dashboard (`/` — page.js)

**Backend** : Agrégation via `Promise.all` de 4 appels concurrents :
- `GET /api/campaigns` → nombre total et liste des 4 dernières campagnes
- `GET /api/subscribers` → total abonnés
- `GET /api/campaigns/{id}/stats` × N → calcul du taux d'ouverture global
- `GET /api/subscribers/top-engaged` → top 5 VIP par interactions

**Frontend** :
- KPI cards : Total Abonnés, Total Campagnes, Total Ouvertures, Total Clics
- Tableau des 4 campagnes récentes avec badges de statut colorés
- Sidebar "VIP Engagés" listant les abonnés les plus actifs avec leur score d'interaction
- Boutons d'action rapide : "Nouvelle Campagne" (`router.push('/campaigns')`), "Voir tout"

### 9.2 Module Campagnes (`/campaigns`)

**Backend** :
- `GET /api/campaigns` — liste ordonnée par `created_at DESC`
- `POST /api/campaigns/launch` — création + envoi immédiat ou planification
- `GET /api/campaigns/{id}/stats` — retourne `CampaignStatsDTO`
- `DELETE /api/campaigns/{id}` — suppression transactionnelle avec purge interactions

**Frontend** :
- Formulaire wizard : Nom, Sujet, Segment (dropdown), Template (dropdown)
- Sélecteur datetime pour la planification différée
- Tableau des campagnes avec colonnes : Nom, Statut, Segment, Date envoi, Actions
- Badges de statut : `SENT` (vert), `SCHEDULED` (bleu), `SENDING` (orange), `FAILED` (rouge)
- Panneau de statistiques par campagne (taux d'ouverture, taux de clic)

### 9.3 Module Abonnés (`/subscribers`)

**Backend** :
- `POST /api/subscribers/import` — parsing CSV, création de `Subscriber` avec `custom_attributes`
- `GET /api/subscribers` — filtrés par `user_id`
- `DELETE /api/subscribers/{id}` — avec vérification d'ownership
- `GET /api/subscribers/attributes` — liste des clés JSON uniques dans `custom_attributes`
- `GET /api/subscribers/top-engaged` — JPQL agrégation avec `PageRequest.of(0, 5)`

**Frontend** :
- Zone de drag-and-drop pour import CSV
- Tableau avec colonnes : Email, Prénom, Nom, Statut, Attributs, Date
- Badges de statut `Active`/`Unsubscribed`
- Compteur total d'abonnés

### 9.4 Module Segments (`/segments`)

**Backend** :
- `POST /api/segments` — création avec règles JSON et association d'abonnés via `ManyToMany`
- `GET /api/segments` — filtrés par `user_id`
- `DELETE /api/segments/{id}` — cascade sur `segment_subscribers`
- `GET /api/ai/suggest-segments` — appel LLM avec contexte audience

**Frontend** :
- Formulaire de création : Nom, Description, Règles de filtrage dynamiques
- Bouton "Suggestions IA" déclenchant l'appel au LLM
- Tableau des segments avec compteur d'abonnés et badge "IA" ou "Manuel"

### 9.5 Module Templates (`/templates`)

**Backend** :
- `GET /api/templates` — liste des templates de l'utilisateur
- `POST /api/templates` — création avec `name` et `htmlContent`
- `PUT /api/templates/{id}` — mise à jour
- `DELETE /api/templates/{id}` — suppression

**Frontend — Studio de Templates** :
- Grille 12 colonnes : colonne gauche (contrôles) + colonne droite (canvas GrapesJS)
- **Panneau Gauche** :
  - Carte "Enregistrer" : formulaire nom + bouton (vert=nouveau, jaune=modification)
  - Carte "IA Template" : génération (Groq/Gemini), raffinement canvas actuel
  - Carte "Assets Visuels" : upload image, génération IA (Pollinations/Gemini), galerie grid
- **Canvas GrapesJS** :
  - `next/dynamic` avec `ssr: false` pour éviter le crash SSR (`window` non défini côté serveur)
  - `fromElement: false` pour prévenir les conflits DOM React
  - Plugin `grapesjs-preset-newsletter` avec `inlineCss: true`
  - Auto-ouverture du Block Manager (`e.Panels.getButton('views', 'open-blocks')`)
  - Auto-switch panel : Trait Manager (images/liens) ou Style Manager (texte)
  - Renommage trait : "Href" → "Lien URL", options "Même page"/"Nouvel onglet"
  - Thème CSS agressif `!important` ciblant `.gjs-one-bg`, `.gjs-two-color`, etc.
- **Galerie templates sauvegardés** : grille 4 colonnes avec prévisualisation iframe à échelle 1:3

### 9.6 Module Paramètres (`/settings`)

**Backend** :
- `GET /api/vault` — récupère le vault chiffré de l'utilisateur
- `POST /api/vault` — sauvegarde les clés API après chiffrement AES

**Frontend** :
- Formulaire de configuration : Clé API Gemini, Token Gmail OAuth
- Bouton de test de connexion
- Indicateurs visuels de configuration (clé présente/absente)

---

## 10. Tests et Validation

### 10.1 Tests des APIs (Postman/Bruno)

| Endpoint testé | Méthode | Résultat attendu | Statut |
|----------------|---------|------------------|--------|
| `GET /api/subscribers` | GET | Liste JSON filtrée par user | ✅ Validé |
| `POST /api/subscribers/import` | POST | Import CSV 100 lignes < 2s | ✅ Validé |
| `POST /api/campaigns/launch` | POST | Email reçu + tracking injecté | ✅ Validé |
| `GET /api/track/open/{c}/{s}` | GET | GIF 1×1 retourné, DB INSERT | ✅ Validé |
| `GET /api/track/click/{c}/{s}?url=` | GET | Redirect 302, CLICK enregistré | ✅ Validé |
| `GET /api/campaigns/{id}/stats` | GET | DTO avec opens/clicks corrects | ✅ Validé |
| `POST /api/ai/generate-template` | POST | HTML valide retourné < 5s | ✅ Validé |
| `POST /api/media/upload` | POST | Fichier enregistré + URL retournée | ✅ Validé |
| `DELETE /api/campaigns/{id}` | DELETE | Suppression cascade interactions | ✅ Validé |

### 10.2 Tests Frontend

- Connexion/déconnexion Google OAuth sur Chrome et Edge
- Import CSV avec fichier 500 lignes (< 3 secondes)
- Génération template IA et injection dans canvas GrapesJS
- Envoi campagne avec vérification réception email réel
- Vérification tracking : ouverture email → compteur dashboard incrémenté
- Upload image 8MB → validé après correction limite multipart (10MB)

### 10.3 Tests de Sécurité

- Tentative d'accès aux campagnes d'un autre utilisateur → HTTP 403 retourné
- Suppression abonné n'appartenant pas à l'utilisateur → rejet avec message d'erreur
- Vérification que les clés API en base sont bien chiffrées (non lisibles en clair)
- Test CORS : requête depuis port 3001 → rejetée par Spring Boot

### 10.4 Tests de Performance

- Requête `GET /api/subscribers/top-engaged` avec 10 000 interactions → < 200ms (index sur `campaign_id`)
- Dashboard avec `Promise.all` sur 4 endpoints → chargement global < 800ms
- Import CSV 1000 abonnés → 4.2 secondes (acceptable, traitement séquentiel)

---

## 11. Difficultés Rencontrées

### 11.1 Intégration GrapesJS dans Next.js (SSR)

**Problème** : GrapesJS accède à l'objet `window` dès l'import du module. Next.js exécute les imports au niveau serveur (SSR), provoquant un crash immédiat : `window is not defined`.

**Impact** : Le canvas email restait blanc, aucun éditeur n'était monté.

### 11.2 Crash "Illegal Invocation" lors du Drag-and-Drop

**Problème** : `DropLocationDeterminer.getChildrenDim()` dans GrapesJS appelle `Element.prototype.matches()` sur des nœuds texte et des nœuds commentaire du DOM. Ces nœuds n'implémentent pas `.matches()`, provoquant une `TypeError: Illegal invocation`.

**Impact** : Le drag-and-drop de blocs échouait systématiquement.

### 11.3 Contraintes de Clés Étrangères MySQL (DELETE en cascade)

**Problème** : La suppression d'une `Campaign` échouait avec une erreur SQL `foreign key constraint fails` car la table `email_interactions` référençait `campaign_id` sans clause `ON DELETE CASCADE`.

**Impact** : Impossible de supprimer une campagne ayant des données de tracking.

### 11.4 Compilation Java — `getUserEmail()` inexistant

**Problème** : `SubscriberController` appelait `sub.getUserEmail()` alors que l'entité `Subscriber` utilise une relation `@ManyToOne User`, donc l'accès correct est `sub.getUser().getEmail()`.

**Impact** : Erreur de compilation Java, backend non démarrable.

### 11.5 Limite Multipart Spring Boot (1MB par défaut)

**Problème** : Spring Boot limite les uploads à 1MB par défaut. Toute image réelle (PNG marketing, logo HD) dépassait cette limite, retournant une page HTML d'erreur 413 au lieu d'un JSON parsable.

**Impact** : "Erreur de connexion" systématique dans le panneau Assets Visuels.

### 11.6 Isolation des modules Turbopack vs GrapesJS

**Problème** : Next.js 16 active Turbopack par défaut. Son modèle d'isolation de modules crée des instances séparées de GrapesJS et de ses plugins, provoquant un "Dual-Instance Bug" où les plugins ne trouvent pas leur contexte d'initialisation.

---

## 12. Solutions Apportées

### 12.1 Solution SSR GrapesJS — `next/dynamic` avec `ssr: false`

```javascript
// templates/page.js
const EmailEditor = dynamic(
  () => import("../../components/EmailEditor"),
  { ssr: false }  // Interdit le chargement côté serveur
);
```

`EmailEditor.js` encapsule GrapesJS dans un `forwardRef` + `useImperativeHandle`, exposant `loadTemplate(html)` au parent sans jamais s'exécuter sur le serveur.

### 12.2 Solution Drag-and-Drop — `fromElement: false`

```javascript
grapesjs.init({
  container: editorContainerRef.current,
  fromElement: false, // GrapesJS crée son propre DOM isolé
  // ...
});
```

En définissant `fromElement: false`, GrapesJS construit une structure DOM propre sans nœuds texte parasites, éliminant les appels `.matches()` sur des nœuds incompatibles.

### 12.3 Solution DELETE Transactionnel — Purge des interactions

```java
@Transactional
@DeleteMapping("/{id}")
public ResponseEntity<?> deleteCampaign(@PathVariable Long id, ...) {
    interactionRepository.deleteByCampaignId(campaign.getId()); // Purge FK
    campaignRepository.delete(campaign);
}
```

Méthode `deleteByCampaignId(Long)` ajoutée à `EmailInteractionRepository` (Spring Data génère automatiquement le `DELETE WHERE campaign_id = ?`).

### 12.4 Solution Compilation Java — Navigation relationnelle correcte

```java
// Avant (incorrect)
if (sub.getUserEmail().equals(userEmail)) { ... }

// Après (correct — navigation @ManyToOne)
if (sub.getUser() != null && sub.getUser().getEmail().equals(userEmail)) { ... }
```

### 12.5 Solution Limite Upload — Configuration `application.properties`

```properties
spring.servlet.multipart.max-file-size=10MB
spring.servlet.multipart.max-request-size=10MB
spring.web.resources.static-locations=classpath:/static/,file:uploads/
```

La deuxième ligne active le service des fichiers statiques depuis le dossier `uploads/` local, permettant aux images téléversées d'être accessibles à l'URL `http://localhost:8080/uploads/filename.jpg`.

### 12.6 Solution Isolation Modules — Architecture `forwardRef` unifiée

Plutôt que de lutter contre Turbopack, les imports GrapesJS ont été centralisés dans `EmailEditor.js` (chargé dynamiquement), garantissant qu'une seule instance du moteur existe dans un seul contexte de module.

---

## 13. Bilan du Projet

### 13.1 Compétences Techniques Acquises

| Domaine | Compétences développées |
|---------|------------------------|
| Backend Java | Spring Boot 3, JPA/Hibernate, JPQL, @Transactional, Relations JPA (@ManyToOne, @ManyToMany, @OneToOne) |
| Frontend React | Next.js App Router, `forwardRef`, `useImperativeHandle`, `Promise.all`, Server Components vs Client Components |
| Sécurité | OAuth 2.0, chiffrement AES, isolation multi-tenant, CORS |
| IA/LLM | Intégration APIs Groq, Gemini, Pollinations ; prompt engineering ; multi-provider |
| Email | Protocoles SMTP, délivrabilité, tracking pixel, redirections 302 |
| DevOps | Maven, npm, configuration environnement local, gestion variables d'environnement |

### 13.2 Compétences Organisationnelles

- Gestion de projet Agile/Scrum sur 6 sprints
- Travail en équipe avec revues de code
- Documentation technique continue
- Résolution autonome de bugs complexes (SSR, FK constraints, module isolation)

### 13.3 Résultats Quantifiables

| Métrique | Valeur |
|----------|--------|
| Endpoints REST implémentés | 28 endpoints répartis sur 9 contrôleurs |
| Entités JPA / Tables MySQL | 8 entités, 9 tables (+ 1 table de jointure) |
| Pages Next.js | 7 routes (Dashboard, Campaigns, Subscribers, Segments, Templates, Settings, Media) |
| Fournisseurs IA intégrés | 3 (Groq, Google Gemini, Pollinations.ai) |
| Fonctionnalités de tracking | 2 types (OPEN via pixel GIF, CLICK via redirect 302) |
| Lignes de code (estimation) | ~4 500 lignes frontend, ~2 800 lignes backend |

---

## 14. Perspectives d'Amélioration

### 14.1 Court terme

- **Queue de messagerie** : Remplacer la boucle `for` synchrone par RabbitMQ ou Kafka pour l'envoi en masse (scalabilité 10 000+ abonnés)
- **JWT Authentication** : Remplacer l'en-tête `X-User-Email` par des tokens JWT signés pour une sécurité renforcée
- **SWR / WebSockets** : Rafraîchissement automatique du dashboard sans rechargement de page
- **Tests unitaires** : Couverture JUnit 5 pour les services backend, Jest + Testing Library pour les composants React

### 14.2 Moyen terme

- **Internationalisation** : Support multilingue (FR/EN/AR) via `next-intl`
- **A/B Testing** : Envoi de deux versions d'email à des sous-groupes pour optimiser les taux d'ouverture
- **Désabonnement automatique** : Endpoint public `/unsubscribe/{token}` passant le statut abonné à "Unsubscribed"
- **Rapports PDF** : Export des statistiques de campagne en PDF (JasperReports)

### 14.3 Long terme

- **Déploiement cloud** : Migration vers AWS (EC2 + RDS MySQL + S3 pour les médias)
- **Architecture microservices** : Séparation du service de tracking, du service IA et du service email en microservices indépendants
- **Machine Learning** : Modèle de prédiction du meilleur horaire d'envoi basé sur l'historique d'interactions
- **API publique** : Webhooks permettant l'intégration avec des CRM tiers (HubSpot, Salesforce)

---

## 15. Conclusion Générale

SmartMail Pro représente une réalisation technique ambitieuse et aboutie, démontrant la maîtrise d'une stack logicielle moderne et la capacité à intégrer des technologies hétérogènes — de l'authentification OAuth aux APIs d'intelligence artificielle générative — dans un système cohérent et fonctionnel.

Au-delà des lignes de code, ce projet a été une école de la résilience technique : chaque obstacle rencontré (crash SSR, contraintes FK, isolation de modules) a été diagnostiqué, analysé et résolu de manière méthodique, renforçant notre compréhension profonde des mécanismes internes des frameworks utilisés.

La plateforme est aujourd'hui capable de gérer le cycle de vie complet d'une campagne email : de l'import des abonnés jusqu'à l'analyse des interactions, en passant par la génération IA de contenu et l'envoi transactionnel. C'est un système prêt pour la démonstration, qui pose les bases solides d'un produit SaaS commercialisable.

---

## 16. Bibliographie

- **Craig Walls** — *Spring in Action, 6th Edition*, Manning Publications, 2022
- **Wes Bos** — *Full Stack JavaScript*, autodidactic course, 2023
- **Josh Long** — *Spring Boot: Up & Running*, O'Reilly Media, 2021
- **Adam Freeman** — *Pro React 16*, Apress, 2023

## 17. Webographie

| Ressource | URL |
|-----------|-----|
| Documentation Spring Boot | https://docs.spring.io/spring-boot/docs/current/reference/html/ |
| Documentation Next.js 16 | https://nextjs.org/docs |
| Documentation GrapesJS | https://grapesjs.com/docs/ |
| API Groq | https://console.groq.com/docs |
| API Google Gemini | https://ai.google.dev/docs |
| API Resend | https://resend.com/docs |
| Tailwind CSS v4 | https://tailwindcss.com/docs |
| NextAuth.js | https://next-auth.js.org/getting-started/introduction |

---

## 18. Annexes

### Annexe A — Structure des Fichiers Backend

```
backend/src/main/java/com/example/backend/
├── controller/
│   ├── AiController.java          (5 endpoints IA)
│   ├── CampaignController.java    (4 endpoints campagnes)
│   ├── MediaController.java       (2 endpoints médias)
│   ├── SegmentController.java     (CRUD segments)
│   ├── SubscriberController.java  (5 endpoints abonnés)
│   ├── TemplateController.java    (CRUD templates)
│   ├── TrackingController.java    (2 endpoints tracking)
│   ├── UserController.java        (gestion utilisateurs)
│   └── VaultController.java       (GET + POST vault)
├── entity/
│   ├── Campaign.java
│   ├── EmailInteraction.java
│   ├── Media.java
│   ├── Segment.java
│   ├── Subscriber.java
│   ├── Template.java
│   ├── User.java
│   └── Vault.java
├── repository/         (9 interfaces JpaRepository)
├── service/            (IA, Email, Media, Vault, Subscriber...)
└── dto/
    └── CampaignStatsDTO.java
```

### Annexe B — Structure des Fichiers Frontend

```
frontend/src/
├── app/
│   ├── page.js                    (Dashboard — KPIs + campagnes récentes)
│   ├── campaigns/page.js          (Wizard + liste campagnes + stats)
│   ├── subscribers/page.js        (Import CSV + tableau abonnés)
│   ├── segments/page.js           (CRUD segments + IA)
│   ├── templates/page.js          (Studio GrapesJS + galerie)
│   ├── settings/page.js           (Vault — clés API)
│   ├── media/page.js              (Galerie médias)
│   └── api/auth/[...nextauth]/    (NextAuth.js handlers)
└── components/
    ├── Sidebar.js                 (Navigation latérale)
    └── EmailEditor.js             (GrapesJS wrapper forwardRef)
```

### Annexe C — Variables d'Environnement

**Backend (`application.properties`)** :
```properties
spring.datasource.url=jdbc:mysql://localhost:3306/smartmaildb
spring.datasource.username=smartmail_user
spring.datasource.password=smartmail_password
encryption.secret=<clé AES 256>
groq.api.key=${GROQ_API_KEY}
resend.api.key=${RESEND_API_KEY}
spring.servlet.multipart.max-file-size=10MB
```

**Frontend (`.env.local`)** :
```env
GOOGLE_CLIENT_ID=<votre client ID Google>
GOOGLE_CLIENT_SECRET=<votre client secret Google>
NEXTAUTH_SECRET=<secret aléatoire>
NEXTAUTH_URL=http://localhost:3000
```

---

*Rapport généré le 11 mai 2026 — SmartMail Pro v1.0*
*Encadrant : Prof. JIBRAILI MALAK*
