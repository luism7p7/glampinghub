<div align="center">

# GlampingHub

**Plataforma web para encontrar y reservar glampings, cabañas, fincas y zonas de camping en Colombia.**

### [▶ Probar la app en línea](https://glampinghub-mzyu.onrender.com)

[![build](https://github.com/luism7p7/glampinghub/actions/workflows/build.yml/badge.svg)](https://github.com/luism7p7/glampinghub/actions/workflows/build.yml)
![Java](https://img.shields.io/badge/Java_21-ED8B00?style=flat-square&logo=openjdk&logoColor=white)
![Spring Boot](https://img.shields.io/badge/Spring_Boot_3.3-6DB33F?style=flat-square&logo=springboot&logoColor=white)
![MySQL](https://img.shields.io/badge/MySQL-4479A1?style=flat-square&logo=mysql&logoColor=white)
![Docker](https://img.shields.io/badge/Docker-2496ED?style=flat-square&logo=docker&logoColor=white)
![JavaScript](https://img.shields.io/badge/JavaScript-F7DF1E?style=flat-square&logo=javascript&logoColor=black)

<img src="docs/demo.gif" alt="Demo de GlampingHub" width="760" />

</div>

> **Cuentas de demostración** (también hay botones en la página):
> cliente `demo@glampinghub.co` / `demo123` · administrador `admin@glampinghub.co` / `admin123`.
> Está en un plan gratuito: la primera visita puede tardar hasta un minuto mientras el servidor despierta.

## ¿Qué hace?

- **Buscar** por destino, fechas de llegada y salida, y número de personas, mostrando qué lugares están disponibles.
- **Reservar de verdad:** se eligen fechas y huéspedes, se calcula el total y no se pueden reservar fechas ya ocupadas ni superar la capacidad del lugar.
- **Cuentas de usuario** con contraseña cifrada y sección **Mis reservas** para ver y cancelar.
- **Panel de administración:** confirmar o cancelar reservas, publicar y eliminar lugares, y ver la actividad reciente.

## Arquitectura

Aplicación **Spring Boot** por capas. El mismo servidor entrega el front-end (HTML, CSS y JavaScript en `static/`) y la API REST que este consume.

```
CampingHub/src/main/java/com/example/CampingHub
├── entities/       Finca, Reserva, Usuario (Cliente, Administrador), Sesion, Notificacion
├── repositories/   Spring Data JPA (consultas de disponibilidad y cruces de fechas)
├── services/       Lógica de negocio: validaciones de reservas, capacidad y permisos
├── controllers/    API REST
├── seguridad/      Sesiones con token, control de acceso por rol y manejo de errores
├── dto/            Respuestas sin datos sensibles
└── DatosIniciales  Carga lugares de ejemplo y cuentas de demostración
```

| Recurso | Endpoints principales | Acceso |
|---|---|---|
| `/fincas` | `GET /fincas`, `GET /fincas/{id}`, `GET /fincas/{id}/ocupadas` | público |
| `/usuarios` | `POST /usuarios` (registro), `POST /usuarios/login`, `POST /usuarios/logout`, `GET /usuarios/yo` | público / usuario |
| `/reservas` | `POST /crear`, `GET /mias`, `PUT /cancelar/{id}` | usuario |
| `/reservas` | `GET /reservas`, `PUT /confirmar/{id}` | administrador |
| `/fincas` | `POST`, `PUT /{id}`, `DELETE /{id}` | administrador |

**Seguridad:** contraseñas cifradas con BCrypt (nunca se devuelven en las respuestas), sesiones con token en la cabecera `Authorization: Bearer`, permisos por rol y credenciales de la base de datos solo en variables de entorno.

## Despliegue

- **Servidor:** contenedor Docker (ver `Dockerfile`) en Render.
- **Base de datos:** MySQL administrado en Aiven, con conexión SSL.
- **Integración continua:** GitHub Actions compila y ejecuta las pruebas (`FlujoReservaTests`) en cada cambio.

## Cómo ejecutarlo en tu computador

Requisitos: Java 21 y Maven (o el `mvnw` incluido). **No necesitas MySQL**: sin variables de entorno arranca con una base H2 en memoria y los datos de ejemplo.

```bash
cd CampingHub
./mvnw spring-boot:run
```

Abre **http://localhost:8080**.

Para usar MySQL, define estas variables antes de arrancar:

```bash
DB_URL=jdbc:mysql://localhost:3306/campinghub_db
DB_USERNAME=root
DB_PASSWORD=tu_contraseña
```

Pruebas: `./mvnw verify`

## Documentación

El diagrama UML y el informe del sistema están en [glampinghub-uml](https://github.com/luism7p7/glampinghub-uml).

---

Proyecto individual de **Luis Miguel Muñoz Rendón** · [Portafolio](https://luism7p7.github.io/#glampinghub) · [LinkedIn](https://www.linkedin.com/in/luismiguelmu%C3%B1ozrendon)
