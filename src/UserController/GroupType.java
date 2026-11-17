package UserController;

import java.util.Arrays;
import java.util.List;

/**
 * The population groups that the recommendation strategies advise separately.
 *
 * <p>This is the {@code GroupType} enumeration of the domain model. The user picks one of these
 * labels when registering, and the advice shown on the resident dashboard is chosen from it.</p>
 */
public enum GroupType {

    GENERAL_PUBLIC("General public"),
    CHILDREN("Children"),
    ELDERLY("Elderly");

    private final String label;

    GroupType(String label) {
        this.label = label;
    }

    /** The text shown in the registration combo box. */
    public String label() {
        return label;
    }

    /** All labels, in enum order, ready for a Swing combo box model. */
    public static List<String> labels() {
        return Arrays.stream(values()).map(GroupType::label).toList();
    }

    /**
     * Reads a label back, accepting either the display label ("Children") or the stored name
     * ("CHILDREN").
     *
     * @throws IllegalArgumentException when the text is not one of the known groups
     */
    public static GroupType fromLabel(String label) {
        String value = label == null ? "" : label.trim();
        for (GroupType type : values()) {
            if (type.label.equalsIgnoreCase(value) || type.name().equalsIgnoreCase(value.replace(' ', '_'))) {
                return type;
            }
        }
        throw new IllegalArgumentException(
                "Unknown user group \"" + label + "\". Use one of: " + String.join(", ", labels()) + ".");
    }
}
