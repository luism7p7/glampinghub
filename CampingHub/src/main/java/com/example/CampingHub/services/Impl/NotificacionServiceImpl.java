package com.example.CampingHub.services.Impl;

import com.example.CampingHub.entities.Notificacion;
import com.example.CampingHub.repositories.NotificacionRepository;
import com.example.CampingHub.services.NotificacionService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class NotificacionServiceImpl implements NotificacionService {

    private final NotificacionRepository notificacionRepository;

    public NotificacionServiceImpl(NotificacionRepository notificacionRepository) {
        this.notificacionRepository = notificacionRepository;
    }

    @Override
    public Notificacion crearNotificacion(Notificacion notificacion) {
        return notificacionRepository.save(notificacion);
    }

    @Override
    public Notificacion obtenerNotificacionPorId(Long id) {
        return notificacionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Notificacion no encontrada con id: " + id));
    }

    @Override
    public List<Notificacion> obtenerTodasLasNotificaciones() {
        return notificacionRepository.findTop20ByOrderByIdNotificacionDesc();
    }

    @Override
    public Notificacion actualizarNotificacion(Notificacion notificacion) {
        // Verifica existencia antes de guardar
        obtenerNotificacionPorId(notificacion.getIdNotificacion());
        return notificacionRepository.save(notificacion);
    }

    @Override
    public void eliminarNotificacion(Long id) {
        // Verifica existencia antes de eliminar
        obtenerNotificacionPorId(id);
        notificacionRepository.deleteById(id);
    }

    @Override
    public Notificacion enviarNotificacion(Long id) {
        Notificacion n = obtenerNotificacionPorId(id);
        n.enviar();
        return n;
    }
}