package com.example.CampingHub.services.Impl;

import com.example.CampingHub.dto.NuevaReserva;
import com.example.CampingHub.dto.Rango;
import com.example.CampingHub.entities.Finca;
import com.example.CampingHub.entities.Notificacion;
import com.example.CampingHub.entities.Reserva;
import com.example.CampingHub.entities.Usuario;
import com.example.CampingHub.repositories.FincaRepository;
import com.example.CampingHub.repositories.NotificacionRepository;
import com.example.CampingHub.repositories.ReservaRepository;
import com.example.CampingHub.repositories.UsuarioRepository;
import com.example.CampingHub.seguridad.ApiException;
import com.example.CampingHub.services.ReservaService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.List;
import java.util.Locale;

@Service
public class ReservaServiceImpl implements ReservaService {

    public static final ZoneId ZONA = ZoneId.of("America/Bogota");
    private static final int MAX_NOCHES = 30;

    private final ReservaRepository reservaRepository;
    private final UsuarioRepository usuarioRepository;
    private final FincaRepository fincaRepository;
    private final NotificacionRepository notificacionRepository;

    public ReservaServiceImpl(ReservaRepository reservaRepository, UsuarioRepository usuarioRepository,
                              FincaRepository fincaRepository, NotificacionRepository notificacionRepository) {
        this.reservaRepository = reservaRepository;
        this.usuarioRepository = usuarioRepository;
        this.fincaRepository = fincaRepository;
        this.notificacionRepository = notificacionRepository;
    }

    @Override
    @Transactional
    public Reserva crearReserva(NuevaReserva d, Usuario cliente) {
        if (d == null || d.idFinca() == null) throw ApiException.badRequest("Elige el lugar que quieres reservar.");
        if (d.fechaInicio() == null || d.fechaFin() == null) throw ApiException.badRequest("Elige la fecha de llegada y la de salida.");

        Finca finca = fincaRepository.findById(d.idFinca())
                .orElseThrow(() -> ApiException.notFound("Ese lugar ya no existe."));
        Usuario usuario = usuarioRepository.findById(cliente.getIdUsuario())
                .orElseThrow(() -> ApiException.notFound("Tu usuario ya no existe. Vuelve a iniciar sesión."));

        LocalDate hoy = LocalDate.now(ZONA);
        LocalDate inicio = d.fechaInicio();
        LocalDate fin = d.fechaFin();
        if (inicio.isBefore(hoy)) throw ApiException.badRequest("La fecha de llegada no puede ser en el pasado.");
        if (!fin.isAfter(inicio)) throw ApiException.badRequest("La salida debe ser al menos un día después de la llegada.");
        if (inicio.isAfter(hoy.plusYears(1))) throw ApiException.badRequest("Solo se puede reservar hasta con un año de anticipación.");
        long noches = ChronoUnit.DAYS.between(inicio, fin);
        if (noches > MAX_NOCHES) throw ApiException.badRequest("La estadía máxima es de " + MAX_NOCHES + " noches.");

        int huespedes = d.huespedes() == null ? 1 : d.huespedes();
        if (huespedes < 1) throw ApiException.badRequest("Debe haber al menos un huésped.");
        if (huespedes > finca.getCapacidad())
            throw ApiException.badRequest(finca.getNombre() + " recibe máximo " + finca.getCapacidad() + " personas.");

        if (reservaRepository.contarCruces(finca.getIdFinca(), inicio, fin) > 0)
            throw ApiException.conflict("Esas fechas ya están reservadas en " + finca.getNombre() + ". Prueba con otras.");

        Reserva r = new Reserva(usuario, finca, inicio, fin);
        r.setHuespedes(huespedes);
        r.setTotal(noches * finca.getPrecioNoche());
        r.setEstado("PENDIENTE");
        Reserva guardada = reservaRepository.save(r);

        notificar("Nueva reserva #" + guardada.getIdReserva() + " de " + usuario.getNombre() + " en " + finca.getNombre()
                + " (" + inicio + " a " + fin + ", " + pesos(guardada.getTotal()) + ")");
        return guardada;
    }

    @Override
    @Transactional(readOnly = true)
    public Reserva obtenerReservaPorId(Long id) {
        Reserva r = reservaRepository.findById(id).orElseThrow(() -> ApiException.notFound("Reserva no encontrada."));
        r.getFinca().getNombre();
        r.getUsuario().getNombre();
        return r;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Reserva> obtenerTodasLasReservas() {
        return reservaRepository.findTodasConDetalle();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Reserva> obtenerReservasDe(Usuario usuario) {
        return reservaRepository.findDelUsuario(usuario.getIdUsuario());
    }

    @Override
    @Transactional(readOnly = true)
    public List<Rango> fechasOcupadas(Long idFinca) {
        return reservaRepository.findOcupadas(idFinca, LocalDate.now(ZONA)).stream()
                .map(r -> new Rango(r.getFechaInicio(), r.getFechaFin()))
                .toList();
    }

    @Override
    @Transactional
    public Reserva confirmarReserva(Long id) {
        Reserva r = obtenerReservaPorId(id);
        if ("CANCELADA".equals(r.getEstado())) throw ApiException.badRequest("No se puede confirmar una reserva cancelada.");
        r.setEstado("CONFIRMADA");
        notificar("Reserva #" + id + " confirmada");
        return reservaRepository.save(r);
    }

    @Override
    @Transactional
    public Reserva cancelarReserva(Long id, Usuario quien) {
        Reserva r = obtenerReservaPorId(id);
        boolean esAdmin = "ADMIN".equals(quien.getRol());
        if (!esAdmin && !r.getUsuario().getIdUsuario().equals(quien.getIdUsuario()))
            throw ApiException.forbidden("Solo puedes cancelar tus propias reservas.");
        if ("CANCELADA".equals(r.getEstado())) throw ApiException.badRequest("Esta reserva ya estaba cancelada.");
        r.setEstado("CANCELADA");
        notificar("Reserva #" + id + " cancelada por " + (esAdmin ? "el administrador" : quien.getNombre()));
        return reservaRepository.save(r);
    }

    private void notificar(String mensaje) {
        Notificacion n = new Notificacion();
        n.setMensaje(mensaje.length() > 250 ? mensaje.substring(0, 250) : mensaje);
        n.setFechaEnvio(new Date());
        notificacionRepository.save(n);
    }

    static String pesos(double v) {
        NumberFormat f = NumberFormat.getCurrencyInstance(Locale.forLanguageTag("es-CO"));
        f.setMaximumFractionDigits(0);
        return f.format(v);
    }
}
