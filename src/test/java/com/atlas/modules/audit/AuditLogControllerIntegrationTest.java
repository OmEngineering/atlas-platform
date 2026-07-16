package com.atlas.modules.audit;

import com.atlas.modules.audit.repository.AuditLogRepository;
import com.atlas.modules.auth.dto.LoginRequest;
import com.atlas.modules.auth.dto.RegisterRequest;
import com.atlas.modules.billing.repository.SubscriptionRepository;
import com.atlas.modules.invitations.repository.InvitationRepository;
import com.atlas.modules.organizations.OrganizationIntegrationTestSupport;
import com.atlas.modules.organizations.dto.CreateOrganizationRequest;
import com.atlas.modules.organizations.dto.UpdateOrganizationRequest;
import com.atlas.modules.organizations.repository.MembershipRepository;
import com.atlas.modules.organizations.repository.OrganizationRepository;
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

class AuditLogControllerIntegrationTest extends OrganizationIntegrationTestSupport {

    @Autowired
    private AuditLogRepository auditLogRepository;
    @Autowired
    private InvitationRepository invitationRepository;
    @Autowired
    private MembershipRepository membershipRepository;
    @Autowired
    private SubscriptionRepository subscriptionRepository;
    @Autowired
    private OrganizationRepository organizationRepository;

    @BeforeEach
    void cleanData() {
        auditLogRepository.deleteAll();
        invitationRepository.deleteAll();
        membershipRepository.deleteAll();
        subscriptionRepository.deleteAll();
        organizationRepository.deleteAll();
    }

    @Test
    void ownerCanReadAuditTrailAfterOrgUpdate() throws Exception {
        String ownerToken = registerAndLogin("audit-owner@example.com", "Owner");
        UUID organizationId = createOrganization(ownerToken, "audit-org", "Audit Org");

        mockMvc.perform(patch("/api/v1/organizations/" + organizationId)
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new UpdateOrganizationRequest("Audit Org Updated", null, null, null, null))))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/organizations/" + organizationId + "/audit-logs")
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items.length()").value(greaterThanOrEqualTo(2)))
                .andExpect(jsonPath("$.data.items[0].action").exists());
    }

    @Test
    void memberCannotReadAuditLogs() throws Exception {
        String ownerToken = registerAndLogin("audit-admin@example.com", "Owner");
        UUID organizationId = createOrganization(ownerToken, "audit-private", "Private Org");
        bumpSeats(organizationId, 3);

        MvcResult inviteResult = mockMvc.perform(post("/api/v1/organizations/" + organizationId + "/invitations")
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"member@example.com\",\"role\":\"MEMBER\"}"))
                .andExpect(status().isCreated())
                .andReturn();
        UUID invitationId = UUID.fromString(objectMapper.readTree(inviteResult.getResponse().getContentAsString())
                .path("data").path("id").asText());

        String memberToken = registerAndLogin("member@example.com", "Member");
        mockMvc.perform(post("/api/v1/invitations/" + invitationId + "/accept")
                        .header("Authorization", "Bearer " + memberToken))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/organizations/" + organizationId + "/audit-logs")
                        .header("Authorization", "Bearer " + memberToken))
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
        return UUID.fromString(objectMapper.readTree(result.getResponse().getContentAsString())
                .path("data").path("id").asText());
    }
}
