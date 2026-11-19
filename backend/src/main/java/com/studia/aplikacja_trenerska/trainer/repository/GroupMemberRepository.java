package com.studia.aplikacja_trenerska.trainer.repository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;
import com.studia.aplikacja_trenerska.trainer.entity.GroupMember;
import com.studia.aplikacja_trenerska.trainer.entity.GroupMemberId;

public interface GroupMemberRepository extends JpaRepository<GroupMember, GroupMemberId> {
    List<GroupMember> findAllByGroupIdOrderByInvitedAtAscUserIdAsc(Long groupId);
    List<GroupMember> findAllByUserIdOrderByInvitedAtDescGroupIdDesc(Long userId);
    long countByGroupId(Long groupId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select m from GroupMember m where m.groupId = :groupId and m.userId = :userId")
    Optional<GroupMember> findForUpdate(@Param("groupId") Long groupId, @Param("userId") Long userId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select m from GroupMember m where m.groupId = :groupId order by m.userId")
    List<GroupMember> findAllForUpdate(@Param("groupId") Long groupId);
}
