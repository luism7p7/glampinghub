package com.example.CampingHub.repositories;

import com.example.CampingHub.entities.Sesion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface SesionRepository extends JpaRepository<Sesion, Long> {
    Optional<Sesion> findByToken(String token);

    @Modifying
    @Query("delete from Sesion s where s.expira < :ahora")
    int borrarVencidas(@Param("ahora") LocalDateTime ahora);

    @Modifying
    @Query("delete from Sesion s where s.usuario.idUsuario = :idUsuario")
    void borrarDelUsuario(@Param("idUsuario") Long idUsuario);
}
