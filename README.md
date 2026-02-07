# URL Shortener Service

A production-ready URL shortening service built with Spring Boot, featuring JWT authentication, analytics tracking, rate limiting, and API key management.

## Features

- **URL Shortening**: Create short URLs with custom aliases or auto-generated codes
- **Click Analytics**: Track clicks with detailed statistics (browser, device, OS, country, referer)
- **User Authentication**: JWT-based authentication with refresh tokens
- **API Key Management**: Generate API keys for programmatic access
- **Rate Limiting**: Protect the API with configurable rate limits (60/min, 1000/hour)
- **Admin Dashboard**: Platform statistics and user management for admins
- **Docker Support**: Ready for containerized deployment

## Tech Stack

- **Backend**: Java 17, Spring Boot 3.2.1
- **Database**: PostgreSQL (H2 for testing)
- **Security**: Spring Security, JWT (JJWT)
- **Documentation**: OpenAPI 3.0 (Swagger UI)
- **Containerization**: Docker, Docker Compose

## Getting Started

### Prerequisites

- Java 17 or higher
- Maven 3.8+
- PostgreSQL 15+ (or use Docker)
- Docker & Docker Compose (optional)

### Running with Docker (Recommended)

```bash
# Start the application with PostgreSQL
docker-compose up -d

# View logs
docker-compose logs -f app

# Stop the application
docker-compose down
```

The application will be available at `http://localhost:8080/api`

### Running Locally

1. **Start PostgreSQL** (or configure H2 for development)

2. **Configure the application**
   ```bash
   # Set environment variables
   export SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/urlshortener
   export SPRING_DATASOURCE_USERNAME=postgres
   export SPRING_DATASOURCE_PASSWORD=your_password
   export JWT_SECRET=your-secret-key-min-32-characters
   export APP_BASE_URL=http://localhost:8080/api/r
   ```

3. **Run the application**
   ```bash
   ./mvnw spring-boot:run
   ```

## API Documentation

Once the application is running, access the Swagger UI at:
- **Swagger UI**: `http://localhost:8080/api/swagger-ui.html`
- **OpenAPI JSON**: `http://localhost:8080/api/api-docs`

## API Endpoints

### Authentication
| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/auth/register` | Register a new user |
| POST | `/api/auth/login` | Login and get tokens |
| POST | `/api/auth/refresh` | Refresh access token |
| POST | `/api/auth/logout` | Logout and invalidate token |

### URL Management
| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/urls/shorten` | Create a shortened URL |
| GET | `/api/urls` | Get all URLs (authenticated) |
| GET | `/api/urls/{shortCode}` | Get URL details |
| PUT | `/api/urls/{shortCode}` | Update a URL |
| DELETE | `/api/urls/{shortCode}` | Delete a URL |

### Redirect
| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/r/{shortCode}` | Redirect to original URL |

### Analytics
| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/analytics/{shortCode}` | Get URL analytics |

### API Keys
| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/api-keys` | Create new API key |
| GET | `/api/api-keys` | List all API keys |
| PATCH | `/api/api-keys/{id}/revoke` | Revoke an API key |
| DELETE | `/api/api-keys/{id}` | Delete an API key |

### User Profile
| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/users/me` | Get current user profile |
| PUT | `/api/users/me` | Update profile |
| DELETE | `/api/users/me` | Delete account |

### Admin (Requires ADMIN role)
| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/admin/stats` | Get platform statistics |
| GET | `/api/admin/users` | List all users |
| GET | `/api/admin/users/{id}` | Get user details |
| PATCH | `/api/admin/users/{id}/toggle-status` | Enable/disable user |
| DELETE | `/api/admin/users/{id}` | Delete user |

## Usage Examples

### Register a new user
```bash
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "email": "user@example.com",
    "password": "securepassword",
    "name": "John Doe"
  }'
```

### Create a short URL
```bash
curl -X POST http://localhost:8080/api/urls/shorten \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <your-token>" \
  -d '{
    "url": "https://example.com/very/long/url",
    "customAlias": "mylink",
    "expirationDays": 30
  }'
```

### Get analytics
```bash
curl -X GET http://localhost:8080/api/analytics/mylink \
  -H "Authorization: Bearer <your-token>"
```

## Configuration

Key configuration options in `application.yml`:

| Property | Description | Default |
|----------|-------------|---------|
| `app.base-url` | Base URL for short links | `http://localhost:8080/api/r` |
| `app.short-url-length` | Length of generated short codes | `7` |
| `app.url.default-expiration-days` | Default URL expiration | `365` |
| `app.url.max-custom-alias-length` | Max custom alias length | `20` |
| `jwt.expiration` | JWT token expiration (ms) | `86400000` (24h) |
| `jwt.refresh-expiration` | Refresh token expiration (ms) | `604800000` (7d) |

## Project Structure

```
src/main/java/com/urlshortener/
├── controller/          # REST API controllers
├── service/             # Business logic
├── repository/          # Data access layer
├── entity/              # JPA entities
├── dto/                 # Request/Response DTOs
├── security/            # JWT & authentication
├── config/              # Application configuration
└── exception/           # Custom exceptions
```

## Security Features

- **JWT Authentication**: Secure token-based authentication
- **Password Encryption**: BCrypt password hashing
- **Rate Limiting**: Prevents API abuse
- **API Key Hashing**: Secure storage of API keys
- **Input Validation**: Request validation and sanitization
- **CORS Configuration**: Configurable cross-origin settings

## License

This project is licensed under the MIT License.
