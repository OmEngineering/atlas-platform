package com.atlas.modules.billing;

import com.atlas.modules.auth.dto.LoginRequest;
import com.atlas.modules.auth.dto.RegisterRequest;
import com.atlas.modules.billing.repository.SubscriptionRepository;
import com.atlas.modules.invitations.repository.InvitationRepository;
import com.atlas.modules.organizations.OrganizationIntegrationTestSupport;
import com.atlas.modules.organizations.dto.CreateOrganizationRequest;
import com.atlas.modules.organizations.repository.MembershipRepository;
import com.atlas.modules.organizations.repository.OrganizationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class BillingControllerIntegrationTest extends OrganizationIntegrationTestSupport {

    @Autowired
    private OrganizationRepository organizationRepository;
    @Autowired
    private MembershipRepository membershipRepository;
    @Autowired
    private SubscriptionRepository subscriptionRepository;
    @Autowired
    private InvitationRepository invitationRepository;

    @BeforeEach
    void cleanData() {
        invitationRepository.deleteAll();
        membershipRepository.deleteAll();
        subscriptionRepository.deleteAll();
        organizationRepository.deleteAll();
    }

    @Test
    void ownerCanReadSubscription() throws Exception {
        String ownerToken = registerAndLogin("billing-owner@example.com", "Owner");
        UUID organizationId = createOrganization(ownerToken, "billing-org", "Billing Org");

        mockMvc.perform(get("/api/v1/organizations/" + organizationId + "/subscription")
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.plan").value("FREE"))
                .andExpect(jsonPath("$.data.seats").value(1))
                .andExpect(jsonPath("$.data.usedSeats").value(1));
    }

    @Test
    void memberCannotAccessBilling() throws Exception {
        String ownerToken = registerAndLogin("billing-admin@example.com", "Owner");
        UUID organizationId = createOrganization(ownerToken, "billing-private", "Private Org");
        bumpSeats(organizationId, 3);

        MvcResult inviteResult = mockMvc.perform(post("/api/v1/organizations/" + organizationId + "/invitations")
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"billing-member@example.com\",\"role\":\"MEMBER\"}"))
                .andExpect(status().isCreated())
                .andReturn();
        UUID invitationId = UUID.fromString(objectMapper.readTree(inviteResult.getResponse().getContentAsString())
                .path("data").path("id").asText());

        String memberToken = registerAndLogin("billing-member@example.com", "Member");
        mockMvc.perform(post("/api/v1/invitations/" + invitationId + "/accept")
                        .header("Authorization", "Bearer " + memberToken))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/organizations/" + organizationId + "/subscription")
                        .header("Authorization", "Bearer " + memberToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void ownerCanStartCheckoutWithStubAdapter() throws Exception {
        String ownerToken = registerAndLogin("billing-checkout@example.com", "Owner");
        UUID organizationId = createOrganization(ownerToken, "billing-checkout", "Checkout Org");
        bumpSeats(organizationId, 5);

        mockMvc.perform(post("/api/v1/organizations/" + organizationId + "/subscription/checkout")
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"plan\":\"TEAM\",\"seats\":5}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.checkoutUrl").exists())
                .andExpect(jsonPath("$.data.sessionId").exists());
    }

    @Test
    void changePlanRejectsSeatsBelowActiveMembers() throws Exception {
        String ownerToken = registerAndLogin("billing-seats@example.com", "Owner");
        UUID organizationId = createOrganization(ownerToken, "billing-seats", "Seats Org");
        bumpSeats(organizationId, 5);

        MvcResult inviteResult = mockMvc.perform(post("/api/v1/organizations/" + organizationId + "/invitations")
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"billing-seats-member@example.com\",\"role\":\"MEMBER\"}"))
                .andExpect(status().isCreated())
                .andReturn();
        UUID invitationId = UUID.fromString(objectMapper.readTree(inviteResult.getResponse().getContentAsString())
                .path("data").path("id").asText());

        String memberToken = registerAndLogin("billing-seats-member@example.com", "Member");
        mockMvc.perform(post("/api/v1/invitations/" + invitationId + "/accept")
                        .header("Authorization", "Bearer " + memberToken))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/organizations/" + organizationId + "/subscription/change")
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"plan\":\"FREE\",\"seats\":1}"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.error.code").value("SEAT_LIMIT_EXCEEDED"));
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
