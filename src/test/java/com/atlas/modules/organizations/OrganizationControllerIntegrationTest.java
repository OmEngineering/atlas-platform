package com.atlas.modules.organizations;

import com.atlas.modules.auth.dto.LoginRequest;
import com.atlas.modules.auth.dto.RegisterRequest;
import com.atlas.modules.organizations.dto.CreateOrganizationRequest;
import com.atlas.modules.organizations.repository.MembershipRepository;
import com.atlas.modules.organizations.repository.OrganizationRepository;
import com.atlas.modules.billing.repository.SubscriptionRepository;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class OrganizationControllerIntegrationTest extends OrganizationIntegrationTestSupport {

    @Autowired
    private OrganizationRepository organizationRepository;
    @Autowired
    private MembershipRepository membershipRepository;
    @Autowired
    private SubscriptionRepository subscriptionRepository;

    @BeforeEach
    void cleanData() {
        membershipRepository.deleteAll();
        subscriptionRepository.deleteAll();
        organizationRepository.deleteAll();
    }

    @Test
    void createsOrganizationAndOwnerMembership() throws Exception {
        String token = registerAndLogin("owner@example.com", "Owner User");

        CreateOrganizationRequest request = new CreateOrganizationRequest(
                "Acme Inc", "acme", "UTC", "IN");

        mockMvc.perform(post("/api/v1/organizations")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.name").value("Acme Inc"))
                .andExpect(jsonPath("$.data.slug").value("acme"))
                .andExpect(jsonPath("$.data.status").value("ACTIVE"));
    }

    @Test
    void rejectsDuplicateSlug() throws Exception {
        String token = registerAndLogin("dup@example.com", "Dup User");
        CreateOrganizationRequest request = new CreateOrganizationRequest("Acme", "acme", "UTC", "US");

        mockMvc.perform(post("/api/v1/organizations")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)));

        mockMvc.perform(post("/api/v1/organizations")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("SLUG_TAKEN"));
    }

    @Test
    void blocksCrossTenantOrganizationAccess() throws Exception {
        String ownerToken = registerAndLogin("tenant-a@example.com", "Tenant A");
        String outsiderToken = registerAndLogin("tenant-b@example.com", "Tenant B");

        UUID organizationId = createOrganization(ownerToken, "tenant-a", "Tenant A Org");

        mockMvc.perform(get("/api/v1/organizations/" + organizationId)
                        .header("Authorization", "Bearer " + outsiderToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void allowsAdminToUpdateOrganization() throws Exception {
        String token = registerAndLogin("admin@example.com", "Admin User");
        UUID organizationId = createOrganization(token, "admin-org", "Admin Org");

        mockMvc.perform(patch("/api/v1/organizations/" + organizationId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Updated Org\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("Updated Org"));
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
