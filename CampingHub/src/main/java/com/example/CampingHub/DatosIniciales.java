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
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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
        // Agrega los lugares de ejemplo que falten (por nombre), sin tocar los que ya existen.
        Set<String> existentes = new HashSet<>();
        for (Finca f : fincaRepository.findAll()) {
            if (f.getNombre() != null) existentes.add(f.getNombre().trim().toLowerCase());
        }
        List<Finca> nuevas = fincasDeEjemplo().stream()
                .filter(f -> !existentes.contains(f.getNombre().trim().toLowerCase()))
                .toList();
        if (!nuevas.isEmpty()) {
            fincaRepository.saveAll(nuevas);
            log.info("Se cargaron {} lugares de ejemplo (total: {})", nuevas.size(), fincaRepository.count());
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
                        "photo-1657664069074-8b245ee46be9", "photo-1633524792530-25a41459e618", "photo-1738838072982-d0e578967643"),
                finca("Cabaña Playa Palomino", "Cabaña", "Palomino, La Guajira", 4, 390000,
                        "Cabaña de madera frente al mar Caribe, entre palmeras y a pocos pasos del río Palomino. Incluye desayuno y hamacas en la terraza.",
                        "photo-1785576636006-07accba76866", "photo-1620244919974-ba9a164e4b76", "photo-1702743599501-a821d0b38b66"),
                finca("Glamping Desierto de la Tatacoa", "Glamping", "Villavieja, Huila", 2, 330000,
                        "Domo en medio del desierto, con cama doble, terraza y telescopio para ver uno de los cielos más despejados de Colombia.",
                        "photo-1762254799629-b8542e611aa7", "photo-1762255097010-49e99eadd616", "photo-1783696177820-a0f8d10065b6"),
                finca("Finca Río Fonce", "Finca", "San Gil, Santander", 12, 720000,
                        "Casa campestre con piscina y zona verde, cerca del río Fonce y de Barichara. Punto de partida para rafting, parapente y caminatas.",
                        "photo-1714395009352-9aadc86a69bd", "photo-1733153453347-f80cf175fca5", "photo-1784571208419-207020aa09ac"),
                finca("Ecolodge Amazonas", "Refugio", "Leticia, Amazonas", 6, 460000,
                        "Cabañas de palma dentro de la selva, con senderos iluminados, avistamiento de delfines rosados y recorridos en canoa por el río.",
                        "photo-1780686222657-1ba6a42ee4d8", "photo-1761950116791-8bfc445e5186", "photo-1775195560641-4fee7a5a7bfe"),
                finca("Cabaña Jardín Colonial", "Cabaña", "Jardín, Antioquia", 5, 300000,
                        "Cabaña en las montañas del suroeste antioqueño, con vista a los cafetales y a 10 minutos del parque principal de Jardín.",
                        "photo-1789638720830-c2d12b03afbe", "photo-1729866293555-d86cc3e8ab25", "photo-1789638720797-6d6fd066c9d7"),
                finca("Camping Playa Tayrona", "Camping", "Parque Tayrona, Magdalena", 6, 160000,
                        "Zona de camping a la sombra junto a la playa, con carpas equipadas, duchas y caminatas por la selva hasta Cabo San Juan.",
                        "photo-1584574985085-f7191d6eb34b", "photo-1652118976116-951b8d331484", "photo-1688435034170-ca82aba7b644")
        );
    }
}
