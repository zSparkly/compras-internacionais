package com.aula.pos.compras.repository;

import com.aula.pos.compras.entity.Cliente;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

public interface ClienteRepository extends JpaRepository<Cliente, UUID> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from Cliente c where c.id = :id")
    Optional<Cliente> findByIdForUpdate(@Param("id") UUID id);
}
