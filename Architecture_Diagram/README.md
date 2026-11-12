# GreenAir Klang Valley

**CSC61204 Software Construction — Group Assignment**

GreenAir Klang Valley is a Java desktop application designed to help users access local air quality information, receive population-specific environmental advice, find suitable green spaces, and manage pollution-related alerts and reports.

> **Project status:** Under development. This README describes the proposed system architecture; individual functions may still be undergoing implementation and testing.

## Project Objectives

- Provide air quality readings and forecasts for predefined monitoring points in Kuala Lumpur and Selangor.
- Support personalised environmental advice for the general public, children, and elderly people.
- Recommend parks using location and air quality information.
- Support pollution alerts and user-submitted reports.
- Provide role-appropriate interfaces for residents, environmental officers, and administrators.

## User Roles

| Role | Main responsibilities |
| --- | --- |
| **Resident** | View air quality information, advice, park recommendations, reports, and alerts. |
| **Environmental Officer** | Access pollution alerts and submitted reports for monitored districts. |
| **Administrator** | Manage user accounts and monitoring points. |

Access to individual controller operations is intended to follow the permissions of each user role.

## System Architecture

![GreenAir Klang Valley System Architecture](architecture/greenair-system-architecture.png)

The application uses a **five-layer architecture** incorporating **Model–View–Controller (MVC)** principles:

1. **View Layer:** Java Swing interfaces, including login/registration and role-specific views.
2. **Controller Layer:** Handles user interactions and delegates operations to services. The design includes `UserController`, `MonitoringPointController`, `AirQualityController`, `RecommendationController`, `AlertController`, and `ReportController`.
3. **Service Layer:** Contains business logic for user management, monitoring points, air quality, environmental advice, park recommendations, pollution alerts, and reports.
4. **Repository Layer:** Encapsulates data access and persistence operations.
5. **Domain Model Layer:** Defines shared entities such as `UserProfile`, `MonitoringPoint`, `AirQualityReading`, `PollutionAlert`, `Park`, and `Report`, as well as the `UserRole` and `GroupType` enumerations.

### Design Patterns

- **Strategy Pattern:** `RecommendationStrategy` supports different advice behaviours through `GeneralPublicStrategy`, `ChildrenStrategy`, and `ElderlyStrategy`.
- **Observer Pattern:** `PollutionAlertService` coordinates alert notifications using `AlertObserver`, `ResidentAlertObserver`, and `OfficerAlertObserver`.

### External API Integration

`AirQualityService` obtains air quality readings and forecasts through `OpenMeteoClient`, which communicates with the **Open-Meteo Air Quality API**. The architecture also includes `AirQualityRepository` for storing historical/cached readings and supporting fallback when suitable cached data is available.

### Data Persistence

The current architecture specifies JSON-based storage through repositories, with the following proposed files:

- `users.json`
- `monitoring_points.json`
- `air_history.json`
- `parks.json`
- `alerts.json`
- `reports.json`

## Technology Stack

- **Language:** Java
- **Desktop UI:** Java Swing
- **Architecture:** Layered architecture with MVC principles
- **Design patterns:** Strategy and Observer
- **External data:** Open-Meteo Air Quality API (HTTP/JSON)
- **Data persistence:** JSON files (as specified in the architecture)
- **Testing targets:** JUnit and JaCoCo

## Running the Application

The application is under development. The team will add the exact build/run steps after confirming the project's entry-point class, dependency management, and executable JAR packaging.

## Testing

The planned integration and testing work includes Java GUI/API integration, JUnit tests, and JaCoCo code-coverage reporting. Test evidence and runnable release instructions will be documented as implementation progresses.

## Architecture Documentation

- [View the system architecture diagram](docs/architecture/greenair-system-architecture.png)

---

*Developed for CSC61204 Software Construction.*
