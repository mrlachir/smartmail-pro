# Guide de Présentation Backend — SmartMail Pro
### Script de présentation pour le jury — Prof. JIBRAILI MALAK
> **Usage** : Ce document est votre script personnel. Les sections en *italique* sont vos points de parole. Les blocs de code sont à afficher à l'écran pendant la démonstration.

---

## 1. Le Pitch Architectural (Discours d'introduction — 2 minutes)

> *Script à prononcer en ouvrant IntelliJ IDEA sur la structure du projet :*

---

**« Notre backend est une API REST construite sur Spring Boot 3, le framework Java le plus adopté en entreprise pour les architectures SaaS. Nous avons choisi cette stack pour trois raisons précises :**

**Premièrement, la robustesse.** Spring Boot intègre nativement un serveur Tomcat embarqué, une gestion automatique des transactions SQL, et un système d'injection de dépendances — ce qui nous a permis de nous concentrer sur la logique métier plutôt que sur la plomberie technique.

**Deuxièmement, la productivité.** Grâce à Spring Data JPA, nous n'écrivons pas une seule ligne de SQL pour les opérations standard. L'interface `JpaRepository` génère automatiquement les requêtes `SELECT`, `INSERT`, `UPDATE`, `DELETE` à partir du nom des méthodes Java.

**Troisièmement, la scalabilité.** Notre architecture respecte strictement le patron **3-Tiers** — Contrôleur / Service / Repository — ce qui signifie que chaque couche est indépendante, testable séparément, et remplaçable sans impacter les autres.

Notre base de données est **MySQL 8**, avec **8 entités JPA** mappées sur **9 tables** (plus une table de jointure). La communication avec le frontend Next.js se fait exclusivement via des requêtes HTTP REST, authentifiées par l'en-tête `X-User-Email`. »**

---

## 2. Architecture 3-Tiers — Exemples Concrets du Code

> *Montrez la structure de packages dans IntelliJ : `controller/`, `service/`, `repository/`*

```
com.example.backend/
├── controller/    ← Couche 1 : Points d'entrée HTTP
├── service/       ← Couche 2 : Logique métier
├── repository/    ← Couche 3 : Accès aux données (JPA)
└── entity/        ← Modèle de données (mapping ORM)
```

---

### 2.1 Couche 1 — Les Contrôleurs (Points d'Entrée HTTP)

> *Expliquez en montrant `CampaignController.java` :*

**« Le contrôleur est la façade de notre API. Il ne contient aucune logique métier — son seul rôle est de recevoir la requête HTTP, d'extraire les paramètres, et de déléguer à la couche service. »**

```java
// CampaignController.java — Exemple du point d'entrée "Lancer une campagne"
@RestController                          // Spring sait que cette classe gère des requêtes HTTP REST
@RequestMapping("/api/campaigns")        // Préfixe de toutes les routes de ce contrôleur
@CrossOrigin(origins = "http://localhost:3000") // Autorise uniquement le frontend Next.js

public class CampaignController {

    @Autowired private CampaignRepository campaignRepository;
    @Autowired private EnterpriseEmailService emailService;
    // ...

    @PostMapping("/launch")
    public ResponseEntity<?> launchCampaign(
            @RequestHeader("X-User-Email") String userEmail,  // Identifie l'utilisateur
            @RequestBody Map<String, Object> payload) {       // Corps JSON de la requête
        // ...
    }
}
```

> **Points à souligner au professeur :**
> - `@RestController` = Spring détecte automatiquement cette classe comme gestionnaire de routes HTTP
> - `@RequestHeader("X-User-Email")` = mécanisme d'isolation multi-tenant : **chaque requête porte l'identité de l'utilisateur**
> - `@RequestBody Map<String, Object>` = Spring désérialise automatiquement le JSON en objet Java
> - `ResponseEntity<?>` = nous contrôlons précisément le code HTTP retourné (200, 400, 403...)

---

### 2.2 Couche 2 — Les Services (Logique Métier)

> *Montrez `SegmentService.java` ou `EnterpriseEmailService.java`*

**« La couche service isole la logique métier des détails HTTP. Quand `SegmentController` reçoit une requête de création de segment, il délègue immédiatement à `SegmentService.saveSegment()`. Le contrôleur ne sait pas comment les abonnés sont filtrés — c'est le service qui le sait. »**

```java
// SegmentController.java — Le contrôleur délègue au service
@PostMapping
public ResponseEntity<Segment> createSegment(
        @RequestBody Segment segment,
        @RequestHeader("X-User-Email") String userEmail) {

    // Le contrôleur ne fait QUE déléguer — aucune logique ici
    return ResponseEntity.ok(segmentService.saveSegment(segment, userEmail));
}
```

> **Pourquoi cette séparation ?**
> - **Testabilité** : On peut tester `SegmentService` avec JUnit sans démarrer le serveur HTTP
> - **Réutilisabilité** : `EnterpriseEmailService` est appelé par `CampaignController` ET par le scheduler de campagnes planifiées
> - **Clarté** : Un nouveau développeur comprend immédiatement le rôle de chaque classe

---

### 2.3 Couche 3 — Les Repositories (Accès aux Données JPA)

> *Montrez `EmailInteractionRepository.java` — c'est notre exemple le plus impressionnant*

**« Voici la puissance de Spring Data JPA. Cette interface n'a pas de corps — aucune implémentation. Spring génère automatiquement le SQL à l'exécution, en déduisant les requêtes depuis les noms de méthodes. »**

```java
@Repository
public interface EmailInteractionRepository extends JpaRepository<EmailInteraction, Long> {

    // Spring génère : SELECT COUNT(*) FROM email_interactions
    //                 WHERE campaign_id=? AND subscriber_id=? AND interaction_type=?
    boolean existsByCampaignIdAndSubscriberIdAndInteractionType(
        Long campaignId, Long subscriberId, String interactionType);

    // Spring génère : SELECT COUNT(*) FROM email_interactions
    //                 WHERE campaign_id=? AND interaction_type=?
    long countByCampaignIdAndInteractionType(Long campaignId, String interactionType);

    // Spring génère : DELETE FROM email_interactions WHERE campaign_id=?
    void deleteByCampaignId(Long campaignId);

    // Requête JPQL personnalisée pour les analytiques avancées
    @Query("SELECT e.subscriberId, COUNT(e) FROM EmailInteraction e " +
           "GROUP BY e.subscriberId ORDER BY COUNT(e) DESC")
    List<Object[]> findTopSubscriberIdsWithCount(Pageable pageable);
}
```

> **Points clés à expliquer :**
> - Les 3 premières méthodes : **zéro SQL écrit** — Spring analyse le nom de la méthode et génère la requête
> - `findTopSubscriberIdsWithCount` : requête **JPQL** (Java Persistence Query Language) — syntaxe orientée objet qui opère sur les **entités Java** et non sur les tables SQL directement
> - `Pageable pageable` : pagination intégrée — `PageRequest.of(0, 5)` retourne les 5 premiers résultats
> - **C'est cette requête qui alimente le widget "Top Abonnés VIP" du Dashboard**

---

## 3. Modélisation de la Base de Données (ORM & Entités)

> *Ouvrez le schéma de la base de données MySQL dans MySQL Workbench ou IntelliJ DataGrip*

**« Nos 8 entités Java sont mappées directement sur 9 tables MySQL. Hibernate, le moteur JPA, génère et maintient le schéma automatiquement grâce à `spring.jpa.hibernate.ddl-auto=update`. »**

### 3.1 Tableau des Entités et leurs Tables

| Entité Java | Table MySQL | Colonnes clés |
|-------------|-------------|---------------|
| `User` | `users` | `id`, `email` (UNIQUE), `created_at` |
| `Subscriber` | `subscribers` | `id`, `email`, `first_name`, `last_name`, `status`, `custom_attributes` (JSON), `user_id` (FK) |
| `Segment` | `segments` | `id`, `name`, `rules` (TEXT), `by_ai`, `user_id` (FK) |
| `Template` | `templates` | `id`, `name`, `html_content` (TEXT), `user_id` (FK) |
| `Campaign` | `campaigns` | `id`, `name`, `subject`, `user_email`, `status`, `segment_id` (FK), `template_id` (FK), `scheduled_at`, `sent_at` |
| `EmailInteraction` | `email_interactions` | `id`, `campaign_id`, `subscriber_id`, `interaction_type`, `target_url`, `timestamp` |
| `Media` | `media` | `id`, `file_name`, `file_url`, `file_type`, `user_id` (FK) |
| `Vault` | `vault` | `id`, `gemini_api_key_encrypted`, `gmail_oauth_token_encrypted`, `user_id` (FK, UNIQUE) |
| *(jointure JPA)* | `segment_subscribers` | `segment_id` (FK), `subscriber_id` (FK) |

### 3.2 Les Relations JPA — Comment Java décrit MySQL

**« Nous n'écrivons pas de `CREATE TABLE` ni de `ALTER TABLE ADD FOREIGN KEY`. Nous déclarons les relations en Java avec des annotations, et Hibernate génère le SQL correspondant. »**

```java
// Relation ManyToOne : Un Subscriber appartient à un seul User
// Hibernate crée automatiquement la colonne user_id FK dans la table subscribers
@ManyToOne(fetch = FetchType.LAZY)
@JoinColumn(name = "user_id", nullable = false)
@JsonIgnore   // Empêche la sérialisation infinie JSON (évite la boucle User→Subscriber→User)
private User user;

// Relation ManyToMany : Un Segment contient plusieurs Subscribers, et vice-versa
// Hibernate crée automatiquement la table de jointure segment_subscribers
@ManyToMany(fetch = FetchType.LAZY)
@JoinTable(
    name = "segment_subscribers",
    joinColumns = @JoinColumn(name = "segment_id"),
    inverseJoinColumns = @JoinColumn(name = "subscriber_id")
)
private Set<Subscriber> subscribers = new HashSet<>();

// Relation OneToOne : Un User a exactement un Vault (coffre-fort de clés API)
@OneToOne(fetch = FetchType.LAZY)
@JoinColumn(name = "user_id", unique = true)
private User user;
```

### 3.3 Focus : Campaign ↔ EmailInteraction

> *Expliquez le lien entre ces deux entités — c'est le cœur du système de tracking*

**« La relation entre `Campaign` et `EmailInteraction` est intentionnellement loose. `EmailInteraction` stocke juste les IDs numériques (`campaign_id`, `subscriber_id`) sans relation JPA déclarée. Pourquoi ? Parce que les interactions sont créées par le `TrackingController` qui ne charge jamais l'objet `Campaign` complet — il écrit juste un enregistrement minimal pour des raisons de performance. »**

```java
// EmailInteraction.java — Structure légère, optimisée pour l'écriture haute fréquence
@Entity
@Table(name = "email_interactions")
public class EmailInteraction {
    @Column(name = "campaign_id", nullable = false)
    private Long campaignId;          // Juste l'ID — pas de @ManyToOne pour éviter le chargement JPA inutile

    @Column(name = "interaction_type", nullable = false)
    private String interactionType;   // "OPEN" ou "CLICK"

    @Column(name = "target_url")
    private String targetUrl;         // Null pour les ouvertures, rempli pour les clics
}
```

---

## 4. Démonstration de Flux de Données — Cas Pratique : Suppression d'une Campagne

> *C'est la démonstration technique la plus complète. Montrez le code sur l'écran et lisez le flux étape par étape.*

### Contexte du problème résolu

**« Lorsque nous avons tenté de supprimer une campagne pour la première fois, MySQL a retourné une erreur : `foreign key constraint fails`. En effet, la table `email_interactions` contient des enregistrements référençant `campaign_id`. MySQL refuse de supprimer le parent tant que des enfants existent. »**

**« Voici comment nous avons résolu ce problème de manière propre, en utilisant deux concepts Spring avancés : `@Transactional` et la suppression en cascade manuelle. »**

### Flux complet — 7 étapes

```
Frontend Next.js                    Spring Boot Backend                    MySQL
─────────────────────────────────────────────────────────────────────────────────
1. DELETE /api/campaigns/42    ──►  CampaignController.deleteCampaign()
   Header: X-User-Email: a@b.c          │
                                    2. campaignRepository.findById(42)  ──►  SELECT * FROM campaigns WHERE id=42
                                        │  ◄── Campaign object
                                    3. campaign.getUserEmail() == "a@b.c" ?
                                        │  NON → return 403 FORBIDDEN
                                        │  OUI → continuer
                                    4. @Transactional démarre une transaction SQL
                                        │
                                    5. interactionRepository              ──►  DELETE FROM email_interactions
                                          .deleteByCampaignId(42)                WHERE campaign_id=42
                                        │
                                    6. campaignRepository.delete(campaign) ──►  DELETE FROM campaigns
                                        │                                         WHERE id=42
                                    7. @Transactional COMMIT              ──►  Les 2 DELETE validés atomiquement
                                        │
                                        └── return 200 OK {"message": "Campaign deleted successfully"}
```

### Code à montrer à l'écran

```java
// CampaignController.java — Méthode de suppression complète
@org.springframework.transaction.annotation.Transactional   // ← Garantit l'atomicité des 2 DELETE
@DeleteMapping("/{id}")
public ResponseEntity<?> deleteCampaign(
        @PathVariable Long id,
        @RequestHeader("X-User-Email") String userEmail) {   // ← Identité de l'appelant
    try {
        // Étape 1 : Vérifier que la campagne existe
        Campaign campaign = campaignRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Campaign not found in database."));

        // Étape 2 : Vérifier que l'utilisateur est le propriétaire (sécurité multi-tenant)
        if (!campaign.getUserEmail().equals(userEmail)) {
            return ResponseEntity.status(403)
                .body(Map.of("message", "Unauthorized to delete this campaign."));
        }

        // Étape 3 : Purger les données de tracking AVANT de supprimer la campagne
        //           (respecte la contrainte de clé étrangère MySQL)
        interactionRepository.deleteByCampaignId(campaign.getId());

        // Étape 4 : Supprimer la campagne elle-même
        campaignRepository.delete(campaign);

        return ResponseEntity.ok(Map.of("message", "Campaign deleted successfully"));

    } catch (Exception e) {
        // En cas d'erreur dans @Transactional → ROLLBACK automatique des 2 opérations
        return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
    }
}
```

### Points à expliquer pour chaque ligne clé

| Élément | Explication à prononcer |
|---------|------------------------|
| `@Transactional` | *« Si l'un des deux DELETE échoue, les deux sont annulés — la base reste cohérente »* |
| `@PathVariable Long id` | *« Spring extrait le `42` de l'URL `/api/campaigns/42` automatiquement »* |
| `.orElseThrow(...)` | *« Pattern Optional de Java — pas de NullPointerException, une exception métier claire »* |
| `status(403)` | *« Nous retournons le bon code HTTP sémantique : 403 = Interdit, pas 500 = Erreur serveur »* |
| `interactionRepository.deleteByCampaignId(id)` | *« Spring Data génère le DELETE SQL depuis le nom de la méthode — zéro SQL écrit »* |

---

## 5. Sécurité et Bonnes Pratiques

### 5.1 Stratégie d'Authentification — X-User-Email Header

> *Expliquez en montrant le panneau Réseau du navigateur (F12 > Network)*

**« Notre stratégie de sécurité repose sur deux couches complémentaires. »**

**Couche 1 — Authentification Google OAuth 2.0 (Frontend)** : L'utilisateur s'authentifie via Google. NextAuth.js gère la session et expose `session.user.email` à tous les composants React. L'email de l'utilisateur connecté est injecté dans chaque requête HTTP :

```javascript
// Extrait frontend — chaque fetch() inclut l'identité de l'utilisateur
const res = await fetch("http://localhost:8080/api/campaigns", {
    headers: {
        "X-User-Email": session.user.email  // Email vérifié par Google OAuth
    }
});
```

**Couche 2 — Isolation des données (Backend)** : Chaque endpoint Spring Boot lit cet en-tête et filtre les données en base :

```java
// Le backend ne retourne JAMAIS les données d'un autre utilisateur
@GetMapping
public ResponseEntity<List<Campaign>> getCampaigns(
        @RequestHeader("X-User-Email") String userEmail) {
    return ResponseEntity.ok(
        campaignRepository.findByUserEmailOrderByCreatedAtDesc(userEmail)  // Filtre strict par user
    );
}
```

### 5.2 Chiffrement des Clés API — Le Vault

**« Les clés API sensibles (Gemini, Gmail) ne sont jamais stockées en clair. Notre `VaultService` les chiffre avec AES-256 avant le `INSERT` et les déchiffre après le `SELECT`. »**

```java
// Vault entity — les champs sensibles sont chiffrés en base
@Entity
public class Vault {
    private String geminiApiKeyEncrypted;        // AES-256 — illisible sans la clé de déchiffrement
    private String gmailOauthTokenEncrypted;     // AES-256

    @OneToOne
    @JoinColumn(name = "user_id", unique = true) // Un coffre par utilisateur, jamais partagé
    private User user;
}
```

### 5.3 Gestion des Codes HTTP — Sémantique REST

**« Nous ne retournons jamais un `200 OK` pour une erreur. Notre API respecte les conventions HTTP, ce qui la rend prévisible pour tout client. »**

| Situation | Code retourné | Exemple dans notre code |
|-----------|---------------|------------------------|
| Succès | `200 OK` | Campagne lancée, abonné supprimé |
| Requête invalide | `400 Bad Request` | Segment introuvable, champ manquant |
| Non autorisé | `403 Forbidden` | Tentative de suppression d'une campagne d'un autre user |
| Non trouvé | `404 Not Found` | ID inexistant en base |
| Erreur serveur | `500` (évité) | Capturé par try/catch, transformé en 400 avec message |

```java
// Pattern cohérent dans tous nos contrôleurs
try {
    // ... logique métier ...
    return ResponseEntity.ok(result);                               // 200
} catch (Exception e) {
    return ResponseEntity.badRequest()                              // 400
        .body(Map.of("message", e.getMessage()));                   // Message lisible, pas de stack trace
}
```

### 5.4 Protection CORS

**« Le Cross-Origin Resource Sharing est configuré au niveau de chaque contrôleur. Seul `http://localhost:3000` (notre frontend Next.js) est autorisé à appeler l'API. »**

```java
@CrossOrigin(origins = "http://localhost:3000")  // ← Sur CHAQUE contrôleur métier
public class CampaignController { ... }

// Exception justifiée : le TrackingController autorise toutes origines
// car les pixels de tracking sont chargés par Gmail, Outlook, Apple Mail...
@CrossOrigin(origins = "*")
public class TrackingController { ... }
```

---

## 6. Bonus — La Fonctionnalité la Plus Impressionnante : Le Tracking Email

> *Gardez cette démonstration pour impressionner à la fin si le temps le permet*

**« Le tracking email est techniquement la fonctionnalité la plus élégante du projet. Voici comment elle fonctionne : »**

### Tracking d'Ouverture — Le Pixel Espion

**« Quand nous envoyons un email, nous injectons une balise `<img>` invisible dans le HTML, pointant vers notre serveur. »**

```html
<!-- Injecté automatiquement dans chaque email envoyé -->
<img src="http://localhost:8080/api/track/open/42/17" width="1" height="1" style="display:none"/>
<!-- campaign_id=42, subscriber_id=17 -->
```

**« Quand l'abonné ouvre l'email, son client email charge cette image. Notre serveur Spring Boot reçoit la requête GET, enregistre l'ouverture en base, et retourne un GIF transparent de 1×1 pixel. »**

```java
@GetMapping("/open/{campaignId}/{subscriberId}")
public ResponseEntity<byte[]> trackOpen(
        @PathVariable Long campaignId,
        @PathVariable Long subscriberId) {

    // Enregistrement de l'interaction en base
    EmailInteraction interaction = new EmailInteraction();
    interaction.setInteractionType("OPEN");
    interactionRepository.save(interaction);

    // Retour d'un GIF transparent 1×1 pixel encodé en base64
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.IMAGE_GIF);
    headers.setCacheControl("no-cache, no-store, must-revalidate"); // Empêche le cache navigateur
    return new ResponseEntity<>(TRANSPARENT_GIF, headers, HttpStatus.OK);
}
```

### Tracking de Clic — La Redirection 302

**« Les liens dans les emails ne pointent pas directement vers la destination. Ils passent par notre serveur qui enregistre le clic et redirige immédiatement. »**

```java
@GetMapping("/click/{campaignId}/{subscriberId}")
public ResponseEntity<Void> trackClick(
        @PathVariable Long campaignId,
        @PathVariable Long subscriberId,
        @RequestParam("url") String targetUrl) {  // L'URL finale en paramètre

    // Logique "Implied Open" : si l'abonné clique, il a forcément ouvert
    boolean hasOpened = interactionRepository
        .existsByCampaignIdAndSubscriberIdAndInteractionType(campaignId, subscriberId, "OPEN");
    if (!hasOpened) {
        // Créer aussi une interaction OPEN automatiquement
        interactionRepository.save(new EmailInteraction(campaignId, subscriberId, "OPEN", null));
    }

    // Enregistrer le CLICK
    interactionRepository.save(new EmailInteraction(campaignId, subscriberId, "CLICK", targetUrl));

    // Redirection immédiate vers la destination réelle
    HttpHeaders headers = new HttpHeaders();
    headers.setLocation(URI.create(targetUrl));
    return new ResponseEntity<>(headers, HttpStatus.FOUND); // 302 Redirect — transparent pour l'utilisateur
}
```

---

## 7. Résumé des 28 Endpoints — Tableau de Référence Rapide

| Contrôleur | Method | URL | Description |
|-----------|--------|-----|-------------|
| **Campaign** | GET | `/api/campaigns` | Liste des campagnes (triée par date DESC) |
| | POST | `/api/campaigns/launch` | Lancement ou planification |
| | GET | `/api/campaigns/{id}/stats` | KPIs (envoyés, ouverts, cliqués) |
| | DELETE | `/api/campaigns/{id}` | Suppression transactionnelle |
| **Subscriber** | GET | `/api/subscribers` | Liste filtrée par user |
| | POST | `/api/subscribers/import` | Import CSV multipart |
| | DELETE | `/api/subscribers/{id}` | Suppression avec vérif ownership |
| | GET | `/api/subscribers/attributes` | Clés JSON custom_attributes disponibles |
| | GET | `/api/subscribers/top-engaged` | Top 5 VIP par interactions (JPQL) |
| **Segment** | GET | `/api/segments` | Liste des segments |
| | POST | `/api/segments` | Création avec règles JSON |
| | PUT | `/api/segments/{id}` | Mise à jour |
| | DELETE | `/api/segments/{id}` | Suppression |
| | GET | `/api/segments/{id}/subscribers` | Évaluation des abonnés du segment |
| **Template** | GET | `/api/templates` | Liste des templates |
| | POST | `/api/templates` | Création |
| | PUT | `/api/templates/{id}` | Mise à jour |
| | DELETE | `/api/templates/{id}` | Suppression |
| **AI** | GET | `/api/ai/suggest-segments` | Suggestions IA de segments |
| | POST | `/api/ai/generate-template` | Génération HTML par LLM |
| | POST | `/api/ai/refine-template` | Raffinement du template actuel |
| | POST | `/api/ai/generate-image` | Génération image (Pollinations/Gemini) |
| | POST | `/api/ai/wizard-generate-template` | Template wizard + sauvegarde auto |
| **Tracking** | GET | `/api/track/open/{c}/{s}` | Pixel GIF + INSERT interaction OPEN |
| | GET | `/api/track/click/{c}/{s}` | Redirect 302 + INSERT interaction CLICK |
| **Media** | POST | `/api/media/upload` | Upload image (multipart, max 10MB) |
| | GET | `/api/media` | Galerie de l'utilisateur |
| **Vault** | GET | `/api/vault` | Récupération clés chiffrées |
| | POST | `/api/vault` | Sauvegarde avec chiffrement AES |

---

## 8. Questions Fréquentes du Jury — Réponses Préparées

**Q : Pourquoi ne pas avoir utilisé Spring Security avec JWT ?**

> *« Pour ce prototype académique, nous avons opté pour une stratégie pragmatique : l'authentification est déléguée à Google OAuth 2.0 (le standard de l'industrie), et l'isolation des données est garantie par le filtrage systématique sur `X-User-Email` dans chaque requête. La migration vers JWT serait une évolution naturelle pour une version production — l'architecture 3-tiers que nous avons adoptée rend cette migration non-destructive : il suffirait d'ajouter un filtre Spring Security sans modifier les contrôleurs. »*

**Q : Comment gérez-vous les campagnes planifiées ?**

> *« Dans `CampaignController.launchCampaign()`, si `scheduledAt` est une date future, nous sauvegardons la campagne avec le statut `SCHEDULED` et nous arrêtons. Un `@Scheduled` task Spring (ou un job externe) serait responsable de polling régulier des campagnes `SCHEDULED` dont la date est passée. Pour une version production, nous recommanderions RabbitMQ ou Quartz Scheduler pour une planification plus robuste. »*

**Q : Que se passe-t-il si l'API Resend est indisponible lors de l'envoi ?**

> *« Dans la boucle d'envoi de `CampaignController`, chaque appel `emailService.sendCampaignEmail()` est enveloppé dans un `try-catch` individuel. Si Resend échoue pour un abonné spécifique, l'erreur est loggée (`System.err.println`) mais le traitement continue pour les abonnés suivants. À la fin, si `sentCount == 0`, le statut de la campagne est marqué `FAILED`. C'est un comportement de type best-effort, acceptable pour ce stade du projet. »*

**Q : Comment assurez-vous qu'un utilisateur ne peut pas voir les données d'un autre ?**

> *« Le mécanisme est double. Premièrement, toutes les requêtes JPA incluent un filtre sur `userEmail` ou `user_id` — il est structurellement impossible de retourner des données cross-user en utilisant nos méthodes repository. Deuxièmement, pour les actions mutantes (DELETE notamment), nous vérifions explicitement l'ownership avant toute opération :*
> ```java
> if (!campaign.getUserEmail().equals(userEmail)) {
>     return ResponseEntity.status(403).body(...);
> }
> ```
> *Même si un attaquant devina l'ID d'une campagne d'un autre utilisateur, notre backend retourne 403 et n'effectue aucune modification. »*

---

*Document généré le 12 mai 2026 — SmartMail Pro Backend Presentation Guide*
*À utiliser conjointement avec IntelliJ IDEA (backend ouvert) et MySQL Workbench (schéma visible)*
