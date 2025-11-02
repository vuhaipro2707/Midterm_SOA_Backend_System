# Tuition Payment Microservices System (SOA Midterm Project)

This project implements a Service-Oriented Architecture (SOA) for a comprehensive Tuition Payment System using **Spring Boot (Java 21)** for backend services and **FastAPI (Python)** for the API Gateway. The system utilizes Docker Compose for container orchestration, PostgreSQL for persistence, and Redis for caching and OTP management.

## 🌟 Key Features

* **SOA/Microservices Architecture:** Decoupled services for Auth, Customer Management, Tuition, Payment Processing, OTP, and Mail.
* **API Gateway (FastAPI):** Centralized entry point, handling request routing, JWT validation, and injecting `X-Customer-Id` for authentication context.
* **Distributed Transaction:** Implements the **Saga Pattern** (Choreography-based) in the Payment Processor service for reliable two-phase commitment (Debit Customer -> Update Tuition Status -> Record Transaction), including automated **compensation/rollback** logic (Credit back) upon failure.
* **Security:** JWT-based authentication using **RSA asymmetric keys** (`RS256`).
* **Email & OTP:** Uses a separate Mail Service for asynchronous email notifications and an OTP Service (backed by Redis) for payment confirmation.

## 🛠️ Getting Started (Local Setup)

This project uses Docker Compose to set up all services and dependencies (PostgreSQL, Redis) instantly.

### Prerequisites

* Docker and Docker Compose (v2.x)
* Python 3.x (to run `key.py` for JWT setup)

### 1. Configure Environment Variables

Create a file named `.env` in the project root directory using the `.env example` content and replace the placeholders with your actual Gmail details. You must use a Gmail **App Password** for `MAIL_PASSWORD`.

```bash:.env example:.env
MAIL_USERNAME=your_sender_email@gmail.com
MAIL_PASSWORD=your_app_specific_password # Use App Password for Gmail
```

### 2. Generate JWT Keys

The system uses RSA keys for JWT signing (Auth Service) and validation (API Gateway). The following Python script will generate the `private_key.pem` and `public_key.pem` in the correct locations:

```bash
python3 key.py
```

You should see confirmation messages that both keys were created successfully.

### 3. Run the Services

Use Docker Compose to build all service images and start the entire system:

```bash
docker compose up --build -d
```

Verify that all containers are running:
```bash
docker compose ps
```

All services should eventually show `Status: Up`.

### 4. Default Test User

A default customer is created in the `Auth Service` and `Customer Management Service` databases upon startup:

| Field | Value | Note |
| :--- | :--- | :--- |
| **Username** | `user1` | |
| **Password** | `123` | |
| **Initial Balance** | `5000000` VND | |
| **Sample Tuition ID** | `1` (for student 523H1101) | |

---

## 🌐 API Endpoints

The API Gateway is accessible at `http://localhost:8080`.

**Full Interactive Documentation (Swagger UI):** `http://localhost:8080/docs`

| Service | Method | Endpoint | Description |
| :--- | :--- | :--- | :--- |
| **Auth** | `POST` | `/auth/login` | Authenticate and receive `jwt_token` (HttpOnly Cookie). |
| **Auth** | `POST` | `/auth/logout`| Clear the JWT cookie to log out. |
| **Customer** | `GET` | `/customer/info` | Get authenticated customer's details and balance. |
| **Customer** | `GET` | `/customer/balance` | Get authenticated customer's current available balance. |
| **Tuition** | `GET` | `/tuition/studentId/{id}` | Retrieve all tuition records for a specific student ID. |
| **Tuition** | `GET` | `/tuition/id/{id}` | Retrieve a specific tuition record by ID. |
| **Payment** | `POST` | `/payment/initiate` | **Saga Step 1:** Check eligibility and send OTP to email. |
| **Payment** | `POST` | `/payment/confirm` | **Saga Step 2:** Validate OTP, execute distributed transaction (Debit, Update Tuition, Record Tx). |
| **Payment** | `POST` | `/payment/resend` | Generate and resend a new OTP. |

---

## 🛑 Stopping the Services

To stop and remove all containers and networks, run:

```bash
docker compose down
```

To perform a clean shutdown, removing generated keys and persistent cache, run:

```bash
docker compose down -v
rm -rf ./.m2cache/
rm -f ./ApiGateway/app/public_key.pem ./auth-service/src/main/resources/keys/private_key.pem
```

## 🧑‍💻 Development Mode

For development with hot-reloading (Spring Boot) and volume mounting for code changes, use the dedicated development Compose file:

```bash
docker compose -f docker-compose.yml -f docker-compose.dev.yml up --build -d
```
This mode mounts the source code and uses a shared Maven cache for faster iteration.
