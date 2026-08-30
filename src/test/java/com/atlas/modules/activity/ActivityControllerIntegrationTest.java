package com.atlas.modules.activity;

import com.atlas.modules.activity.repository.ActivityEntryRepository;
import com.atlas.modules.auth.dto.LoginRequest;
import com.atlas.modules.auth.dto.RegisterRequest;
import com.atlas.modules.billing.repository.SubscriptionRepository;
import com.atlas.modules.invitations.repository.InvitationRepository;
import com.atlas.modules.organizations.OrganizationIntegrationTestSupport;
import com.atlas.modules.organizations.dto.CreateOrganizationRequest;
import com.atlas.modules.organizations.repository.MembershipRepository;
import com.atlas.modules.organizations.repository.OrganizationRepository;
import com.atlas.modules.projects.dto.CreateProjectRequest;
import com.atlas.modules.projects.repository.ProjectMembershipRepository;
import com.atlas.modules.projects.repository.ProjectRepository;
import com.atlas.modules.tasks.dto.CreateTaskRequest;
import com.atlas.modules.tasks.dto.UpdateTaskRequest;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ActivityControllerIntegrationTest extends OrganizationIntegrationTestSupport {

    @Autowired
    private ActivityEntryRepository activityEntryRepository;
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
        activityEntryRepository.deleteAll();
        invitationRepository.deleteAll();
        taskRepository.deleteAll();
        projectMembershipRepository.deleteAll();
        projectRepository.deleteAll();
        membershipRepository.deleteAll();
        subscriptionRepository.deleteAll();
        organizationRepository.deleteAll();
    }

    @Test
    void recordsActivityWhenTaskStatusChanges() throws Exception {
        String ownerToken = registerAndLogin("activity-owner@example.com", "Activity Owner");
        UUID organizationId = createOrganization(ownerToken, "activity-org", "Activity Org");

        MvcResult projectResult = mockMvc.perform(post("/api/v1/organizations/" + organizationId + "/projects")
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new CreateProjectRequest("Board", "BRD", null, "ORG_WIDE"))))
                .andExpect(status().isCreated())
                .andReturn();
        UUID projectId = UUID.fromString(objectMapper.readTree(projectResult.getResponse().getContentAsString())
                .path("data").path("id").asText());

        MvcResult taskResult = mockMvc.perform(post("/api/v1/projects/" + projectId + "/tasks")
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new CreateTaskRequest("Ship activity feed", null, null, null, null, null))))
                .andExpect(status().isCreated())
                .andReturn();
        UUID taskId = UUID.fromString(objectMapper.readTree(taskResult.getResponse().getContentAsString())
                .path("data").path("id").asText());

        mockMvc.perform(patch("/api/v1/tasks/" + taskId)
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new UpdateTaskRequest(null, null, "DONE", null, null, null))))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/organizations/" + organizationId + "/activity")
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items.length()", greaterThanOrEqualTo(2)))
                .andExpect(jsonPath("$.data.items[0].eventType").value("task.status_changed"))
                .andExpect(jsonPath("$.data.items[0].summary").value(
                        org.hamcrest.Matchers.containsString("moved")))
                .andExpect(jsonPath("$.data.items[0].summary").value(
                        org.hamcrest.Matchers.containsString("Done")));
    }

    @Test
    void recordsJoinWhenInvitationAccepted() throws Exception {
        String ownerToken = registerAndLogin("activity-host@example.com", "Host");
        UUID organizationId = createOrganization(ownerToken, "join-org", "Join Org");
        bumpSeats(organizationId, 3);

        MvcResult inviteResult = mockMvc.perform(post("/api/v1/organizations/" + organizationId + "/invitations")
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"joiner@example.com\",\"role\":\"MEMBER\"}"))
                .andExpect(status().isCreated())
                .andReturn();
        UUID invitationId = UUID.fromString(objectMapper.readTree(inviteResult.getResponse().getContentAsString())
                .path("data").path("id").asText());

        String memberToken = registerAndLogin("joiner@example.com", "Joiner");
        mockMvc.perform(post("/api/v1/invitations/" + invitationId + "/accept")
                        .header("Authorization", "Bearer " + memberToken))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/organizations/" + organizationId + "/activity")
                        .header("Authorization", "Bearer " + memberToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items.length()", greaterThanOrEqualTo(1)))
                .andExpect(jsonPath("$.data.items[0].eventType").value("invitation.accepted"))
                .andExpect(jsonPath("$.data.items[0].summary").value("Joiner joined the organization"));
    }

    @Test
    void outsiderCannotReadActivity() throws Exception {
        String ownerToken = registerAndLogin("activity-private@example.com", "Owner");
        UUID organizationId = createOrganization(ownerToken, "private-feed", "Private Feed");
        String outsiderToken = registerAndLogin("outsider-activity@example.com", "Outsider");

        mockMvc.perform(get("/api/v1/organizations/" + organizationId + "/activity")
                        .header("Authorization", "Bearer " + outsiderToken))
                .andExpect(status().isForbidden());
    }

    private void bumpSeats(UUID organizationId, int seats) {
        subscriptionRepository.findByOrganizationId(organizationId).ifPresent(subscription -> {
            subscription.setSeats(seats);
            subscriptionRepository.save(subscription);
        });
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
        JsonNode idNode = objectMapper.readTree(result.getResponse().getContentAsString())
                .path("data").path("id");
        return UUID.fromString(idNode.asText());
    }
}
