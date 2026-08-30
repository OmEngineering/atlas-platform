package com.atlas.modules.organizations.entity;

public enum MembershipRole {
    OWNER,
    ADMIN,
    BILLING_MANAGER,
    MEMBER;

    public boolean isAtLeast(MembershipRole required) {
        return ordinal() <= required.ordinal();
    }
}
