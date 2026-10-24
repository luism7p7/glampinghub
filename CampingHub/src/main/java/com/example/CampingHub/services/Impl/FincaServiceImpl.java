package com.example.CampingHub.services.Impl;

import com.example.CampingHub.entities.Finca;
import com.example.CampingHub.repositories.FincaRepository;
import com.example.CampingHub.seguridad.ApiException;
import com.example.CampingHub.services.FincaService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
public class FincaServiceImpl implements FincaService {

    private final FincaRepository fincaRepository;

    public FincaServiceImpl(FincaRepository fincaRepository) {
        this.fincaRepository = fincaRepository;
    }

    @Override
    @Transactional
    public Finca crearFinca(Finca finca) {
        validar(finca);
        finca.setIdFinca(null);
        finca.setBase(false);
        return fincaRepository.save(finca);
    }

    @Override
    @Transactional(readOnly = true)
    public Finca obtenerFincaPorId(Long id) {
        return fincaRepository.findById(id).orElseThrow(() -> ApiException.notFound("Lugar no encontrado."));
    }

    @Override
    @Transactional(readOnly = true)
    public List<Finca> obtenerTodasLasFincas() {
        return fincaRepository.findAll();
    }

    @Override
    @Transactional
    public Finca actualizarFinca(Finca finca) {
        Finca actual = obtenerFincaPorId(finca.getIdFinca());
        if (actual.isBase()) throw ApiException.forbidden("Los lugares de ejemplo no se pueden modificar en la demostración.");
        validar(finca);
        finca.setBase(false);
        return fincaRepository.save(finca);
    }

    @Override
    @Transactional
    public void eliminarFinca(Long id) {
        Finca actual = obtenerFincaPorId(id);
        if (actual.isBase()) throw ApiException.forbidden("Los lugares de ejemplo no se pueden eliminar en la demostración.");
        fincaRepository.delete(actual);
    }

    @Override
    public Finca mostrarDisponibilidad(Long id) {
        Finca f = obtenerFincaPorId(id);
        f.mostrarDisponibilidad();
        return f;
    }

    private void validar(Finca f) {
        if (f.getNombre() == null || f.getNombre().isBlank()) throw ApiException.badRequest("Escribe el nombre del lugar.");
        if (f.getUbicacion() == null || f.getUbicacion().isBlank()) throw ApiException.badRequest("Escribe la ubicación.");
        if (f.getCapacidad() < 1 || f.getCapacidad() > 50) throw ApiException.badRequest("La capacidad debe estar entre 1 y 50 personas.");
        if (f.getPrecioNoche() < 10000 || f.getPrecioNoche() > 5000000) throw ApiException.badRequest("El precio por noche debe estar entre $10.000 y $5.000.000.");
        f.setNombre(recortar(f.getNombre(), 80));
        f.setUbicacion(recortar(f.getUbicacion(), 80));
        f.setTipo(recortar(f.getTipo() == null || f.getTipo().isBlank() ? "Glamping" : f.getTipo(), 30));
        f.setDescripcion(recortar(f.getDescripcion(), 1000));
        List<String> imgs = new ArrayList<>();
        if (f.getImagenes() != null) {
            for (String u : f.getImagenes()) {
                if (u != null && u.matches("^https://[^\\s\"'<>]{5,490}$") && imgs.size() < 5) imgs.add(u.trim());
            }
        }
        f.setImagenes(imgs);
    }

    private static String recortar(String s, int max) {
        if (s == null) return null;
        s = s.trim();
        return s.length() > max ? s.substring(0, max) : s;
    }
}
