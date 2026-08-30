package com.atlas.modules.invitations;

import com.atlas.modules.auth.dto.LoginRequest;
import com.atlas.modules.auth.dto.RegisterRequest;
import com.atlas.modules.billing.repository.SubscriptionRepository;
import com.atlas.modules.invitations.dto.CreateInvitationRequest;
import com.atlas.modules.invitations.repository.InvitationRepository;
import com.atlas.modules.organizations.dto.CreateOrganizationRequest;
import com.atlas.modules.organizations.repository.MembershipRepository;
import com.atlas.modules.organizations.repository.OrganizationRepository;
import com.atlas.modules.organizations.OrganizationIntegrationTestSupport;
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

class InvitationControllerIntegrationTest extends OrganizationIntegrationTestSupport {

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
        invitationRepository.deleteAll();
        membershipRepository.deleteAll();
        subscriptionRepository.deleteAll();
        organizationRepository.deleteAll();
    }

    @Test
    void inviteAcceptAndManageMember() throws Exception {
        String ownerToken = registerAndLogin("owner@example.com", "Owner");
        UUID organizationId = createOrganization(ownerToken, "acme", "Acme");
        bumpSeats(organizationId, 3);

        CreateInvitationRequest invite = new CreateInvitationRequest("member@example.com", "MEMBER");
        MvcResult inviteResult = mockMvc.perform(post("/api/v1/organizations/" + organizationId + "/invitations")
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invite)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.email").value("member@example.com"))
                .andExpect(jsonPath("$.data.status").value("PENDING"))
                .andReturn();

        UUID invitationId = UUID.fromString(objectMapper.readTree(inviteResult.getResponse().getContentAsString())
                .path("data").path("id").asText());

        mockMvc.perform(get("/api/v1/organizations/" + organizationId + "/invitations")
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1));

        String memberToken = registerAndLogin("member@example.com", "Member");
        MvcResult acceptResult = mockMvc.perform(post("/api/v1/invitations/" + invitationId + "/accept")
                        .header("Authorization", "Bearer " + memberToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.role").value("MEMBER"))
                .andReturn();

        UUID membershipId = UUID.fromString(objectMapper.readTree(acceptResult.getResponse().getContentAsString())
                .path("data").path("id").asText());

        mockMvc.perform(patch("/api/v1/memberships/" + membershipId)
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"role\":\"ADMIN\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.role").value("ADMIN"));

        mockMvc.perform(delete("/api/v1/memberships/" + membershipId)
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isNoContent());
    }

    @Test
    void blocksInviteWhenSeatsFull() throws Exception {
        String ownerToken = registerAndLogin("full@example.com", "Owner");
        UUID organizationId = createOrganization(ownerToken, "full-org", "Full Org");

        mockMvc.perform(post("/api/v1/organizations/" + organizationId + "/invitations")
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new CreateInvitationRequest("extra@example.com", "MEMBER"))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("SEAT_LIMIT_EXCEEDED"));
    }

    @Test
    void revokesPendingInvitation() throws Exception {
        String ownerToken = registerAndLogin("revoke@example.com", "Owner");
        UUID organizationId = createOrganization(ownerToken, "revoke-org", "Revoke Org");
        bumpSeats(organizationId, 2);

        MvcResult inviteResult = mockMvc.perform(post("/api/v1/organizations/" + organizationId + "/invitations")
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new CreateInvitationRequest("gone@example.com", "MEMBER"))))
                .andExpect(status().isCreated())
                .andReturn();

        UUID invitationId = UUID.fromString(objectMapper.readTree(inviteResult.getResponse().getContentAsString())
                .path("data").path("id").asText());

        mockMvc.perform(delete("/api/v1/invitations/" + invitationId)
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/v1/organizations/" + organizationId + "/invitations")
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(0));
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
