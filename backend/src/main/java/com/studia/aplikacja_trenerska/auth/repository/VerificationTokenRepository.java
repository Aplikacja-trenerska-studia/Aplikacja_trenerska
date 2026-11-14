package com.studia.aplikacja_trenerska.auth.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.studia.aplikacja_trenerska.auth.entity.VerificationToken;
import com.studia.aplikacja_trenerska.auth.entity.VerificationTokenType;

public interface VerificationTokenRepository extends JpaRepository<VerificationToken, Long> {

	Optional<VerificationToken> findByToken(String token);

	Optional<VerificationToken> findByTokenAndType(String token, VerificationTokenType type);

	Optional<VerificationToken> findFirstByUserIdAndTypeOrderByCreatedAtDesc(Long userId, VerificationTokenType type);

	@Modifying(flushAutomatically = true)
	@Query("delete from VerificationToken t where t.user.id = :userId and t.type = :type")
	void deleteByUserIdAndType(@Param("userId") Long userId, @Param("type") VerificationTokenType type);

	@Modifying(flushAutomatically = true)
	@Query("delete from VerificationToken t where t.user.id = :userId")
	void deleteByUserId(@Param("userId") Long userId);
}
