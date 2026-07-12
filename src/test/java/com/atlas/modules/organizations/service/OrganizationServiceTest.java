package com.atlas.modules.organizations.service;

import com.atlas.exception.ApiException;
import com.atlas.modules.auth.entity.User;
import com.atlas.modules.auth.entity.UserStatus;
import com.atlas.modules.billing.repository.SubscriptionRepository;
import com.atlas.modules.billing.service.SubscriptionService;
import com.atlas.modules.organizations.dto.CreateOrganizationRequest;
import com.atlas.modules.organizations.entity.Organization;
import com.atlas.modules.organizations.repository.MembershipRepository;
import com.atlas.modules.organizations.repository.OrganizationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrganizationServiceTest {

    @Mock
    private OrganizationRepository organizationRepository;
    @Mock
    private MembershipRepository membershipRepository;
    @Mock
    private SubscriptionRepository subscriptionRepository;
    @Mock
    private SubscriptionService subscriptionService;
    @Mock
    private MembershipAuthorizationService membershipAuthorizationService;

    private OrganizationService organizationService;

    @BeforeEach
    void setUp() {
        organizationService = new OrganizationService(
                organizationRepository,
                membershipRepository,
                subscriptionRepository,
                subscriptionService,
                membershipAuthorizationService);
    }

    @Test
    void createOrganizationRejectsDuplicateSlug() {
        User creator = verifiedUser();
        when(organizationRepository.existsBySlugIgnoreCase("acme")).thenReturn(true);

        assertThatThrownBy(() -> organizationService.createOrganization(
                new CreateOrganizationRequest("Acme", "acme", "UTC", "US"),
                creator))
                .isInstanceOf(ApiException.class);
    }

    private User verifiedUser() {
        User user = new User();
        user.setId(UUID.randomUUID());
        user.setEmail("user@example.com");
        user.setFullName("User");
        user.setEmailVerifiedAt(Instant.now());
        user.setStatus(UserStatus.ACTIVE);
        return user;
    }
}
