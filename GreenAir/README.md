# GreenAir

GreenAir is a Java Swing application for monitoring air quality and recommending public parks. The starter project contains empty type declarations from the UML class diagram.

## Build

Use JDK 17 or newer. In IntelliJ IDEA, open the project folder, select a JDK, and mark `src/` as a Sources Root. Choose **Build > Build Project**.

To compile from PowerShell, run these commands from the project root.

```powershell
$sources = Get-ChildItem -LiteralPath src -Recurse -File -Filter '*.java'
javac --release 17 -d out $sources.FullName
```

## Package structure

Source files live directly under the module folders in `src/`. Each Java package uses its folder name.

| Package | Responsibility |
| --- | --- |
| `AirQualityController` | `AirQualityController`, Open-Meteo readings and history |
| `RecommendationController` | `RecommendationController`, activity advice and park recommendations |
| `AlertController` | `AlertController`, pollution alerts and subscriptions |
| `ReportController` | `ReportController`, resident reports and officer review |
| `UserController` | `UserController`, accounts, login and roles |
| `MonitoringPointController` | `MonitoringPointController`, monitoring point management |
| `GraphicUserInterface` | Java Swing views and navigation |

Use the class diagram for key field names, class names, and method signatures.
