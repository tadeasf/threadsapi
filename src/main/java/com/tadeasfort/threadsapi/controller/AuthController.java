package com.tadeasfort.threadsapi.controller;

import com.tadeasfort.threadsapi.config.ThreadsApiConfig;
import com.tadeasfort.threadsapi.config.ThreadsAppConfig;
import com.tadeasfort.threadsapi.dto.ThreadsAuthRequest;
import com.tadeasfort.threadsapi.dto.ThreadsTokenResponse;
import com.tadeasfort.threadsapi.dto.WebhookRequest;
import com.tadeasfort.threadsapi.service.ThreadsApiClient;
import com.tadeasfort.threadsapi.service.WebhookService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;

import jakarta.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@Tag(name = "Authentication", description = "Threads API authentication endpoints")
public class AuthController {

    private static final Logger logger = LoggerFactory.getLogger(AuthController.class);

    private final ThreadsApiClient threadsApiClient;
    private final ThreadsApiConfig threadsApiConfig;
    private final ThreadsAppConfig threadsAppConfig;
    private final WebhookService webhookService;

    @Value("${app.url}")
    private String appUrl;

    @Value("${app.frontend.url}")
    private String frontendUrl;

    @Value("${threads.app.secret}")
    private String appSecret;

    public AuthController(ThreadsApiClient threadsApiClient,
            ThreadsApiConfig threadsApiConfig,
            ThreadsAppConfig threadsAppConfig,
            WebhookService webhookService) {
        this.threadsApiClient = threadsApiClient;
        this.threadsApiConfig = threadsApiConfig;
        this.threadsAppConfig = threadsAppConfig;
        this.webhookService = webhookService;
    }

    @GetMapping("/login-url")
    @Operation(summary = "Get Meta login URL", description = "Returns the authorization URL for Meta OAuth flow")
    public ResponseEntity<Map<String, String>> getLoginUrl() {
        String redirectUri = appUrl + "/api/auth/callback";

        String authUrl = UriComponentsBuilder.fromUriString(threadsApiConfig.getAuthUrl())
                .queryParam("client_id", threadsAppConfig.getId())
                .queryParam("redirect_uri", redirectUri)
                .queryParam("scope",
                        "threads_basic,threads_content_publish,threads_delete,threads_keyword_search,threads_location_tagging,threads_manage_insights,threads_manage_mentions,threads_manage_replies,threads_read_replies")
                .queryParam("response_type", "code")
                .toUriString();

        Map<String, String> response = new HashMap<>();
        response.put("authUrl", authUrl);
        response.put("redirectUri", redirectUri);

        return ResponseEntity.ok(response);
    }

    @PostMapping("/exchange-token")
    @Operation(summary = "Exchange authorization code for access token", description = "Exchanges the authorization code received from Meta for an access token")
    public ResponseEntity<ThreadsTokenResponse> exchangeToken(
            @RequestBody ThreadsAuthRequest authRequest) {

        try {
            // Exchange code for short-lived token
            ThreadsTokenResponse shortLivedToken = threadsApiClient.exchangeCodeForToken(
                    authRequest.getCode(),
                    authRequest.getRedirectUri());

            if (shortLivedToken != null && shortLivedToken.getAccessToken() != null) {
                // Exchange for long-lived token
                ThreadsTokenResponse longLivedToken = threadsApiClient.exchangeForLongLivedToken(
                        shortLivedToken.getAccessToken());

                return ResponseEntity.ok(longLivedToken != null ? longLivedToken : shortLivedToken);
            }

            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/callback")
    @Operation(summary = "OAuth callback endpoint", description = "Handles the OAuth callback from Meta")
    public ResponseEntity<String> handleCallback(
            @Parameter(description = "Authorization code from Meta") @RequestParam String code,
            @Parameter(description = "State parameter") @RequestParam(required = false) String state,
            @Parameter(description = "Error parameter") @RequestParam(required = false) String error) {

        logger.info("OAuth callback received - code: {}, state: {}, error: {}", code, state, error);

        // Handle error cases
        if (error != null) {
            String frontendUrl = getFrontendUrl();
            return ResponseEntity.status(302)
                    .header("Location", frontendUrl + "/login?error=" + error)
                    .body("Redirecting to frontend with error...");
        }

        try {
            // Exchange code for token server-side
            String redirectUri = appUrl + "/api/auth/callback";
            ThreadsTokenResponse tokenResponse = threadsApiClient.exchangeCodeForToken(code, redirectUri);

            if (tokenResponse != null && tokenResponse.getAccessToken() != null) {
                // Exchange for long-lived token
                ThreadsTokenResponse longLivedToken = threadsApiClient.exchangeForLongLivedToken(
                        tokenResponse.getAccessToken());

                // Use the long-lived token if available, otherwise use short-lived
                ThreadsTokenResponse finalToken = longLivedToken != null ? longLivedToken : tokenResponse;

                // Get user profile to extract the actual user ID
                String userId = null;
                try {
                    Map<String, Object> userProfile = threadsApiClient.getUserProfile(finalToken.getAccessToken());
                    userId = (String) userProfile.get("id");
                } catch (Exception e) {
                    logger.error("Failed to fetch user profile for user ID extraction", e);
                    // Fallback to token user ID if available
                    userId = finalToken.getUserId();
                }

                // Redirect to frontend with token
                String frontendUrl = getFrontendUrl();
                String redirectUrl = frontendUrl + "/login?success=true&access_token=" +
                        finalToken.getAccessToken() + "&user_id=" + userId;

                logger.info("Redirecting to frontend: {}", frontendUrl);

                return ResponseEntity.status(302)
                        .header("Location", redirectUrl)
                        .body("Redirecting to frontend...");

            } else {
                logger.error("Failed to exchange code for token");
                String frontendUrl = getFrontendUrl();
                return ResponseEntity.status(302)
                        .header("Location", frontendUrl + "/login?error=token_exchange_failed")
                        .body("Redirecting to frontend with error...");
            }

        } catch (Exception e) {
            logger.error("Error in OAuth callback", e);
            String frontendUrl = getFrontendUrl();
            return ResponseEntity.status(302)
                    .header("Location", frontendUrl + "/login?error=server_error")
                    .body("Redirecting to frontend with error...");
        }
    }

    /**
     * Helper method to get frontend URL based on environment
     */
    private String getFrontendUrl() {
        return frontendUrl;
    }

    @PostMapping("/refresh-token")
    @Operation(summary = "Refresh long-lived token", description = "Refreshes a long-lived access token before it expires")
    public ResponseEntity<ThreadsTokenResponse> refreshToken(
            @Parameter(description = "Current access token") @RequestParam String accessToken) {

        try {
            ThreadsTokenResponse refreshedToken = threadsApiClient.exchangeForLongLivedToken(accessToken);
            return ResponseEntity.ok(refreshedToken);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @PostMapping("/uninstall")
    @Operation(summary = "Handle user deauthorization webhook", description = "Webhook endpoint called when users revoke app permissions")
    public ResponseEntity<String> handleUninstall(
            @RequestBody WebhookRequest webhookRequest,
            @RequestHeader(value = "X-Hub-Signature-256", required = false) String signature,
            HttpServletRequest request) {

        logger.info("Received uninstall webhook: {}", webhookRequest);

        try {
            // Verify webhook signature for security
            String payload = getRequestBody(request);
            if (signature != null && !webhookService.verifyWebhookSignature(payload, signature, appSecret)) {
                logger.warn("Invalid webhook signature for uninstall event");
                return ResponseEntity.status(401).body("Invalid signature");
            }

            // Process the deauthorization
            webhookService.handleUserDeauthorization(webhookRequest);

            logger.info("Successfully processed uninstall webhook");
            return ResponseEntity.ok("OK");

        } catch (Exception e) {
            logger.error("Error processing uninstall webhook", e);
            return ResponseEntity.internalServerError().body("Error processing webhook");
        }
    }

    @PostMapping("/delete")
    @Operation(summary = "Handle user deletion webhook", description = "Webhook endpoint called when users delete their Meta account")
    public ResponseEntity<String> handleDelete(
            @RequestBody WebhookRequest webhookRequest,
            @RequestHeader(value = "X-Hub-Signature-256", required = false) String signature,
            HttpServletRequest request) {

        logger.info("Received delete webhook: {}", webhookRequest);

        try {
            // Verify webhook signature for security
            String payload = getRequestBody(request);
            if (signature != null && !webhookService.verifyWebhookSignature(payload, signature, appSecret)) {
                logger.warn("Invalid webhook signature for delete event");
                return ResponseEntity.status(401).body("Invalid signature");
            }

            // Process the deletion
            webhookService.handleUserDeletion(webhookRequest);

            logger.info("Successfully processed delete webhook");
            return ResponseEntity.ok("OK");

        } catch (Exception e) {
            logger.error("Error processing delete webhook", e);
            return ResponseEntity.internalServerError().body("Error processing webhook");
        }
    }

    @GetMapping("/uninstall")
    @Operation(summary = "Verify uninstall webhook", description = "Webhook verification endpoint for uninstall events")
    public ResponseEntity<String> verifyUninstallWebhook(
            @RequestParam("hub.mode") String mode,
            @RequestParam("hub.challenge") String challenge,
            @RequestParam("hub.verify_token") String verifyToken) {

        logger.info("Webhook verification request - mode: {}, challenge: {}, token: {}", mode, challenge, verifyToken);

        // Verify the token matches your configured verify token
        // You should set this in your Meta app webhook configuration
        if ("subscribe".equals(mode) && appSecret.equals(verifyToken)) {
            logger.info("Webhook verification successful");
            return ResponseEntity.ok(challenge);
        } else {
            logger.warn("Webhook verification failed");
            return ResponseEntity.status(403).body("Forbidden");
        }
    }

    @GetMapping("/delete")
    @Operation(summary = "Verify delete webhook", description = "Webhook verification endpoint for delete events")
    public ResponseEntity<String> verifyDeleteWebhook(
            @RequestParam("hub.mode") String mode,
            @RequestParam("hub.challenge") String challenge,
            @RequestParam("hub.verify_token") String verifyToken) {

        logger.info("Webhook verification request - mode: {}, challenge: {}, token: {}", mode, challenge, verifyToken);

        // Verify the token matches your configured verify token
        if ("subscribe".equals(mode) && appSecret.equals(verifyToken)) {
            logger.info("Webhook verification successful");
            return ResponseEntity.ok(challenge);
        } else {
            logger.warn("Webhook verification failed");
            return ResponseEntity.status(403).body("Forbidden");
        }
    }

    /**
     * Helper method to get request body for signature verification
     */
    private String getRequestBody(HttpServletRequest request) {
        try {
            return request.getReader().lines()
                    .reduce("", (accumulator, actual) -> accumulator + actual);
        } catch (Exception e) {
            logger.error("Error reading request body", e);
            return "";
        }
    }
}