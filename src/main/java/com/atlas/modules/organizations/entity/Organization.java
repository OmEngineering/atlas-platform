package com.atlas.modules.organizations.entity;

import com.atlas.shared.entity.SoftDeletableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Entity
@Table(name = "organizations")
@Getter
@Setter
public class Organization extends SoftDeletableEntity {

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, unique = true)
    private String slug;

    @Column(name = "logo_url")
    private String logoUrl;

    @Column(columnDefinition = "text")
    private String description;

    @Column(nullable = false)
    private String timezone = "UTC";

    @Column(length = 2)
    private String country;

    @Column(name = "subscription_id", unique = true)
    private UUID subscriptionId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OrganizationStatus status = OrganizationStatus.ACTIVE;

    @Column(name = "created_by", nullable = false)
    private UUID createdBy;
}
