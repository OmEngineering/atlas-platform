package com.atlas.modules.projects.entity;

public enum ProjectMembershipRole {
    PM,
    MEMBER,
    GUEST;

    public boolean isAtLeast(ProjectMembershipRole required) {
        return ordinal() <= required.ordinal();
    }
}
