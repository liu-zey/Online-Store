# Online Store Project Documentation

## 1. Project Purpose and Technology Stack

**Project Purpose:**

The project is an online store backend application. It is built using a microservices architecture, leveraging Spring Cloud for this purpose. The primary goal is to provide APIs for managing users and authentication, with potential for future expansion to include other online store functionalities.

**Technology Stack:**

*   **Programming Language:** Java (version 17)
*   **Frameworks:**
    *   Spring Boot (version 3.1.5): For building the core application and RESTful APIs.
    *   Spring Cloud (version 2022.0.4): For building microservices and enabling cloud-native features.
    *   Spring Cloud Alibaba (version 2022.0.0.0): For integration with Alibaba Cloud services, specifically Nacos for service discovery and configuration.
*   **Data Persistence:**
    *   MyBatis (mybatis-spring-boot-starter version 3.0.2): As the Object-Relational Mapping (ORM) framework for database interaction.
    *   MySQL (mysql-connector-j version 8.0.33): As the relational database.
*   **Caching:**
    *   Redis (Jedis client version 4.3.1, Spring Boot Starter Data Redis): For caching data, primarily user sessions/tokens, to improve performance.
*   **Service Discovery and Configuration:**
    *   Nacos (spring-cloud-starter-alibaba-nacos-config, spring-cloud-starter-alibaba-nacos-discovery, nacos-client version 2.2.0): Used for service registration, discovery, and distributed configuration management.
*   **Build Tool:** Maven
*   **Other Key Libraries:**
    *   Spring Boot Starter Web: For building RESTful APIs.
    *   Spring Boot Starter Actuator: For monitoring and managing the application.
    *   Spring Boot Starter Validation: For input validation using Jakarta Bean Validation.
    *   Spring Boot Starter AOP: For Aspect-Oriented Programming.
    *   Jackson JSR310 (jackson-datatype-jsr310): For Java 8+ date/time serialization/deserialization.
    *   MyBatis TypeHandlers JSR310 (mybatis-typehandlers-jsr310 version 1.0.2): For handling Java 8+ date/time types with MyBatis.
    *   Spring Cloud Starter Bootstrap: To ensure bootstrap configuration (e.g., for Nacos) is loaded early in the application startup.

## 2. Core Application Layers and Components

The project follows a standard layered architecture common in Spring Boot applications:

- **`com.example.onlinestore.OnlineStoreApplication.java`**:
    - **Purpose**: This is the main entry point of the Spring Boot application.
    - **Functionality**: It uses `@SpringBootApplication` to enable auto-configuration, component scanning, and other Spring Boot features. `@MapperScan("com.example.onlinestore.mapper")` is used to discover MyBatis mapper interfaces. `@RefreshScope` allows for refreshing beans in the application context, often used with Spring Cloud Config (like Nacos) for dynamic configuration updates.

- **`com.example.onlinestore.config`**:
    - **Purpose**: This package contains configuration classes for various parts of the application.
    - **Key Classes**:
        - `MyBatisConfig.java`: Configures MyBatis, setting up the `SqlSessionFactory` and enabling underscore-to-camel-case mapping for database columns.
        - `RedisConfig.java`: Configures the Redis connection by providing a `StringRedisTemplate` bean.
        - `WebConfig.java`: Configures web-related aspects, such as registering the `AuthInterceptor` to handle authentication for protected API paths.
        - `NacosConfig.java`: Conditionally enables Nacos service discovery based on application properties.
        - Other files like `LocalConfigProperties.java`, `MessageConfig.java`, `RestTemplateConfig.java`, `ValidationConfig.java`, and `YamlPropertySourceFactory.java` handle other specific configurations.

- **`com.example.onlinestore.controller` (Controller Layer)**:
    - **Purpose**: This package holds Spring MVC controllers that handle incoming HTTP requests and route them to appropriate services.
    - **Key Classes**:
        - `AuthController.java`: Manages authentication-related requests, specifically the `/api/auth/login` endpoint. It uses `UserService` to perform login logic.
        - `UserController.java`: Handles user-related requests, such as listing users (`GET /api/users`). It demonstrates the use of custom annotations like `@RequireAdmin` for access control and `@ValidateParams` for request validation.

- **`com.example.onlinestore.service`** and **`com.example.onlinestore.service.impl` (Service Layer)**:
    - **Purpose**: The `service` package defines interfaces for business logic, while `service.impl` provides their concrete implementations.
    - **Key Classes**:
        - `UserService.java` (interface): Defines contracts for user-related operations like login, listing users, and retrieving a user by token.
        - `UserServiceImpl.java` (implementation): Implements `UserService`. It interacts with `UserMapper` for database operations, `StringRedisTemplate` for caching user tokens, and potentially external services (indicated by `RestTemplate` and a `userServiceBaseUrl` property).

- **`com.example.onlinestore.mapper` (Data Access Layer)**:
    - **Purpose**: This package contains MyBatis mapper interfaces for database interactions. These interfaces define methods that map to SQL statements.
    - **Key Classes**:
        - `UserMapper.java`: An interface defining methods for CRUD operations on the `User` entity, such as `findByUsername`, `insertUser`, `updateUserToken`, `findAllWithPagination`, and `countTotal`. SQL statements are typically defined in corresponding XML files (e.g., `UserMapper.xml` located in `src/main/resources/mapper/`).

- **`com.example.onlinestore.dto` (Data Transfer Objects)**:
    - **Purpose**: Contains classes used to transfer data between layers, especially for API request and response bodies.
    - **Key Classes**:
        - `LoginRequest.java`: Represents the data for a login request (username, password).
        - `LoginResponse.java`: Represents the data returned after a successful login (token, expiry time).
        - `UserVO.java` (View Object): Represents user data sent in API responses, often a subset or formatted version of the `User` model.
        - `PageResponse.java`, `UserPageRequest.java`: Used for handling paginated list requests and responses.
        - `ErrorResponse.java`: A generic structure for returning error messages.

- **`com.example.onlinestore.model` (Domain Model)**:
    - **Purpose**: Contains entity classes representing data structures, often corresponding to database tables.
    - **Key Classes**:
        - `User.java`: Represents a user in the system with properties like `id`, `username`, `token`, `tokenExpireTime`, `createdAt`, and `updatedAt`.

- **`com.example.onlinestore.interceptor` (Cross-cutting Concerns)**:
    - **Purpose**: Contains Spring MVC interceptors for performing pre-processing or post-processing on HTTP requests.
    - **Key Classes**:
        - `AuthInterceptor.java`: Intercepts requests to `/api/**` (excluding `/api/auth/login`). It checks for an `X-Token` header, validates the token using `UserService`, and sets the current user in `UserContext`.

- **`com.example.onlinestore.aspect` (Cross-cutting Concerns)**:
    - **Purpose**: Contains Aspect-Oriented Programming (AOP) aspects to implement cross-cutting concerns like security and validation.
    - **Key Classes**:
        - `AdminAuthAspect.java`: Works with the `@RequireAdmin` annotation. Before a method annotated with `@RequireAdmin` is executed, this aspect checks if the current user (from `UserContext`) is an administrator.
        - `ValidationAspect.java`: Works with the `@ValidateParams` annotation. It uses Jakarta Bean Validation to validate arguments of methods annotated with `@ValidateParams`.

- **`com.example.onlinestore.annotation` (Custom Annotations)**:
    - **Purpose**: Defines custom annotations used to apply specific behaviors or metadata.
    - **Key Classes**:
        - `RequireAdmin.java`: A method-level annotation indicating that the method requires administrator privileges.
        - `ValidateParams.java`: A method-level annotation that triggers parameter validation for the annotated method.

- **`com.example.onlinestore.context` (Request Context)**:
    - **Purpose**: Provides a way to manage contextual information on a per-request basis.
    - **Key Classes**:
        - `UserContext.java`: Uses a `ThreadLocal` to store the `User` object for the current request, allowing easy access to the authenticated user.

## 3. Key Functionalities

### 3.1. User Authentication

*   **Login Process**:
    *   The client sends a POST request with username and password to `/api/auth/login`.
    *   `AuthController` handles this request and calls `UserService.login()`.
    *   `UserServiceImpl.login()`:
        1.  Checks if the username matches the configured admin username (`admin.auth.username` from `application.yml`). If so, it validates the password against `admin.auth.password`.
        2.  If not an admin, it makes a POST request to an external user authentication service (URL configured via `service.user.base-url`) using `RestTemplate`.
        3.  If authentication is successful, a UUID token is generated.
        4.  The user's details (creating a new user if one doesn't exist, or updating the existing one) along with the new token and its expiry time (1 day) are saved to the database via `UserMapper`.
        5.  The `User` object (including token details) is serialized to JSON and cached in Redis with the key `token:<UUID_TOKEN>` and an expiry of 1 day.
        6.  A `LoginResponse` containing the token and expiry time is returned.
*   **Token Management & Validation**:
    *   Tokens are UUID strings.
    *   User data associated with a token is stored in Redis.
    *   The `AuthInterceptor` intercepts all requests to `/api/**` (except `/api/auth/login`).
    *   It extracts the `X-Token` header. If missing, it returns a 401 Unauthorized error.
    *   It calls `UserService.getUserByToken(token)`. This method attempts to retrieve the user JSON from Redis using the token.
    *   If the token is not found in Redis or is invalid, a 401 error is returned.
    *   If valid, the `User` object is deserialized and stored in `UserContext` (a `ThreadLocal`) for the duration of the request.
*   **Logout**:
    *   No explicit logout endpoint is implemented. Logout would typically involve deleting the token from Redis.

### 3.2. User Management

*   **Operations**:
    *   Listing users with pagination: `GET /api/users`. This operation requires administrator privileges.
*   **Controllers and Services**:
    *   `UserController.listUsers()`: Handles the request. It is annotated with `@RequireAdmin` and `@ValidateParams`.
    *   `UserServiceImpl.listUsers()`: Fetches paginated user data from the database using `UserMapper.findAllWithPagination()` and `UserMapper.countTotal()`.

### 3.3. Data Persistence

*   **Database Interaction**:
    *   The application uses MyBatis as its ORM framework.
    *   `MyBatisConfig.java` configures the `SqlSessionFactory`, specifying mapper XML locations (`classpath:mapper/*.xml`) and enabling underscore-to-camel-case mapping.
*   **Mapper Interfaces**:
    *   `UserMapper.java` is a MyBatis mapper interface defining methods for database operations on `User` entities. These methods are linked to SQL statements in `UserMapper.xml`.

### 3.4. Caching

*   **Redis Usage**:
    *   Redis is configured in `RedisConfig.java` (providing `StringRedisTemplate`).
    *   `UserServiceImpl` uses `StringRedisTemplate` for caching.
*   **Cached Data**:
    *   User objects (serialized as JSON) are cached in Redis upon successful login. The Redis key is `token:<TOKEN_STRING>`, and the value is the user's JSON data. These entries expire after 1 day.

### 3.5. Configuration Management

*   **Role of Nacos**:
    *   The project uses Nacos for service discovery and distributed configuration management (dependencies in `pom.xml`: `spring-cloud-starter-alibaba-nacos-config`, `spring-cloud-starter-alibaba-nacos-discovery`).
    *   `NacosConfig.java` enables Nacos discovery conditionally.
    *   `bootstrap.yml` configures the Nacos server address, namespace, and the data ID of the configuration file to be fetched from Nacos (e.g., `online-store.yaml`). The application can refresh its configuration dynamically from Nacos due to `@RefreshScope` on `OnlineStoreApplication`.
*   **Configuration Loading**:
    *   `bootstrap.yml` is loaded first by Spring Cloud, providing Nacos connection details.
    *   Configurations are then fetched from Nacos.
    *   `application.yml` provides local default configurations, which can be overridden by Nacos or environment variables.

### 3.6. API Input Validation

*   **Process**:
    *   Input validation is performed using Jakarta Bean Validation annotations on DTOs (e.g., `@Valid` on controller method parameters, and annotations like `@NotNull`, `@Min` on DTO fields).
*   **`@ValidateParams` and `ValidationAspect`**:
    *   The custom `@ValidateParams` annotation can be added to controller methods.
    *   `ValidationAspect` intercepts calls to these annotated methods.
    *   Its `validateParameters` advice uses `jakarta.validation.Validator` to validate method arguments.
    *   If validation errors occur, it returns a 400 Bad Request response with localized error messages (using `MessageSource`). Otherwise, the original method proceeds.

### 3.7. Authorization (Admin Access Control)

*   **Protection Mechanism**:
    *   Admin-only endpoints are protected using the custom `@RequireAdmin` annotation and `AdminAuthAspect`.
*   **`@RequireAdmin` and `AdminAuthAspect`**:
    *   Controller methods requiring admin rights (e.g., `UserController.listUsers()`) are annotated with `@RequireAdmin`.
    *   `AdminAuthAspect.checkAdminAuth()` is a `@Before` advice that runs before such methods.
    *   It retrieves the current user from `UserContext.getCurrentUser()`.
*   **Admin Identification**:
    *   A user is identified as an admin if their username matches the `admin.auth.username` property in `application.yml`.
    *   If the user is not logged in or is not the admin, an `IllegalArgumentException` is thrown, preventing access. Error messages are localized.

## 4. Project Setup and How to Run

**Required Software:**

*   **JDK:** Version 17 or higher
*   **Maven:** Version 3.6 or higher
*   **MySQL:** Version 8.0
*   **Redis:** Version 6.0 or higher

**Database Setup:**

1.  Ensure your MySQL service is running.
2.  Create the database using the following SQL command:
    ```sql
    CREATE DATABASE online_store DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
    ```

**Configuration Modifications:**

1.  Open the `src/main/resources/application.yml` file.
2.  Update the `spring.datasource` section with your MySQL connection details (URL, username, password).
3.  Update the `spring.data.redis` section with your Redis connection details (host, port, password).
4.  Optionally, configure Nacos settings in `src/main/resources/bootstrap.yml` and `application.yml` if you intend to use Nacos for configuration and service discovery. Set `NACOS_ENABLED` environment variable or property to `true`.

**Running the Application:**

1.  Ensure both MySQL and Redis services are started and accessible.
2.  Navigate to the root directory of the project in your terminal.
3.  Run the application using the following Maven command:
    ```bash
    mvn spring-boot:run
    ```
The application will typically start on port 8080, as configured in `application.yml`.
