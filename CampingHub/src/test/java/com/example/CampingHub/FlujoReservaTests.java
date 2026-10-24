package com.example.CampingHub;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.time.ZoneId;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** Prueba de punta a punta de la API: registro, sesión, reserva, cruces de fechas y permisos. */
@SpringBootTest
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class FlujoReservaTests {

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;

    private String login(String correo, String clave) throws Exception {
        String body = mvc.perform(post("/usuarios/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"correo\":\"" + correo + "\",\"contraseña\":\"" + clave + "\"}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return json.readTree(body).get("token").asText();
    }

    private long primeraFinca() throws Exception {
        String body = mvc.perform(get("/fincas")).andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(6))))
                .andExpect(jsonPath("$[0].imagenes", hasSize(3)))
                .andExpect(jsonPath("$[0].reservas").doesNotExist())
                .andReturn().getResponse().getContentAsString();
        return json.readTree(body).get(0).get("idFinca").asLong();
    }

    private String reserva(long idFinca, LocalDate inicio, LocalDate fin, int huespedes) {
        return "{\"idFinca\":" + idFinca + ",\"fechaInicio\":\"" + inicio + "\",\"fechaFin\":\"" + fin + "\",\"huespedes\":" + huespedes + "}";
    }

    @Test
    void registroNoDevuelveLaContraseñaYNoPermiteCorreoRepetido() throws Exception {
        String nuevo = "{\"nombre\":\"Ana Prueba\",\"correo\":\"ana@correo.com\",\"contraseña\":\"secreta1\"}";
        mvc.perform(post("/usuarios").contentType(MediaType.APPLICATION_JSON).content(nuevo))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.contraseña").doesNotExist())
                .andExpect(jsonPath("$.rol").value("CLIENTE"));
        mvc.perform(post("/usuarios").contentType(MediaType.APPLICATION_JSON).content(nuevo))
                .andExpect(status().isConflict());
        login("ana@correo.com", "secreta1");
        mvc.perform(post("/usuarios/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"correo\":\"ana@correo.com\",\"contraseña\":\"otra\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void reservaCompletaConValidaciones() throws Exception {
        String token = login(DatosIniciales.DEMO_CLIENTE, "demo123");
        long finca = primeraFinca();
        LocalDate hoy = LocalDate.now(ZoneId.of("America/Bogota"));
        LocalDate inicio = hoy.plusDays(40), fin = hoy.plusDays(43);

        // Sin sesión no se puede reservar
        mvc.perform(post("/reservas/crear").contentType(MediaType.APPLICATION_JSON).content(reserva(finca, inicio, fin, 2)))
                .andExpect(status().isUnauthorized());

        // Reserva válida: 3 noches, total calculado
        mvc.perform(post("/reservas/crear").header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON).content(reserva(finca, inicio, fin, 2)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.noches").value(3))
                .andExpect(jsonPath("$.estado").value("PENDIENTE"))
                .andExpect(jsonPath("$.total").value(greaterThan(0.0)));

        // Las mismas fechas ya no están disponibles
        mvc.perform(post("/reservas/crear").header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON).content(reserva(finca, inicio.plusDays(1), fin.plusDays(1), 1)))
                .andExpect(status().isConflict());

        // Fechas en el pasado, salida antes de llegada y exceso de huéspedes
        mvc.perform(post("/reservas/crear").header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON).content(reserva(finca, hoy.minusDays(2), hoy, 1)))
                .andExpect(status().isBadRequest());
        mvc.perform(post("/reservas/crear").header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON).content(reserva(finca, fin, inicio, 1)))
                .andExpect(status().isBadRequest());
        mvc.perform(post("/reservas/crear").header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON).content(reserva(finca, hoy.plusDays(60), hoy.plusDays(61), 99)))
                .andExpect(status().isBadRequest());

        // Aparece en "mis reservas" y en las fechas ocupadas
        String mias = mvc.perform(get("/reservas/mias").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        JsonNode r = json.readTree(mias).get(0);
        mvc.perform(get("/fincas/" + finca + "/ocupadas")).andExpect(jsonPath("$", hasSize(1)));

        // Un cliente no puede confirmar; el administrador sí
        long id = r.get("idReserva").asLong();
        mvc.perform(put("/reservas/confirmar/" + id).header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
        String admin = login(DatosIniciales.DEMO_ADMIN, "admin123");
        mvc.perform(put("/reservas/confirmar/" + id).header("Authorization", "Bearer " + admin))
                .andExpect(status().isOk()).andExpect(jsonPath("$.estado").value("CONFIRMADA"))
                .andExpect(jsonPath("$.correoCliente").value(containsString("***")));

        // El cliente cancela y las fechas quedan libres
        mvc.perform(put("/reservas/cancelar/" + id).header("Authorization", "Bearer " + token))
                .andExpect(status().isOk()).andExpect(jsonPath("$.estado").value("CANCELADA"));
        mvc.perform(get("/fincas/" + finca + "/ocupadas")).andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void permisosDeAdministracion() throws Exception {
        String admin = login(DatosIniciales.DEMO_ADMIN, "admin123");
        String cliente = login(DatosIniciales.DEMO_CLIENTE, "demo123");
        String nueva = "{\"nombre\":\"Cabaña de prueba\",\"ubicacion\":\"Jardín, Antioquia\",\"capacidad\":3,\"precioNoche\":200000,\"imagenes\":[\"https://images.unsplash.com/photo-1\"]}";

        mvc.perform(post("/fincas").header("Authorization", "Bearer " + cliente)
                .contentType(MediaType.APPLICATION_JSON).content(nueva)).andExpect(status().isForbidden());
        String creada = mvc.perform(post("/fincas").header("Authorization", "Bearer " + admin)
                        .contentType(MediaType.APPLICATION_JSON).content(nueva))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.base").value(false))
                .andReturn().getResponse().getContentAsString();
        long id = json.readTree(creada).get("idFinca").asLong();
        mvc.perform(delete("/fincas/" + id).header("Authorization", "Bearer " + admin)).andExpect(status().isNoContent());

        // Los lugares de ejemplo no se pueden borrar
        mvc.perform(delete("/fincas/" + primeraFinca()).header("Authorization", "Bearer " + admin))
                .andExpect(status().isForbidden());

        // El CRUD interno de usuarios no está disponible sin la clave
        mvc.perform(get("/usuarios").header("Authorization", "Bearer " + admin)).andExpect(status().isForbidden());
        mvc.perform(get("/reservas")).andExpect(status().isUnauthorized());
    }
}
