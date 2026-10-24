package com.example.CampingHub.repositories;

import com.example.CampingHub.entities.Reserva;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface ReservaRepository extends JpaRepository<Reserva, Long> {

    @Query("select r from Reserva r join fetch r.finca join fetch r.usuario where r.usuario.idUsuario = :idUsuario order by r.fechaInicio desc")
    List<Reserva> findDelUsuario(@Param("idUsuario") Long idUsuario);

    @Query("select r from Reserva r join fetch r.finca join fetch r.usuario order by r.creada desc")
    List<Reserva> findTodasConDetalle();

    /** Reservas activas (no canceladas) de una finca que se cruzan con el rango [inicio, fin). */
    @Query("select count(r) from Reserva r where r.finca.idFinca = :idFinca and r.estado <> 'CANCELADA' " +
           "and r.fechaInicio < :fin and r.fechaFin > :inicio")
    long contarCruces(@Param("idFinca") Long idFinca, @Param("inicio") LocalDate inicio, @Param("fin") LocalDate fin);

    @Query("select r from Reserva r where r.finca.idFinca = :idFinca and r.estado <> 'CANCELADA' and r.fechaFin > :desde order by r.fechaInicio")
    List<Reserva> findOcupadas(@Param("idFinca") Long idFinca, @Param("desde") LocalDate desde);

    @Modifying
    @Query("delete from Reserva r where r.usuario.idUsuario = :idUsuario")
    void borrarDelUsuario(@Param("idUsuario") Long idUsuario);
}
