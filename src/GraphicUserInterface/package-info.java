/**
 * Java Swing user interface (the View layer). It contains no business rules: every user action is passed to
 * one of the controllers.
 * <ul>
 *   <li>{@code GreenAirApp}: entry point. Builds all repositories, services and controllers and opens the window.</li>
 *   <li>{@code MainFrame}: the window. Keeps the logged-in user, switches screens and registers the alert observer.</li>
 *   <li>{@code LoginView} and {@code RegisterView}: username login and resident registration (regions are chosen later in the resident's All regions tab).</li>
 *   <li>{@code ResidentView}: air quality, an all-regions AQI table where residents subscribe to regions, park
 *       recommendations, activity advice, reports and alerts for the subscribed regions.</li>
 *   <li>{@code OfficerView}: citizen reports, alerts for all districts and a district overview.</li>
 *   <li>{@code AdminView}: user account and monitoring point management.</li>
 *   <li>{@code Theme}: shared colours, fonts and component styling.</li>
 * </ul>
 */
package graphicuserinterface;
