package UniNest.Backend.controller;

import UniNest.Backend.dto.RegisterDeviceTokenRequest;
import UniNest.Backend.dto.SendNotificationRequest;
import UniNest.Backend.model.AppNotification;
import UniNest.Backend.model.User;
import UniNest.Backend.service.NotificationService;
import UniNest.Backend.service.UserService;
import UniNest.Backend.util.SanitizationUtil;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@RestController
@RequestMapping("/notifications")
public class NotificationController {

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private UserService userService;

    @PostMapping("/register-token")
    public void registerToken(@RequestBody RegisterDeviceTokenRequest req) throws Exception {
        String currentUserId = SecurityContextHolder.getContext().getAuthentication().getName();
        notificationService.registerToken(currentUserId, req.getToken());
    }

    @PreAuthorize("hasAnyRole('TENANT','LETTINGAGENT')")
    @GetMapping("/my")
    public ResponseEntity<List<AppNotification>> getMyNotifications() {
        try {
            String currentUserId = SecurityContextHolder.getContext().getAuthentication().getName();
            List<AppNotification> notifications = notificationService.getNotificationsForUser(currentUserId);
            return new ResponseEntity<>(notifications, HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>(new ArrayList<>(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @PreAuthorize("hasRole('LETTINGAGENT')")
    @PostMapping("/send")
    public int send(@RequestBody SendNotificationRequest req) throws Exception {
        String currentUserId = SecurityContextHolder.getContext().getAuthentication().getName();

        String houseCode = req.getHouseCode() != null
                ? SanitizationUtil.sanitize(req.getHouseCode())
                : null;

        String title = req.getTitle() != null
                ? SanitizationUtil.sanitize(req.getTitle())
                : "New message";

        String body = req.getBody() != null
                ? SanitizationUtil.sanitize(req.getBody())
                : "";

        Set<String> recipientIds = new LinkedHashSet<>();

        List<String> requestedTenantIds = req.getTenantUserIds();
        if (requestedTenantIds != null && !requestedTenantIds.isEmpty()) {
            for (String id : requestedTenantIds) {
                if (id != null && !id.isBlank()) {
                    recipientIds.add(id);
                }
            }
        } else if (houseCode != null && !houseCode.isBlank()) {
            List<User> users = userService.getUsersForApartment(houseCode);

            for (User u : users) {
                if (u != null
                        && u.getId() != null
                        && !u.getId().isBlank()
                        && ("2".equals(u.getRole()) || "TENANT".equalsIgnoreCase(u.getRole()))) {
                    recipientIds.add(u.getId());
                }
            }
        }

        // Add the current agent too, so the agent also sees their own sent message
        if (currentUserId != null && !currentUserId.isBlank()) {
            recipientIds.add(currentUserId);
        }

        List<String> targetUserIds = new ArrayList<>(recipientIds);

        if (targetUserIds.isEmpty()) {
            return 0;
        }

        notificationService.createMessageNotification(
                targetUserIds,
                title,
                body
        );

        return notificationService.sendToUsers(
                title,
                body,
                targetUserIds,
                "HOME",
                null
        );
    }
}