package RecommendationController;

import AirQualityController.AirQualityReading;
import UserController.GroupType;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Objects;

/** The UI-facing recommendation controller with the two dependencies shown in the class diagram. */
public class RecommendationController {
    private static final DateTimeFormatter DISPLAY_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private final AdviceService adviceService;
    private final ParkRecommendationService parkService;

    /** Connects the advice and park recommendation services. */
    public RecommendationController(AdviceService adviceService, ParkRecommendationService parkService) {
        this.adviceService = Objects.requireNonNull(adviceService, "adviceService");
        this.parkService = Objects.requireNonNull(parkService, "parkService");
    }

    /** Returns group-specific advice with the reading's timestamp and cached-data status. */
    public String getActivityAdvice(GroupType userType, String pointId) {
        Objects.requireNonNull(userType, "userType");
        AirQualityReading reading = parkService.currentReading(pointId);
        String source = reading.cached() ? "Cached data fetched at " : "Live data fetched at ";
        return "US AQI " + reading.usAqi() + ". Reading for " + reading.validTime().format(DISPLAY_TIME) + ".\n"
                + source + reading.fetchedAt().format(DISPLAY_TIME) + " (Asia/Kuala_Lumpur).\n\n"
                + adviceService.getAdvice(userType, reading);
    }

    /** Returns the park service's ordered recommendations for the selected monitoring point. */
    public List<Park> getRecommendedParks(String pointId) {
        return parkService.getRecommendedPark(pointId);
    }
}
