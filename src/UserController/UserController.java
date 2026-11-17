package UserController;

import java.nio.file.Path;
import java.util.List;

/**
 * Controller layer of the user account feature, the {@code UserController} of Figure 3.0.
 *
 * <p>It is the mediator between {@code GraphicUserInterface} (LoginView, RegisterView, AdminView)
 * and {@link UserService}. The controller keeps the signed-in account, decides who may do what -
 * only an administrator may list, re-role, deactivate or delete accounts, and nobody may lock
 * themselves out - and turns a service rule violation into
 * {@link UserService.UserRequestException} with a message the view can display.</p>
 *
 * <p>Nothing here imports Swing, so the class can be unit tested without starting the GUI.</p>
 */
public class UserController {

    private final UserService service;
    private UserProfile currentUser;

    public UserController(UserService service) {
        this.service = service;
    }

    /** Controller over the given users.json file. */
    public UserController(ProfileRepository profiles) {
        this(new UserService(profiles));
    }

    /** Controller over the users.json file of the running application. */
    public UserController() {
        this(new ProfileRepository());
    }

    // ---------------------------------------------------------------- session

    /**
     * Signs an account in and remembers it until {@link #logOut()}.
     *
     * @return the signed-in profile
     * @throws UserService.UserRequestException when the username is unknown or the account is inactive
     */
    public UserProfile logIn(String username) {
        currentUser = service.logIn(username);
        return currentUser;
    }

    public void logOut() {
        currentUser = null;
    }

    public UserProfile currentUser() {
        return currentUser;
    }

    public boolean isLoggedIn() {
        return currentUser != null;
    }

    public boolean isAdministrator() {
        return currentUser != null && currentUser.isAdministrator();
    }

    public Path profileFile() {
        return service.repository().file();
    }

    // ---------------------------------------------------------------- registration and profile

    /**
     * Registers a resident account from the registration form.
     *
     * @return the created account, already stored in users.json
     * @throws UserService.UserRequestException when the form is incomplete or the username is taken
     */
    public UserProfile register(String username, String fullName, GroupType groupType, List<String> districts) {
        return service.registerResident(username, fullName, groupType, districts);
    }

    /**
     * Lets the signed-in user change their own name, population group and alert districts.
     *
     * @throws UserService.UserRequestException when nobody is signed in or the input is invalid
     */
    public UserProfile updateProfile(String fullName, GroupType groupType, List<String> districts) {
        UserProfile actor = requireSession();
        UserProfile updated = service.updateProfile(actor.id(), fullName, groupType, districts);
        currentUser = updated;
        return updated;
    }

    /** Refreshes the cached session from users.json, for example after an administrator edited it. */
    public UserProfile reloadSession() {
        if (currentUser == null) return null;
        currentUser = service.requireProfile(currentUser.id());
        return currentUser;
    }

    // ---------------------------------------------------------------- administrator functions

    /** Every account, as listed on the administrator dashboard. */
    public List<UserProfile> listAccounts() {
        requireAdministrator("see the user accounts");
        return service.findAll();
    }

    /**
     * Gives an account another role.
     *
     * @throws UserService.UserRequestException when the caller is not an administrator or tries to
     *                                         drop their own administrator role
     */
    public UserProfile changeRole(String targetId, UserRole role) {
        UserProfile actor = requireAdministrator("change user roles");
        if (isSelf(actor, targetId) && (role == null || !role.isAdministrator())) {
            throw new UserService.UserRequestException("You cannot remove your own administrator role.");
        }
        UserProfile updated = service.changeRole(targetId, role);
        keepSessionInSync(updated);
        return updated;
    }

    /** Activates or deactivates an account. */
    public UserProfile setUserActive(String targetId, boolean active) {
        UserProfile actor = requireAdministrator("activate or deactivate accounts");
        if (isSelf(actor, targetId) && !active) {
            throw new UserService.UserRequestException(
                    "You cannot deactivate the account you are logged in with.");
        }
        UserProfile updated = service.setActive(targetId, active);
        keepSessionInSync(updated);
        return updated;
    }

    /** Deletes an account. */
    public void deleteAccount(String targetId) {
        UserProfile actor = requireAdministrator("delete accounts");
        if (isSelf(actor, targetId)) {
            throw new UserService.UserRequestException(
                    "You cannot delete the account you are logged in with.");
        }
        service.deleteAccount(targetId);
    }

    // ---------------------------------------------------------------- access control

    private UserProfile requireSession() {
        if (currentUser == null) throw new UserService.UserRequestException("Sign in first.");
        return currentUser;
    }

    private UserProfile requireAdministrator(String action) {
        UserProfile actor = requireSession();
        if (!actor.isAdministrator()) {
            throw new UserService.UserRequestException("Only administrators may " + action + ".");
        }
        return actor;
    }

    private static boolean isSelf(UserProfile actor, String targetId) {
        return actor != null && targetId != null && actor.id().equals(targetId.trim());
    }

    /** Keeps the session copy up to date when an administrator changes their own account. */
    private void keepSessionInSync(UserProfile updated) {
        if (currentUser != null && currentUser.id().equals(updated.id())) {
            currentUser = updated;
        }
    }
}
