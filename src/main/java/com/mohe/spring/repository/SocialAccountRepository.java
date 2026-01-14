package com.mohe.spring.repository;

import com.mohe.spring.entity.SocialAccount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SocialAccountRepository extends JpaRepository<SocialAccount, Long> {

    Optional<SocialAccount> findByProviderAndProviderId(String provider, String providerId);

    Optional<SocialAccount> findByProviderAndProviderEmail(String provider, String providerEmail);

    List<SocialAccount> findByUserId(Long userId);

    Optional<SocialAccount> findByUserIdAndProvider(Long userId, String provider);

    boolean existsByProviderAndProviderId(String provider, String providerId);

    boolean existsByUserIdAndProvider(Long userId, String provider);

    @Query("DELETE FROM SocialAccount sa WHERE sa.user.id = :userId AND sa.provider = :provider")
    void deleteByUserIdAndProvider(@Param("userId") Long userId, @Param("provider") String provider);
}
