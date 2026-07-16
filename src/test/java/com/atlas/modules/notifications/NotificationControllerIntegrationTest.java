package com.atlas.modules.notifications;

import com.atlas.modules.auth.dto.LoginRequest;
import com.atlas.modules.auth.dto.RegisterRequest;
import com.atlas.modules.billing.repository.SubscriptionRepository;
import com.atlas.modules.invitations.repository.InvitationRepository;
import com.atlas.modules.notifications.repository.NotificationPreferenceRepository;
import com.atlas.modules.notifications.repository.NotificationRepository;
import com.atlas.modules.organizations.OrganizationIntegrationTestSupport;
import com.atlas.modules.organizations.dto.CreateOrganizationRequest;
import com.atlas.modules.organizations.repository.MembershipRepository;
import com.atlas.modules.organizations.repository.OrganizationRepository;
import com.atlas.modules.projects.dto.AddProjectMemberRequest;
import com.atlas.modules.projects.dto.CreateProjectRequest;
import com.atlas.modules.projects.repository.ProjectMembershipRepository;
import com.atlas.modules.projects.repository.ProjectRepository;
import com.atlas.modules.tasks.dto.AddAssigneeRequest;
import com.atlas.modules.tasks.dto.CreateTaskRequest;
import com.atlas.modules.tasks.repository.TaskRepository;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import java.util.UUID;

import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class NotificationControllerIntegrationTest extends OrganizationIntegrationTestSupport {

    @Autowired
    private NotificationRepository notificationRepository;
    @Autowired
    private NotificationPreferenceRepository notificationPreferenceRepository;
    @Autowired
    private InvitationRepository invitationRepository;
    @Autowired
    private TaskRepository taskRepository;
    @Autowired
    private ProjectMembershipRepository projectMembershipRepository;
    @Autowired
    private ProjectRepository projectRepository;
    @Autowired
    private MembershipRepository membershipRepository;
    @Autowired
    private SubscriptionRepository subscriptionRepository;
    @Autowired
    private OrganizationRepository organizationRepository;

    @BeforeEach
    void cleanData() {
        notificationRepository.deleteAll();
        notificationPreferenceRepository.deleteAll();
        invitationRepository.deleteAll();
        taskRepository.deleteAll();
        projectMembershipRepository.deleteAll();
        projectRepository.deleteAll();
        membershipRepository.deleteAll();
        subscriptionRepository.deleteAll();
        organizationRepository.deleteAll();
    }

    @Test
    void assignsTaskAndNotifiesAssignee() throws Exception {
        String ownerToken = registerAndLogin("owner-notify@example.com", "Owner");
        UUID organizationId = createOrganization(ownerToken, "notify-org", "Notify Org");
        bumpSeats(organizationId, 3);

        MvcResult projectResult = mockMvc.perform(post("/api/v1/organizations/" + organizationId + "/projects")
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new CreateProjectRequest("Core", "CORE", null, "ORG_WIDE"))))
                .andExpect(status().isCreated())
                .andReturn();
        UUID projectId = UUID.fromString(objectMapper.readTree(projectResult.getResponse().getContentAsString())
                .path("data").path("id").asText());

        String memberToken = registerAndLogin("assignee@example.com", "Assignee");
        UUID memberId = userIdFromToken(memberToken);

        MvcResult inviteResult = mockMvc.perform(post("/api/v1/organizations/" + organizationId + "/invitations")
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"assignee@example.com\",\"role\":\"MEMBER\"}"))
                .andExpect(status().isCreated())
                .andReturn();
        UUID invitationId = UUID.fromString(objectMapper.readTree(inviteResult.getResponse().getContentAsString())
                .path("data").path("id").asText());
        mockMvc.perform(post("/api/v1/invitations/" + invitationId + "/accept")
                        .header("Authorization", "Bearer " + memberToken))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/projects/" + projectId + "/members")
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new AddProjectMemberRequest(memberId, "MEMBER"))))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/v1/notifications")
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items.length()").value(greaterThanOrEqualTo(1)));

        MvcResult taskResult = mockMvc.perform(post("/api/v1/projects/" + projectId + "/tasks")
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new CreateTaskRequest("Ship notifications", null, null, null, null, null))))
                .andExpect(status().isCreated())
                .andReturn();
        UUID taskId = UUID.fromString(objectMapper.readTree(taskResult.getResponse().getContentAsString())
                .path("data").path("id").asText());

        mockMvc.perform(post("/api/v1/tasks/" + taskId + "/assignees")
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new AddAssigneeRequest(memberId))))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/notifications?unreadOnly=true")
                        .header("Authorization", "Bearer " + memberToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items[0].eventType").value("task.assignee_added"))
                .andExpect(jsonPath("$.data.items[0].title").value("You were assigned a task"));

        mockMvc.perform(get("/api/v1/notifications/unread-count")
                        .header("Authorization", "Bearer " + memberToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.unreadCount").value(greaterThanOrEqualTo(1)));

        JsonNode list = objectMapper.readTree(mockMvc.perform(get("/api/v1/notifications")
                        .header("Authorization", "Bearer " + memberToken))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString());
        UUID notificationId = UUID.fromString(list.path("data").path("items").get(0).path("id").asText());

        mockMvc.perform(post("/api/v1/notifications/" + notificationId + "/read")
                        .header("Authorization", "Bearer " + memberToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.readAt").isNotEmpty());

        mockMvc.perform(put("/api/v1/notifications/preferences")
                        .header("Authorization", "Bearer " + memberToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"inAppEnabled\":true,\"emailEnabled\":false}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.emailEnabled").value(false));
    }

    private void bumpSeats(UUID organizationId, int seats) {
        subscriptionRepository.findByOrganizationId(organizationId).ifPresent(subscription -> {
            subscription.setSeats(seats);
            subscriptionRepository.save(subscription);
        });
    }

    private UUID userIdFromToken(String token) throws Exception {
        MvcResult me = mockMvc.perform(get("/api/v1/auth/me")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn();
        return UUID.fromString(objectMapper.readTree(me.getResponse().getContentAsString())
                .path("data").path("id").asText());
    }

    private String registerAndLogin(String email, String fullName) throws Exception {
        mockMvc.perform(post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new RegisterRequest(email, "password1234", fullName))));

        MvcResult loginResult = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequest(email, "password1234"))))
                .andExpect(status().isOk())
                .andReturn();

        return objectMapper.readTree(loginResult.getResponse().getContentAsString())
                .path("data").path("accessToken").asText();
    }

    private UUID createOrganization(String token, String slug, String name) throws Exception {
        CreateOrganizationRequest request = new CreateOrganizationRequest(name, slug, "UTC", "US");
        MvcResult result = mockMvc.perform(post("/api/v1/organizations")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn();
        return UUID.fromString(objectMapper.readTree(result.getResponse().getContentAsString())
                .path("data").path("id").asText());
    }
}
