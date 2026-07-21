# **ENTERPRISE DIGITAL INSURANCE & CARE PLATFORM**

### **(FHIR-Enabled, Microservice-Based, Kenya & SHA-Aligned)**

---

## **1\. Executive Overview (CEO / Steering Committee)**

### **Objective**

Build a **national-scale digital insurance platform** that:

* Digitizes insurance cards (member wallet)

* Executes eligibility, authorization, claims, and payments

* Integrates providers, insurers, SHA-style workflows

* Is interoperable using **FHIR R4**

* Is secure, auditable, and regulator-ready

### **Strategic Value**

* Positions the organization as a **platform owner**, not a software vendor

* Enables ecosystem integrations (hospitals, labs, EMRs, mobile apps)

* Reduces fraud, processing time, and operational cost

* Creates long-term data and transaction leverage

### **Non-Goals (Explicitly Out of Scope – Phase 1\)**

* Full EMR replacement

* Advanced clinical decision support

* AI-driven diagnostics

---

## **2\. Architectural Principles (Technical Lead)**

1. **Microservice Ownership**  
    Each service owns its data, logic, and FHIR resources.

2. **FHIR as the Canonical Model**  
    Internal models may vary, but interoperability is always FHIR-aligned.

3. **Event-Driven by Default**  
    Kafka is the backbone for state change propagation.

4. **Security First**  
    Zero-trust, consent-driven, audit-by-design.

5. **Progressive Scalability**  
    Single-cluster → multi-cluster without redesign.

---

## **3\. High-Level System Architecture**

### **Logical Layers**

\[ Mobile App / Provider Portal / Admin Portal \]  
                    ↓  
\[ API Gateway \]  
                    ↓  
 \[ Domain Microservices (FHIR-based) \]  
                    ↓  
 \[ Kafka | PostgreSQL | Object Storage \]  
                    ↓  
 \[ Observability & Audit \]

---

## **4\. Microservices Landscape (Authoritative)**

| Service | Core Responsibility |
| :---- | :---- |
| Identity & IAM | Authentication, authorization, roles |
| Patient Service | Member identity & demographics |
| Provider Service | Practitioners, facilities |
| Organization Service | Insurers, hospitals, SHA |
| Eligibility Service | Cover validation & limits |
| Encounter Service | Care context anchor |
| Claims Service | Claim lifecycle & adjudication |
| Payments Service | Settlements & reconciliation |
| FHIR Gateway | External interoperability |
| Consent Service | Data sharing permissions |
| Document Service | Clinical & financial artifacts |
| Audit & Compliance | Regulatory traceability |
| Notification Service | SMS, email, push |

---

## **5\. FHIR Resource Ownership by Service (Summary)**

(Condensed; expanded later)

| Service | FHIR Resources |
| :---- | :---- |
| Patient | Patient, RelatedPerson |
| Provider | Practitioner, PractitionerRole |
| Organization | Organization, Location |
| Encounter | Encounter, EpisodeOfCare |
| Eligibility | Coverage, InsurancePlan |
| Claims | Claim, ClaimResponse |
| Payments | PaymentNotice, PaymentReconciliation |
| Clinical | Observation, Condition, Procedure |
| Consent | Consent |
| Documents | DocumentReference, Binary |
| Audit | AuditEvent, Provenance |

---

## **6\. Resource-to-Database Schema Mapping (Technical)**

### **Example: Patient Service**

**FHIR: Patient**

| FHIR Field | DB Column | Notes |
| :---- | :---- | :---- |
| Patient.id | patient\_id (UUID) | Primary key |
| identifier.value | national\_id | SHA / National ID |
| name.family | last\_name | Indexed |
| name.given | first\_name |  |
| telecom.value | phone | Encrypted |
| gender | gender | Enum |
| birthDate | dob |  |
| active | is\_active |  |

**Design Rule:**

* One FHIR resource \= one primary table

* References stored as foreign UUIDs

* JSONB for extensions (PostgreSQL)

---

### **Example: Coverage (Eligibility Service)**

| FHIR Field | DB Column |
| :---- | :---- |
| Coverage.id | coverage\_id |
| beneficiary.reference | patient\_id |
| payor.reference | insurer\_id |
| status | status |
| period.start | start\_date |
| period.end | end\_date |
| class.value | plan\_tier |

---

## **7\. FHIR Reference Interaction Matrix**

This matrix informs **diagram arrows and service dependencies**.

| Source Resource | References | Target Service |
| :---- | :---- | :---- |
| Encounter | Patient | Patient Service |
| Encounter | Practitioner | Provider Service |
| Encounter | Organization | Organization Service |
| Claim | Coverage | Eligibility Service |
| Claim | Encounter | Encounter Service |
| ClaimResponse | Claim | Claims Service |
| PaymentNotice | ClaimResponse | Payments Service |
| Observation | Encounter | Encounter Service |
| DocumentReference | Patient | Patient Service |
| Consent | Patient | Patient Service |

**Rule:**  
 No service directly queries another service’s database—only references.

---

## **8\. Kafka Event Model (Lifecycle-Driven)**

### **Core Topics (Canonical)**

| Topic | Trigger |
| :---- | :---- |
| patient.created | New member onboarded |
| coverage.verified | Eligibility confirmed |
| encounter.started | Provider interaction |
| claim.submitted | Claim raised |
| claim.adjudicated | Claim processed |
| payment.released | Funds approved |
| consent.updated | Access rules changed |
| document.uploaded | Artifact stored |

---

### **Example: Claim Lifecycle**

claim.submitted  
    ↓  
eligibility.checked  
    ↓  
claim.adjudicated  
    ↓  
payment.released

Each event:

* Immutable

* Versioned

* Includes resource ID \+ metadata

* Audit logged automatically

---

## **9\. Infrastructure & Clustering Model**

### **Baseline Production Cluster (Confirmed Viable)**

**Per Node**

* 8 vCPU

* 12 GB RAM

* 240 GB SSD RAID 10

* 6 TB bandwidth

### **Initial Cluster Topology**

Cluster A (Core Services)  
\- API Gateway  
\- Patient, Provider, Organization  
\- Eligibility

Cluster B (Transactions)  
\- Claims  
\- Payments  
\- Kafka

Cluster C (Data & Compliance)  
\- PostgreSQL  
\- Document Storage  
\- Audit

Allows **horizontal growth without re-architecture**.

---

## **10\. Security & Compliance (CEO / Legal)**

* OAuth2 /OIDC

* Role-based access (Admin, Provider, Insurer)

* Consent-driven data access (FHIR Consent)

* Full audit trail (AuditEvent \+ Provenance)

* SHA-style claim transparency

* Data residency-ready

---

## **11\. Sprint & Delivery Implications (Project Manager)**

### **Phase 1 (Foundational – 6–8 weeks)**

* IAM

* Patient, Provider, Organization

* Coverage & Eligibility

* API Gateway

### **Phase 2 (Transactional – 6 weeks)**

* Encounter

* Claims

* Payments

* Kafka orchestration

### **Phase 3 (Interoperability – 4 weeks)**

* FHIR Gateway

* Consent

* Documents

* External integrations

---

## **12\. Financial & Resource Drivers (CEO)**

### **Primary Cost Centers**

* Engineering (Backend, Mobile, DevOps)

* Infrastructure (VPS, backups, observability)

* Security & compliance tooling

* SMS / notification services

### **Cost Optimization Built-In**

* Stateless services

* Shared Kafka

* External object storage

* Progressive scaling (no upfront overprovisioning)

---

## **13\. Strategic Closing**

This architecture:

* Is **defensible** to regulators

* Is **credible** to enterprise insurers

* Is **scalable** without redesign

* Creates **long-term platform leverage**

It deliberately avoids overengineering while preserving authority and control.

**14\. Pricing**

| Item | Price per unit | Total | Renewable per unit | Years |
| :---- | :---- | :---- | :---- | :---- |
| Domain name**bimacare.org** | $7.48/yr | $7.48/yr | $12.98/yr | 1 |
| Premium DNS | $4.88/yr | $4.88/yr | $9.98/yr | 1 |
| SSL | $8.99/yr | $17.98 | $9.99/yr | 2 |
| Virtual Machine (VPS) | $89.24 | $89.24 | $89.24 | 1/4 |
|  |  |  |  |  |
| **Total** |  | **$119.58 or $101.6 (Without the SSL)** |  |  |

[Additional resources](https://afya-akili-digital.lovable.app/)

