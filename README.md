# shopin-api

<p align="center">
  <img src="https://raw.githubusercontent.com/ErickBeLike/shopin-api/main/assets/ShopIn-recorted.png" alt="ShopIn API Logo" width="250"/>
</p>

## E-commerce Project (API-First)

This is the API project for an e-commerce platform, built with an **API-First** approach using **Spring Boot**. The goal is to create a robust and scalable API that will serve as the backbone for a complete online store application, demonstrating skills in back-end development, business logic, and security.

The front-end of this project will be developed separately and will consume this API.

---

## Technologies Used

* **Programming Language:** Java
* **Back-end Framework:** Spring Boot
* **Database:** PostgreSQL / MySQL
* **Dependency Management:** Maven
* **API Documentation:** Swagger (OpenAPI)
* **Security:** JWT (JSON Web Tokens)
* **(Optional) AI:** Python
* **External Services:**
    * **Cloudinary:** For image storage and transformation.
    * **MailerSend / Ethereal / MailTrap:** For transactional email delivery.

---

## Identity & Security Module - Feature Deep Dive

The foundation of this application is a production-grade user management and security module, engineered to be both robust and flexible.

### Authentication
* **Multiple Authentication Methods:** Standard Email/Password, **Google (OIDC)**, and **Facebook (OAuth2)**.
* **Social Identity System (Discord-Style):** Users can choose a non-unique `username`, and the system assigns a unique `discriminator` (e.g., `Erick#0001`), drastically improving the registration user experience.
* **Two-Phase OAuth2 Registration:** A user-friendly flow that allows new social-login users to choose their `username` before the account is finalized.
* **Social Account Linking:** Logged-in users can link multiple OAuth2 providers to a single primary account, preventing account duplication.

### Session Management
* **JWT Issuance:** Implements JSON Web Tokens (`accessToken` & `refreshToken`) for secure, stateless communication.
* **Secure Refresh Token Storage:** `refreshToken` is stored securely in an `HttpOnly` cookie to mitigate XSS attacks.
* **Session Invalidation:** Any critical security event (password change, email change, 2FA modification) automatically invalidates all other active sessions to protect against takeovers.

### Account Security
* **Multi-Method Two-Factor Authentication (2FA):**
    * Support for **Authenticator Apps (TOTP)**.
    * Support for **Email Codes**.
    * Users can select their **preferred 2FA method**.
    * High-security, two-step enable/disable flows requiring password and/or code confirmation.
* **Comprehensive Password Management:**
    * Full **password reset flow** via email codes.
    * Ability for OAuth-only users to **create a local password**.
    * Secure **social account unlinking** with password confirmation.

---

## Modules and Progress

Here you can check the current status of the API development. The modules are designed to be implemented in stages, following an agile development methodology.

* [x] **Module 1 - Identity & Security:** _`Completed`_
    * All features described in the section above.
* [ ] **Module 2 - General CRUDs:** _`In progress`_
    * User Management (Registration, Login, Profile)
    * Product Management
    * Order Management
* [ ] **Module 3 - AI Implementation:** _`To do`_
    * Product recommendation engine
    * Data analysis for predictions
* [ ] **Module 4 - Payment Gateway:** _`To do`_
    * Stripe Integration (Sandbox Mode)
    * Secure transaction handling
    * Webhooks for payment notifications

---

### Quick Start Guide

To clone and run this project on your local machine:

1.  Clone the repository:
    `git clone https://github.com/ErickBeLike/shopin-api.git`
2.  Navigate to the project directory:
    `cd shopin-api`
3.  Configure your environment variables in a `.env` file (e.g., for the database and JWT key).
4.  Run the application ;)

Once the application is running, you can access the API documentation via Swagger UI.

---

### API Documentation

The API is fully documented with **Swagger UI**. Once the application is running, you can access the documentation in your browser at:

_`Coming Soon`_

Here you will find all available endpoints, their request parameters, and example responses.

---

### Contributions

Feel free to clone and explore the code. Any suggestions or improvements are welcome.
