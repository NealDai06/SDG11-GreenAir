package RecommendationController;

import AirQualityController.AirQualityReading;
import UserController.GroupType;
import java.util.Objects;

/** Selects a group-specific implementation of the Strategy interface in the class diagram. */
public class AdviceService {
    private RecommendationStrategy strategy;

    /** Selects the public, children or elderly strategy and delegates the advice rules to it. */
    public synchronized String getAdvice(GroupType userType, AirQualityReading reading) {
        Objects.requireNonNull(reading, "reading");
        strategy = switch (Objects.requireNonNull(userType, "userType")) {
            case GENERAL_PUBLIC -> new GeneralPublicStrategy();
            case CHILDREN -> new ChildrenStrategy();
            case ELDERLY -> new ElderlyStrategy();
        };
        return strategy.advise(reading);
    }
}
