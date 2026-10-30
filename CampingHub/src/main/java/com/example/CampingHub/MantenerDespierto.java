package com.example.CampingHub;

import com.example.CampingHub.repositories.FincaRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/**
 * Mantiene la app y la base de datos activas en los planes gratuitos.
 *
 * <ul>
 *   <li>Render duerme el servidor tras 15 minutos sin visitas: cada 10 minutos la app se visita
 *       a sí misma por su dirección pública ({@code RENDER_EXTERNAL_URL}, que Render entrega sola).</li>
 *   <li>Aiven apaga la base de datos gratuita si no tiene actividad: en cada latido se hace una
 *       consulta real.</li>
 * </ul>
 * En local (sin {@code RENDER_EXTERNAL_URL}) solo hace la consulta a la base de datos.
 */
@Component
public class MantenerDespierto {

    private static final Logger log = LoggerFactory.getLogger(MantenerDespierto.class);

    private final FincaRepository fincaRepository;
    private final String urlPublica;
    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(20)).build();

    public MantenerDespierto(FincaRepository fincaRepository, @Value("${RENDER_EXTERNAL_URL:}") String urlPublica) {
        this.fincaRepository = fincaRepository;
        this.urlPublica = urlPublica == null ? "" : urlPublica.replaceAll("/+$", "");
    }

    @Scheduled(initialDelayString = "PT2M", fixedDelayString = "PT10M")
    public void latido() {
        try {
            long lugares = fincaRepository.count();
            log.debug("Latido: base de datos activa ({} lugares)", lugares);
        } catch (Exception e) {
            log.warn("Latido: la base de datos no respondió: {}", e.getMessage());
        }
        if (urlPublica.isBlank()) return;
        try {
            HttpRequest req = HttpRequest.newBuilder(URI.create(urlPublica + "/salud"))
                    .timeout(Duration.ofSeconds(60)).header("User-Agent", "glampinghub-latido").GET().build();
            int estado = http.send(req, HttpResponse.BodyHandlers.discarding()).statusCode();
            log.debug("Latido: {} respondió {}", urlPublica, estado);
        } catch (Exception e) {
            log.warn("Latido: no se pudo visitar {}: {}", urlPublica, e.getMessage());
        }
    }
}
