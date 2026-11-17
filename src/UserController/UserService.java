package UserController;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;

/**
 * Service layer of the user account feature, the {@code UserService} of Figure 3.0.
 *
 * <p>It owns every rule that has to hold whichever screen triggered the request: usernames are
 * trimmed, unique and 3-30 characters long, full names are required, the population group is one
 * of the {@link GroupType} values, an account is only usable while it is active, and the alert
 * districts are stored without blanks or duplicates. Persistence is delegated to
 * {@link ProfileRepository}, so this class never touches a file.</p>
 */
public class UserService {

    /**
     * Thrown when a request cannot be carried out. The message is written for the end user, so the
     * controller or the view can show it as it is instead of a stack trace.
     */
    public static class UserRequestException extends RuntimeException {
        public UserRequestException(String message) {
            super(message);
        }
    }

    static final int USERNAME_MIN_LENGTH = 3;
    static final int USERNAME_MAX_LENGTH = 30;
    static final int NAME_MAX_LENGTH = 80;

    private static final String USERNAME_PATTERN =
            "[A-Za-z0-9._-]{" + USERNAME_MIN_LENGTH + "," + USERNAME_MAX_LENGTH + "}";

    private final ProfileRepository profiles;

    public UserService(ProfileRepository profiles) {
        this.profiles = profiles;
    }

    public ProfileRepository repository() {
        return profiles;
    }

    public List<UserProfile> findAll() {
        return profiles.findAll();
    }

    public Optional<UserProfile> findByUsername(String username) {
        return profiles.findByUsername(username);
    }

    /** @throws UserRequestException when no account carries that id any more */
    public UserProfile requireProfile(String id) {
        if (id == null || id.isBlank()) throw new UserRequestException("Select an account first.");
        return profiles.findById(id)
                .orElseThrow(() -> new UserRequestException("Account " + id.trim() + " no longer exists."));
    }

    /**
     * Checks a username and returns the matching account.
     *
     * @throws UserRequestException when the username is unknown or the account is deactivated
     */
    public UserProfile logIn(String username) {
        String cleanUsername = username == null ? "" : username.trim();
        if (cleanUsername.isEmpty()) throw new UserRequestException("Enter your username.");

        UserProfile profile = profiles.findByUsername(cleanUsername)
                .orElseThrow(() -> new UserRequestException("No account found for \"" + cleanUsername + "\"."));
        if (!profile.active()) {
            throw new UserRequestException("This account is deactivated. Contact an administrator.");
        }
        return profile;
    }

    /**
     * Creates the account of a new registration. Self-registration always produces a
     * {@link UserRole#RESIDENT}; other roles are only handed out by an administrator.
     *
     * @throws UserRequestException when the form is not acceptable or the username is taken
     */
    public UserProfile registerResident(String username, String fullName, GroupType groupType, List<String> districts) {
        String cleanUsername = validateUsername(username);
        String cleanFullName = validateFullName(fullName);
        if (groupType == null) throw new UserRequestException("Choose a user group.");
        if (profiles.existsByUsername(cleanUsername)) {
            throw new UserRequestException("The username \"" + cleanUsername + "\" is already taken.");
        }
        UserProfile draft = new UserProfile("", cleanUsername, cleanFullName, UserRole.RESIDENT,
                groupType, true, cleanDistricts(districts));
        return profiles.save(draft);
    }

    /** Applies a profile edit made by the owner of the account. */
    public UserProfile updateProfile(String userId, String fullName, GroupType groupType, List<String> districts) {
        UserProfile profile = requireProfile(userId);
        if (groupType == null) throw new UserRequestException("Choose a user group.");
        UserProfile updated = new UserProfile(profile.id(), profile.username(), validateFullName(fullName),
                profile.role(), groupType, profile.active(), cleanDistricts(districts));
        return profiles.save(updated);
    }

    /** Changes the role of an account. */
    public UserProfile changeRole(String userId, UserRole role) {
        UserProfile profile = requireProfile(userId);
        if (role == null) throw new UserRequestException("Choose a role.");
        UserProfile updated = new UserProfile(profile.id(), profile.username(), profile.fullName(),
                role, profile.groupType(), profile.active(), profile.districts());
        return profiles.save(updated);
    }

    /** Activates or deactivates an account. */
    public UserProfile setActive(String userId, boolean active) {
        UserProfile profile = requireProfile(userId);
        UserProfile updated = new UserProfile(profile.id(), profile.username(), profile.fullName(),
                profile.role(), profile.groupType(), active, profile.districts());
        return profiles.save(updated);
    }

    /** Removes an account from users.json. */
    public void deleteAccount(String userId) {
        UserProfile profile = requireProfile(userId);
        if (!profiles.deleteById(profile.id())) {
            throw new UserRequestException("Account " + profile.id() + " no longer exists.");
        }
    }

    // ---------------------------------------------------------------- validation

    private static String validateUsername(String username) {
        String value = username == null ? "" : username.trim();
        if (value.isEmpty()) throw new UserRequestException("A username is required.");
        if (!value.matches(USERNAME_PATTERN)) {
            throw new UserRequestException("Username must be " + USERNAME_MIN_LENGTH + "-" + USERNAME_MAX_LENGTH
                    + " characters: letters, digits, '.', '_' or '-'.");
        }
        return value;
    }

    private static String validateFullName(String fullName) {
        String value = fullName == null ? "" : fullName.trim();
        if (value.isEmpty()) throw new UserRequestException("A full name is required.");
        if (value.length() > NAME_MAX_LENGTH) {
            throw new UserRequestException("A full name must be at most " + NAME_MAX_LENGTH + " characters.");
        }
        return value;
    }

    /** Trims the subscribed districts and drops blanks and duplicates. */
    private static List<String> cleanDistricts(List<String> districts) {
        if (districts == null) return List.of();
        LinkedHashSet<String> cleaned = new LinkedHashSet<>();
        for (String district : districts) {
            if (district != null && !district.isBlank()) cleaned.add(district.trim());
        }
        return new ArrayList<>(cleaned);
    }
}
