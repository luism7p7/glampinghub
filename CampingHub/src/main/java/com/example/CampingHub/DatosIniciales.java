package com.example.CampingHub;

import com.example.CampingHub.entities.Administrador;
import com.example.CampingHub.entities.Cliente;
import com.example.CampingHub.entities.Finca;
import com.example.CampingHub.entities.Usuario;
import com.example.CampingHub.repositories.FincaRepository;
import com.example.CampingHub.repositories.UsuarioRepository;
import com.example.CampingHub.seguridad.AuthService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Carga los datos de ejemplo cuando la base de datos está vacía y deja listas
 * las cuentas de demostración (cliente y administrador).
 */
@Component
public class DatosIniciales implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DatosIniciales.class);

    public static final String DEMO_CLIENTE = "demo@glampinghub.co";
    public static final String DEMO_ADMIN = "admin@glampinghub.co";

    private final FincaRepository fincaRepository;
    private final UsuarioRepository usuarioRepository;
    private final AuthService authService;

    public DatosIniciales(FincaRepository fincaRepository, UsuarioRepository usuarioRepository, AuthService authService) {
        this.fincaRepository = fincaRepository;
        this.usuarioRepository = usuarioRepository;
        this.authService = authService;
    }

    @Override
    @Transactional
    public void run(String... args) {
        if (fincaRepository.count() == 0) {
            fincaRepository.saveAll(fincasDeEjemplo());
            log.info("Se cargaron {} lugares de ejemplo", fincaRepository.count());
        }
        asegurarCuenta(DEMO_CLIENTE, "Visitante", "demo123", false);
        asegurarCuenta(DEMO_ADMIN, "Administrador", "admin123", true);
        int borradas = authService.limpiarVencidas();
        if (borradas > 0) log.info("Se borraron {} sesiones vencidas", borradas);
    }

    /** Crea la cuenta si no existe y restablece su contraseña de demostración. */
    private void asegurarCuenta(String correo, String nombre, String clave, boolean admin) {
        Usuario u = usuarioRepository.findByCorreoIgnoreCase(correo).orElse(null);
        if (u == null) {
            u = admin ? new Administrador(nombre, correo, clave) : new Cliente(nombre, correo, clave);
        } else if (!u.contraseñaCoincide(clave)) {
            u.setContraseña(clave);
        } else {
            return;
        }
        usuarioRepository.save(u);
    }

    private static String img(String id) {
        return "https://images.unsplash.com/" + id + "?auto=format&fit=crop&w=1200&q=80";
    }

    private static Finca finca(String nombre, String tipo, String ubicacion, int capacidad, double precio,
                               String descripcion, String... fotos) {
        Finca f = new Finca(null, capacidad, ubicacion, nombre, precio);
        f.setTipo(tipo);
        f.setDescripcion(descripcion);
        f.setImagenes(new ArrayList<>(Arrays.stream(fotos).map(DatosIniciales::img).toList()));
        f.setBase(true);
        return f;
    }

    static List<Finca> fincasDeEjemplo() {
        return List.of(
                finca("Glamping Bosque Andino", "Glamping", "Guatavita, Cundinamarca", 4, 280000,
                        "Carpa de lona con cama doble, baño privado y deck con vista al embalse de Tominé. Incluye desayuno campesino y fogata en la noche.",
                        "photo-1502856755506-d8626589ef19", "photo-1470265164177-33def6c4a87e", "photo-1593053272490-e0ed6d6a42c5"),
                finca("Cabaña del Lago", "Cabaña", "Guatapé, Antioquia", 6, 420000,
                        "Cabaña de madera a orillas del embalse, con kayaks, muelle privado y cocina equipada. A 10 minutos de la Piedra del Peñol.",
                        "photo-1470770841072-f978cf4d019e", "photo-1552335270-a1cdfc205557", "photo-1762119594738-fb27265d5d03"),
                finca("Finca El Cafetal", "Finca", "Salento, Quindío", 10, 650000,
                        "Casa de finca cafetera con cinco habitaciones, piscina y recorrido por el cultivo de café. Cerca del Valle del Cocora.",
                        "photo-1611781638782-0d3cae1cd22e", "photo-1613498248726-8922766cebdb", "photo-1666990986811-9a6dcc77cfb0"),
                finca("Domo Estrellado", "Domo", "Villa de Leyva, Boyacá", 2, 350000,
                        "Domo geodésico con techo transparente para ver las estrellas, jacuzzi al aire libre y chimenea. Ideal para parejas.",
                        "photo-1761788457227-7b50e0f63c87", "photo-1761788457326-d14fad23b308", "photo-1779713802753-ccea01c1c651"),
                finca("Refugio de Montaña", "Refugio", "Sierra Nevada de Santa Marta", 5, 310000,
                        "Refugio entre la selva y la montaña, con hamacas, río cercano y caminatas guiadas a miradores y cascadas.",
                        "photo-1666113907288-076e616f0b1e", "photo-1682020193756-4f1948a1de17", "photo-1661805945058-1012dc588d7e"),
                finca("Camping Río Claro", "Camping", "Río Claro, Antioquia", 8, 180000,
                        "Zona de camping junto al río de mármol, con carpas equipadas, baños, zona de asados y actividades de rafting y espeleología.",
                        "photo-1657664069074-8b245ee46be9", "photo-1633524792530-25a41459e618", "photo-1738838072982-d0e578967643")
        );
    }
}
