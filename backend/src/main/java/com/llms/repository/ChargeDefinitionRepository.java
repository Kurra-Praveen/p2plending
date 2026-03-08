package com.llms.repository;

import com.llms.entity.ChargeDefinition;
import com.llms.enums.ChargeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ChargeDefinitionRepository extends JpaRepository<ChargeDefinition, UUID> {

    Optional<ChargeDefinition> findByCode(String code);

    List<ChargeDefinition> findByIsActiveTrue();

    List<ChargeDefinition> findByChargeTypeAndIsActiveTrue(ChargeType chargeType);

    List<ChargeDefinition> findByIsMandatoryTrueAndIsActiveTrue();

    boolean existsByCode(String code);
}
