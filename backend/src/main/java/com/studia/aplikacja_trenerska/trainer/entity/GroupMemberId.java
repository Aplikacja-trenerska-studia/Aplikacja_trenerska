package com.studia.aplikacja_trenerska.trainer.entity;

import java.io.Serializable;
import java.util.Objects;

public class GroupMemberId implements Serializable {
    private Long groupId;
    private Long userId;
    public GroupMemberId() { }
    public GroupMemberId(Long groupId, Long userId) { this.groupId = groupId; this.userId = userId; }
    @Override public boolean equals(Object other) {
        return other instanceof GroupMemberId id && Objects.equals(groupId, id.groupId) && Objects.equals(userId, id.userId);
    }
    @Override public int hashCode() { return Objects.hash(groupId, userId); }
}
