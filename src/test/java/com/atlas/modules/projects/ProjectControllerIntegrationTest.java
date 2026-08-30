package com.atlas.modules.projects;

import com.atlas.modules.auth.dto.LoginRequest;
import com.atlas.modules.auth.dto.RegisterRequest;
import com.atlas.modules.invitations.repository.InvitationRepository;
import com.atlas.modules.organizations.dto.CreateOrganizationRequest;
import com.atlas.modules.organizations.repository.MembershipRepository;
import com.atlas.modules.organizations.repository.OrganizationRepository;
import com.atlas.modules.billing.repository.SubscriptionRepository;
import com.atlas.modules.organizations.OrganizationIntegrationTestSupport;
import com.atlas.modules.projects.dto.CreateProjectRequest;
import com.atlas.modules.projects.repository.ProjectMembershipRepository;
import com.atlas.modules.projects.repository.ProjectRepository;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ProjectControllerIntegrationTest extends OrganizationIntegrationTestSupport {

    @Autowired
    private InvitationRepository invitationRepository;
    @Autowired
    private ProjectRepository projectRepository;
    @Autowired
    private ProjectMembershipRepository projectMembershipRepository;
    @Autowired
    private MembershipRepository membershipRepository;
    @Autowired
    private SubscriptionRepository subscriptionRepository;
    @Autowired
    private OrganizationRepository organizationRepository;

    @BeforeEach
    void cleanData() {
        invitationRepository.deleteAll();
        projectMembershipRepository.deleteAll();
        projectRepository.deleteAll();
        membershipRepository.deleteAll();
        subscriptionRepository.deleteAll();
        organizationRepository.deleteAll();
    }

    @Test
    void createsAndListsProject() throws Exception {
        String token = registerAndLogin("pm@example.com", "PM User");
        UUID organizationId = createOrganization(token, "acme", "Acme");

        CreateProjectRequest request = new CreateProjectRequest("Atlas Core", "ATL", "Main product", "ORG_WIDE");
        mockMvc.perform(post("/api/v1/organizations/" + organizationId + "/projects")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.key").value("ATL"));

        mockMvc.perform(get("/api/v1/organizations/" + organizationId + "/projects")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1));
    }

    @Test
    void rejectsDuplicateProjectKey() throws Exception {
        String token = registerAndLogin("dup@example.com", "Dup User");
        UUID organizationId = createOrganization(token, "dup-org", "Dup Org");
        CreateProjectRequest request = new CreateProjectRequest("One", "WEB", null, null);

        mockMvc.perform(post("/api/v1/organizations/" + organizationId + "/projects")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)));

        mockMvc.perform(post("/api/v1/organizations/" + organizationId + "/projects")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new CreateProjectRequest("Two", "WEB", null, null))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("PROJECT_KEY_TAKEN"));
    }

    @Test
    void hidesRestrictedProjectFromOutsiders() throws Exception {
        String ownerToken = registerAndLogin("owner@example.com", "Owner");
        UUID organizationId = createOrganization(ownerToken, "secret-org", "Secret Org");

        MvcResult createResult = mockMvc.perform(post("/api/v1/organizations/" + organizationId + "/projects")
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new CreateProjectRequest("Hidden", "HID", null, "RESTRICTED"))))
                .andExpect(status().isCreated())
                .andReturn();

        UUID projectId = UUID.fromString(objectMapper.readTree(createResult.getResponse().getContentAsString())
                .path("data").path("id").asText());

        String memberToken = registerAndLogin("outsider@example.com", "Outsider");
        inviteAndAccept(ownerToken, organizationId, "outsider@example.com", memberToken);

        mockMvc.perform(get("/api/v1/projects/" + projectId)
                        .header("Authorization", "Bearer " + memberToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void addsAndRemovesProjectMember() throws Exception {
        String ownerToken = registerAndLogin("lead@example.com", "Lead");
        UUID organizationId = createOrganization(ownerToken, "team-org", "Team Org");

        MvcResult createResult = mockMvc.perform(post("/api/v1/organizations/" + organizationId + "/projects")
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new CreateProjectRequest("Team Board", "BRD", null, "ORG_WIDE"))))
                .andExpect(status().isCreated())
                .andReturn();

        UUID projectId = UUID.fromString(objectMapper.readTree(createResult.getResponse().getContentAsString())
                .path("data").path("id").asText());

        String memberToken = registerAndLogin("dev@example.com", "Dev");
        UUID memberUserId = userIdFromToken(memberToken);
        inviteAndAccept(ownerToken, organizationId, "dev@example.com", memberToken);

        mockMvc.perform(post("/api/v1/projects/" + projectId + "/members")
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"userId\":\"" + memberUserId + "\",\"role\":\"MEMBER\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.role").value("MEMBER"));

        mockMvc.perform(delete("/api/v1/projects/" + projectId + "/members/" + memberUserId)
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isNoContent());
    }

    private void inviteAndAccept(String ownerToken, UUID organizationId, String email, String inviteeToken) throws Exception {
        subscriptionRepository.findByOrganizationId(organizationId).ifPresent(sub -> {
            sub.setSeats(5);
            subscriptionRepository.save(sub);
        });

        MvcResult inviteResult = mockMvc.perform(post("/api/v1/organizations/" + organizationId + "/invitations")
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"role\":\"MEMBER\"}"))
                .andExpect(status().isCreated())
                .andReturn();

        UUID invitationId = UUID.fromString(objectMapper.readTree(inviteResult.getResponse().getContentAsString())
                .path("data").path("id").asText());

        mockMvc.perform(post("/api/v1/invitations/" + invitationId + "/accept")
                        .header("Authorization", "Bearer " + inviteeToken))
                .andExpect(status().isOk());
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
