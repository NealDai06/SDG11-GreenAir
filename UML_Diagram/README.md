# UML Class Diagram – Air Quality Monitoring and Park Recommendation System

## Overview

This UML Class Diagram illustrates the object-oriented structure of an Air Quality Monitoring and Park Recommendation System. It shows the main classes, attributes, methods, and relationships between different components.

The diagram follows a layered design consisting of Controllers, Services, Repositories, and Model Classes. It also incorporates the Strategy and Observer design patterns.

## Class Diagram Structure

| Layer | Main Classes | Responsibility |
|---|---|---|
| UI Layer | UI | Provides the user interface. |
| Controller Layer | UserController, MonitoringPointController, AirQualityController, RecommendationController, AlertController | Handles requests and communicates with services. |
| Service Layer | UserService, MonitoringPointService, AirQualityService, AdviceService, ParkRecommendationService, PollutionAlertService, ReportService | Implements the main system logic. |
| Repository Layer | ProfileRepository, MonitoringPointRepository, AirQualityRepository, ParkRepository, AlertRepository, ReportRepository | Manages data storage and retrieval. |
| Model Layer | UserProfile, MonitoringPoint, AirQualityReading, Park, PollutionAlert, Report | Defines the main data entities. |
| External API | OpenMeteoClient | Retrieves air quality data from an external API. |

## Design Patterns

| Pattern | Classes | Purpose |
|---|---|---|
| Strategy Pattern | RecommendationStrategy, GeneralPublicStrategy, ChildrenStrategy, ElderlyStrategy | Provides different activity recommendations for different user groups. |
| Observer Pattern | AlertObserver, ResidentAlertObserver, OfficerAlertObserver | Handles pollution alerts for different user roles. |

## Class Diagram

![UML Class Diagram](docs/class-diagram.jpg)

---

**UML Design Tool:** Visual Paradigm  
