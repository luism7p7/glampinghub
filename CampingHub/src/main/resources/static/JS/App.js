// =================================================================
// GlampingHub · front-end
// Consume la API REST de Spring Boot servida desde el mismo dominio.
// =================================================================

const API_BASE_URL = window.location.origin;
const DEMO = {
    cliente: { correo: 'demo@glampinghub.co', clave: 'demo123' },
    admin: { correo: 'admin@glampinghub.co', clave: 'admin123' }
};

let USUARIO_ACTIVO = leerSesion();
let ALL_PROPERTIES = [];
let FILTRO = null;

// ---------------- utilidades ----------------

function leerSesion() {
    try {
        const s = JSON.parse(localStorage.getItem('GH_SESION'));
        return s && s.token ? s : null;
    } catch (e) { return null; }
}

function guardarSesion(s) {
    USUARIO_ACTIVO = s;
    try {
        if (s) localStorage.setItem('GH_SESION', JSON.stringify(s));
        else localStorage.removeItem('GH_SESION');
    } catch (e) { /* almacenamiento no disponible */ }
}

function esc(v) {
    return String(v ?? '').replace(/[&<>"']/g, c => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[c]));
}

const COP = new Intl.NumberFormat('es-CO', { style: 'currency', currency: 'COP', maximumFractionDigits: 0 });
const pesos = v => COP.format(v || 0);

function fechaISO(d) {
    const y = d.getFullYear(), m = String(d.getMonth() + 1).padStart(2, '0'), day = String(d.getDate()).padStart(2, '0');
    return `${y}-${m}-${day}`;
}
function sumarDias(iso, n) {
    const [y, m, d] = iso.split('-').map(Number);
    return fechaISO(new Date(y, m - 1, d + n));
}
function noches(inicio, fin) {
    if (!inicio || !fin) return 0;
    const a = new Date(inicio + 'T00:00:00'), b = new Date(fin + 'T00:00:00');
    return Math.round((b - a) / 86400000);
}
function fechaBonita(iso) {
    const [y, m, d] = iso.split('-').map(Number);
    return new Date(y, m - 1, d).toLocaleDateString('es-CO', { day: 'numeric', month: 'short', year: 'numeric' });
}
const HOY = fechaISO(new Date());

async function apiFetch(endpoint, options = {}) {
    const headers = { 'Content-Type': 'application/json', ...(options.headers || {}) };
    if (USUARIO_ACTIVO && USUARIO_ACTIVO.token) headers['Authorization'] = 'Bearer ' + USUARIO_ACTIVO.token;

    let response;
    try {
        response = await fetch(API_BASE_URL + endpoint, { ...options, headers });
    } catch (e) {
        throw new Error('No hay conexión con el servidor. Intenta de nuevo en unos segundos.');
    }

    if (response.status === 401 && USUARIO_ACTIVO && !endpoint.startsWith('/usuarios/login')) {
        guardarSesion(null);
        updateUIForUser();
    }
    if (response.ok) {
        if (response.status === 204) return null;
        const tipo = response.headers.get('content-type') || '';
        return tipo.includes('application/json') ? response.json() : response.text();
    }
    const texto = (await response.text()).trim();
    const mensaje = texto && texto.length < 300 && !texto.startsWith('{') ? texto : `Error ${response.status}. Intenta de nuevo.`;
    const err = new Error(mensaje);
    err.status = response.status;
    throw err;
}

function toast(mensaje, tipo = 'ok') {
    const box = document.getElementById('toasts');
    const t = document.createElement('div');
    t.className = 'gh-toast ' + tipo;
    t.textContent = mensaje;
    box.appendChild(t);
    setTimeout(() => t.classList.add('fuera'), 3800);
    setTimeout(() => t.remove(), 4400);
}

// ---------------- modales base ----------------

function openModal(type) {
    closeModal();
    document.getElementById('modal-bg').style.display = 'block';
    const m = document.getElementById(`modal-${type}`);
    m.style.display = 'block';
    m.querySelectorAll('.form-error').forEach(e => e.textContent = '');
    const first = m.querySelector('input');
    if (first) setTimeout(() => first.focus(), 50);
}

function closeModal() {
    document.getElementById('modal-bg').style.display = 'none';
    document.querySelectorAll('.gh-modal').forEach(m => m.style.display = 'none');
    document.querySelectorAll('.gh-modal-container').forEach(m => m.remove());
    document.body.classList.remove('sin-scroll');
}

function abrirPanel(html, clase = '') {
    closeModal();
    const cont = document.createElement('div');
    cont.className = 'gh-modal-container';
    cont.innerHTML = `<div class="gh-panel ${clase}" role="dialog" aria-modal="true">
        <button class="gh-close" aria-label="Cerrar" onclick="closeModal()">&times;</button>${html}</div>`;
    cont.addEventListener('click', e => { if (e.target === cont) closeModal(); });
    document.body.appendChild(cont);
    document.body.classList.add('sin-scroll');
    return cont.querySelector('.gh-panel');
}

document.addEventListener('keydown', e => { if (e.key === 'Escape') closeModal(); });

// ---------------- sesión ----------------

async function handleLogin(correo, clave) {
    const email = correo ?? document.getElementById('login-email').value.trim();
    const password = clave ?? document.getElementById('login-password').value;
    const err = document.getElementById('login-error');
    if (!email || !password) { err.textContent = 'Escribe tu correo y tu contraseña.'; return; }
    err.textContent = '';
    try {
        const s = await apiFetch('/usuarios/login', {
            method: 'POST',
            body: JSON.stringify({ correo: email, contraseña: password })
        });
        guardarSesion(s);
        closeModal();
        updateUIForUser();
        renderProperties(FILTRO);
        toast(`¡Hola, ${s.nombre}! ${s.rol === 'ADMIN' ? 'Entraste como administrador.' : 'Ya puedes reservar.'}`);
    } catch (e) {
        if (correo) toast(e.message, 'error'); else err.textContent = e.message;
    }
}

function entrarDemo(tipo) {
    const d = DEMO[tipo];
    handleLogin(d.correo, d.clave);
}

async function handleRegister() {
    const nombre = document.getElementById('register-name').value.trim();
    const correo = document.getElementById('register-email').value.trim();
    const clave = document.getElementById('register-password').value;
    const err = document.getElementById('register-error');
    if (!nombre || !correo || !clave) { err.textContent = 'Todos los campos son obligatorios.'; return; }
    err.textContent = '';
    try {
        await apiFetch('/usuarios', {
            method: 'POST',
            body: JSON.stringify({ nombre, correo, contraseña: clave })
        });
        await handleLogin(correo, clave);
        toast('Cuenta creada. ¡Bienvenido a GlampingHub!');
    } catch (e) {
        err.textContent = e.message;
    }
}

async function handleLogout() {
    try { await apiFetch('/usuarios/logout', { method: 'POST' }); } catch (e) { /* la sesión ya no existe */ }
    guardarSesion(null);
    updateUIForUser();
    renderProperties(FILTRO);
    toast('Cerraste sesión.');
}

function updateUIForUser() {
    const topBar = document.querySelector('.top-bar');
    const demo = document.getElementById('demo-bar');
    if (!topBar) return;
    const about = `<button class="btn-about" onclick="document.getElementById('about').scrollIntoView({behavior:'smooth'})">¿Quiénes somos?</button>`;
    if (USUARIO_ACTIVO) {
        const isAdmin = USUARIO_ACTIVO.rol === 'ADMIN';
        topBar.innerHTML = `
            <span class="saludo">Hola, ${esc(USUARIO_ACTIVO.nombre)}${isAdmin ? ' <small>ADMIN</small>' : ''}</span>
            ${isAdmin ? `<button class="btn-admin" onclick="openAdminPanel()">Panel de administración</button>` : ''}
            <button class="btn-register" onclick="openMisReservas()">Mis reservas</button>
            ${about}
            <button class="btn-logout" onclick="handleLogout()">Cerrar sesión</button>`;
        if (demo) demo.style.display = 'none';
    } else {
        topBar.innerHTML = `
            <button class="btn-login" onclick="openModal('login')">Iniciar sesión</button>
            <button class="btn-register" onclick="openModal('register')">Registrarse</button>
            ${about}`;
        if (demo) demo.style.display = '';
    }
}

// ---------------- listado ----------------

function aPropiedad(f) {
    return {
        id: f.idFinca,
        nombre: f.nombre,
        tipo: f.tipo || 'Glamping',
        descripcion: f.descripcion || '',
        precioNoche: f.precioNoche,
        precio: pesos(f.precioNoche),
        capacidadNum: f.capacidad,
        capacidad: `Máx. ${f.capacidad} personas`,
        locacion: f.ubicacion,
        base: f.base,
        imagenes: (f.imagenes && f.imagenes.length) ? f.imagenes : ['IMAGENES/imagen_defecto.svg']
    };
}

async function cargarFincas() {
    const container = document.getElementById('propiedades_glamping');
    container.innerHTML = '<div class="cargando"><span class="spinner"></span> Cargando lugares…</div>';
    const aviso = setTimeout(() => {
        container.innerHTML = `<div class="cargando despertando"><span class="spinner"></span>
            <div><b>Despertando el servidor…</b><br>
            Está en un plan gratuito que se duerme cuando nadie lo usa. La primera carga puede tardar hasta un minuto.</div></div>`;
    }, 2500);
    for (let intento = 0; intento < 4; intento++) {
        try {
            const fincas = await apiFetch('/fincas');
            clearTimeout(aviso);
            ALL_PROPERTIES = fincas.map(aPropiedad);
            return true;
        } catch (e) {
            if (intento === 3) {
                clearTimeout(aviso);
                container.innerHTML = `<div class="cargando error">No pudimos cargar los lugares: ${esc(e.message)}
                    <button class="btn-arrendar" onclick="initializeApp()">Reintentar</button></div>`;
                return false;
            }
            await new Promise(r => setTimeout(r, 4000));
        }
    }
}

async function renderProperties(filtered = null) {
    const container = document.getElementById('propiedades_glamping');
    if (!container) return;
    const lista = filtered || ALL_PROPERTIES;
    container.innerHTML = '';
    if (!lista.length) {
        container.innerHTML = '<div class="cargando">No hay lugares que coincidan con tu búsqueda.</div>';
        return;
    }
    lista.forEach(p => {
        const card = document.createElement('div');
        card.className = 'glamping-card';
        card.tabIndex = 0;
        const items = p.imagenes.map((src, i) =>
            `<div class="carousel-item ${i === 0 ? 'active' : ''}">
               <img src="${esc(src)}" class="d-block w-100 glamping-img" alt="${esc(p.nombre)}" loading="lazy">
             </div>`).join('');
        const indicators = p.imagenes.length > 1 ? `<div class="carousel-indicators">${p.imagenes.map((_, i) =>
            `<button type="button" data-bs-target="#carousel-${p.id}" data-bs-slide-to="${i}" ${i === 0 ? 'class="active" aria-current="true"' : ''} aria-label="Foto ${i + 1}"></button>`).join('')}</div>` : '';
        const estado = p.disponible === false ? '<span class="chip ocupado">No disponible en esas fechas</span>'
            : p.disponible === true ? '<span class="chip libre">Disponible</span>' : '';
        card.innerHTML = `
          <div id="carousel-${p.id}" class="carousel slide">
            ${indicators}
            <div class="carousel-inner">${items}</div>
            <span class="chip tipo">${esc(p.tipo)}</span>
          </div>
          <div class="card-info">
              <h5>${esc(p.nombre)}</h5>
              <p class="lugar">${esc(p.locacion)} · ${esc(p.capacidad)}</p>
              <p class="precio">${esc(p.precio)} <span>/ noche</span></p>
              ${estado}
          </div>`;
        const abrir = (e) => {
            if (e.target.closest('.carousel-indicators button')) return;
            mostrarModal(p);
        };
        card.addEventListener('click', abrir);
        card.addEventListener('keydown', e => { if (e.key === 'Enter') abrir(e); });
        container.appendChild(card);
    });
}

// ---------------- búsqueda ----------------

async function buscar() {
    const termino = document.getElementById('destino').value.toLowerCase().trim();
    const llegada = document.getElementById('fecha').value;
    const salida = document.getElementById('fecha-salida').value;
    const huespedes = parseInt(document.getElementById('huespedes').value || '0', 10);
    const titles = document.querySelector('.section-titles');

    if (llegada && salida && noches(llegada, salida) <= 0) {
        toast('La salida debe ser después de la llegada.', 'error');
        return;
    }
    let matches = ALL_PROPERTIES.filter(p =>
        (!termino || p.nombre.toLowerCase().includes(termino) || p.locacion.toLowerCase().includes(termino) || p.tipo.toLowerCase().includes(termino)) &&
        (!huespedes || p.capacidadNum >= huespedes));

    if (llegada && salida) {
        await Promise.all(matches.map(async p => {
            try {
                const ocupadas = await apiFetch(`/fincas/${p.id}/ocupadas`);
                p.disponible = !ocupadas.some(r => r.desde < salida && r.hasta > llegada);
            } catch (e) { p.disponible = undefined; }
        }));
        matches.sort((a, b) => (b.disponible === true) - (a.disponible === true));
    } else {
        ALL_PROPERTIES.forEach(p => delete p.disponible);
    }

    FILTRO = matches;
    const partes = [];
    if (termino) partes.push(`“${esc(termino)}”`);
    if (huespedes) partes.push(`${huespedes} ${huespedes === 1 ? 'persona' : 'personas'}`);
    if (llegada && salida) partes.push(`${fechaBonita(llegada)} → ${fechaBonita(salida)}`);
    titles.innerHTML = `<h2>${matches.length ? `Encontramos ${matches.length} ${matches.length === 1 ? 'lugar' : 'lugares'}` : 'Sin resultados'}</h2>
        <h3>${partes.join(' · ') || 'Todos los lugares'} <button class="link-limpiar" onclick="limpiarBusqueda()">Ver todos</button></h3>`;
    renderProperties(matches);
    titles.scrollIntoView({ behavior: 'smooth' });
}

function limpiarBusqueda() {
    FILTRO = null;
    ['destino', 'fecha', 'fecha-salida', 'huespedes'].forEach(id => document.getElementById(id).value = '');
    ALL_PROPERTIES.forEach(p => delete p.disponible);
    document.querySelector('.section-titles').innerHTML = '<h2>Encuentra parques, cabañas, fincas y espacios recreativos</h2>';
    renderProperties();
}

// ---------------- detalle y reserva ----------------

async function mostrarModal(p) {
    const llegadaBusq = document.getElementById('fecha').value;
    const salidaBusq = document.getElementById('fecha-salida').value;
    const huespBusq = parseInt(document.getElementById('huespedes').value || '0', 10);
    const llegada = llegadaBusq && llegadaBusq >= HOY ? llegadaBusq : sumarDias(HOY, 7);
    const salida = salidaBusq && salidaBusq > llegada ? salidaBusq : sumarDias(llegada, 2);
    const huesp = Math.min(Math.max(huespBusq || 2, 1), p.capacidadNum);

    const slides = p.imagenes.map((src, i) =>
        `<div class="carousel-item ${i === 0 ? 'active' : ''}"><img src="${esc(src)}" class="d-block w-100" alt="${esc(p.nombre)}"></div>`).join('');
    const controles = p.imagenes.length > 1 ? `
        <button class="carousel-control-prev" type="button" data-bs-target="#modal-carousel-${p.id}" data-bs-slide="prev"><span class="carousel-control-prev-icon" aria-hidden="true"></span><span class="visually-hidden">Anterior</span></button>
        <button class="carousel-control-next" type="button" data-bs-target="#modal-carousel-${p.id}" data-bs-slide="next"><span class="carousel-control-next-icon" aria-hidden="true"></span><span class="visually-hidden">Siguiente</span></button>` : '';
    const opcionesHuesp = Array.from({ length: p.capacidadNum }, (_, i) =>
        `<option value="${i + 1}" ${i + 1 === huesp ? 'selected' : ''}>${i + 1} ${i ? 'personas' : 'persona'}</option>`).join('');

    const panel = abrirPanel(`
        <div id="modal-carousel-${p.id}" class="carousel slide">${'<div class="carousel-inner">' + slides + '</div>'}${controles}</div>
        <div class="detalle">
          <div class="detalle-info">
            <span class="chip tipo estatico">${esc(p.tipo)}</span>
            <h2>${esc(p.nombre)}</h2>
            <p class="lugar">${esc(p.locacion)} · ${esc(p.capacidad)}</p>
            <p class="descripcion">${esc(p.descripcion)}</p>
            <div class="ocupadas" id="ocupadas"><span class="spinner chico"></span> Consultando fechas…</div>
          </div>
          <form class="reserva-box" id="form-reserva" onsubmit="return false">
            <p class="precio">${esc(p.precio)} <span>/ noche</span></p>
            <label>Llegada <input type="date" id="r-inicio" min="${HOY}" value="${llegada}"></label>
            <label>Salida <input type="date" id="r-fin" min="${sumarDias(HOY, 1)}" value="${salida}"></label>
            <label>Huéspedes <select id="r-huesp">${opcionesHuesp}</select></label>
            <div class="resumen" id="r-resumen"></div>
            <div class="form-error" id="r-error"></div>
            ${USUARIO_ACTIVO
                ? `<button type="submit" class="btn-arrendar ancho" id="r-boton">Reservar</button>`
                : `<button type="button" class="btn-arrendar ancho" onclick="openModal('login')">Inicia sesión para reservar</button>
                   <p class="nota">¿Solo quieres probar? <a href="#" onclick="entrarDemo('cliente');return false">Entra con la cuenta de demostración</a>.</p>`}
          </form>
        </div>`, 'panel-detalle');

    let ocupadas = [];
    const inicioEl = panel.querySelector('#r-inicio'), finEl = panel.querySelector('#r-fin'), huespEl = panel.querySelector('#r-huesp');
    const resumen = panel.querySelector('#r-resumen'), errEl = panel.querySelector('#r-error');
    const boton = panel.querySelector('#r-boton');

    const actualizar = () => {
        const a = inicioEl.value, b = finEl.value, n = noches(a, b);
        errEl.textContent = '';
        if (a && a < HOY) errEl.textContent = 'La llegada no puede ser en el pasado.';
        else if (a && b && n <= 0) errEl.textContent = 'La salida debe ser después de la llegada.';
        else if (n > 30) errEl.textContent = 'La estadía máxima es de 30 noches.';
        else if (a && b && ocupadas.some(r => r.desde < b && r.hasta > a)) errEl.textContent = 'Esas fechas ya están reservadas. Elige otras.';
        resumen.innerHTML = n > 0 ? `<span>${esc(p.precio)} × ${n} ${n === 1 ? 'noche' : 'noches'}</span><b>${pesos(n * p.precioNoche)}</b>` : '';
        if (boton) boton.disabled = !!errEl.textContent || n <= 0;
    };
    inicioEl.addEventListener('change', () => {
        if (inicioEl.value && finEl.value <= inicioEl.value) finEl.value = sumarDias(inicioEl.value, 1);
        finEl.min = inicioEl.value ? sumarDias(inicioEl.value, 1) : sumarDias(HOY, 1);
        actualizar();
    });
    finEl.addEventListener('change', actualizar);
    actualizar();

    if (boton) {
        panel.querySelector('#form-reserva').addEventListener('submit', async () => {
            boton.disabled = true;
            boton.textContent = 'Reservando…';
            try {
                const r = await apiFetch('/reservas/crear', {
                    method: 'POST',
                    body: JSON.stringify({ idFinca: p.id, fechaInicio: inicioEl.value, fechaFin: finEl.value, huespedes: parseInt(huespEl.value, 10) })
                });
                mostrarConfirmacion(r);
            } catch (e) {
                errEl.textContent = e.message;
                boton.disabled = false;
                boton.textContent = 'Reservar';
                if (e.status === 409) cargarOcupadas();
            }
        });
    }

    async function cargarOcupadas() {
        const box = panel.querySelector('#ocupadas');
        try {
            ocupadas = await apiFetch(`/fincas/${p.id}/ocupadas`);
            box.innerHTML = ocupadas.length
                ? `<b>Fechas ya reservadas:</b> ${ocupadas.map(r => `${fechaBonita(r.desde)} – ${fechaBonita(r.hasta)}`).join(' · ')}`
                : '✅ Todas las fechas están disponibles.';
        } catch (e) { box.textContent = ''; }
        actualizar();
    }
    cargarOcupadas();
}

function mostrarConfirmacion(r) {
    abrirPanel(`
        <div class="confirmacion">
          <div class="check">✓</div>
          <h2>¡Reserva creada!</h2>
          <p>Tu reserva <b>#${r.idReserva}</b> en <b>${esc(r.finca)}</b> quedó <span class="estado PENDIENTE">PENDIENTE</span> hasta que el administrador la confirme.</p>
          <div class="tarjeta-reserva">
            <div><span>Llegada</span><b>${fechaBonita(r.fechaInicio)}</b></div>
            <div><span>Salida</span><b>${fechaBonita(r.fechaFin)}</b></div>
            <div><span>Huéspedes</span><b>${r.huespedes}</b></div>
            <div><span>Total (${r.noches} ${r.noches === 1 ? 'noche' : 'noches'})</span><b>${pesos(r.total)}</b></div>
          </div>
          <button class="btn-arrendar" onclick="openMisReservas()">Ver mis reservas</button>
          <button class="btn-modal-secondary corto" onclick="closeModal()">Seguir explorando</button>
        </div>`, 'panel-chico');
}

// ---------------- mis reservas ----------------

function filaReserva(r, admin) {
    const acciones = [];
    if (admin && r.estado === 'PENDIENTE') acciones.push(`<button class="mini ok" onclick="accionReserva('confirmar', ${r.idReserva})">Confirmar</button>`);
    if (r.estado !== 'CANCELADA') acciones.push(`<button class="mini peligro" onclick="accionReserva('cancelar', ${r.idReserva}, ${admin})">Cancelar</button>`);
    return `<div class="fila-reserva">
        <img src="${esc(r.imagen || 'IMAGENES/imagen_defecto.svg')}" alt="">
        <div class="fila-info">
          <b>${esc(r.finca)}</b> <span class="estado ${esc(r.estado)}">${esc(r.estado)}</span>
          <small>#${r.idReserva} · ${fechaBonita(r.fechaInicio)} → ${fechaBonita(r.fechaFin)} · ${r.huespedes} huésp. · ${pesos(r.total)}</small>
          ${admin ? `<small>Cliente: ${esc(r.cliente)} (${esc(r.correoCliente)})</small>` : ''}
        </div>
        <div class="fila-acciones">${acciones.join('')}</div>
      </div>`;
}

async function openMisReservas() {
    const panel = abrirPanel(`<h2 class="panel-titulo">Mis reservas</h2><div id="lista-mias"><span class="spinner chico"></span> Cargando…</div>`, 'panel-lista');
    try {
        const lista = await apiFetch('/reservas/mias');
        panel.querySelector('#lista-mias').innerHTML = lista.length
            ? lista.map(r => filaReserva(r, false)).join('')
            : '<p class="vacio">Todavía no tienes reservas. Elige un lugar y reserva tus fechas.</p>';
    } catch (e) {
        panel.querySelector('#lista-mias').innerHTML = `<p class="form-error">${esc(e.message)}</p>`;
    }
}

async function accionReserva(accion, id, desdeAdmin) {
    if (accion === 'cancelar' && !confirmarAccion(`¿Cancelar la reserva #${id}?`)) return;
    try {
        await apiFetch(`/reservas/${accion}/${id}`, { method: 'PUT' });
        toast(accion === 'confirmar' ? `Reserva #${id} confirmada.` : `Reserva #${id} cancelada.`);
        if (desdeAdmin || document.querySelector('.panel-admin')) openAdminPanel('reservas');
        else openMisReservas();
    } catch (e) { toast(e.message, 'error'); }
}

function confirmarAccion(texto) {
    return window.confirm(texto);
}

// ---------------- panel de administración ----------------

async function openAdminPanel(pestana = 'reservas') {
    const tabs = [['reservas', 'Reservas'], ['lugares', 'Lugares'], ['nuevo', 'Agregar lugar'], ['actividad', 'Actividad']];
    const panel = abrirPanel(`
        <h2 class="panel-titulo">Panel de administración</h2>
        <div class="tabs">${tabs.map(([k, t]) => `<button class="tab ${k === pestana ? 'activa' : ''}" onclick="openAdminPanel('${k}')">${t}</button>`).join('')}</div>
        <div id="admin-contenido"><span class="spinner chico"></span> Cargando…</div>`, 'panel-lista panel-admin');
    const box = panel.querySelector('#admin-contenido');
    try {
        if (pestana === 'reservas') {
            const lista = await apiFetch('/reservas');
            const cuenta = e => lista.filter(r => r.estado === e).length;
            box.innerHTML = `<div class="kpis">
                  <div><b>${lista.length}</b><span>reservas</span></div>
                  <div><b>${cuenta('PENDIENTE')}</b><span>pendientes</span></div>
                  <div><b>${cuenta('CONFIRMADA')}</b><span>confirmadas</span></div>
                  <div><b>${pesos(lista.filter(r => r.estado !== 'CANCELADA').reduce((s, r) => s + r.total, 0))}</b><span>en reservas activas</span></div>
                </div>` + (lista.length ? lista.map(r => filaReserva(r, true)).join('') : '<p class="vacio">Aún no hay reservas.</p>');
        } else if (pestana === 'lugares') {
            await cargarFincas();
            renderProperties(FILTRO);
            box.innerHTML = ALL_PROPERTIES.map(p => `<div class="fila-reserva">
                  <img src="${esc(p.imagenes[0])}" alt="">
                  <div class="fila-info"><b>${esc(p.nombre)}</b> <span class="chip tipo estatico">${esc(p.tipo)}</span>
                    <small>${esc(p.locacion)} · ${esc(p.capacidad)} · ${esc(p.precio)} / noche</small></div>
                  <div class="fila-acciones">${p.base ? '<small class="nota">Lugar de ejemplo</small>'
                    : `<button class="mini peligro" onclick="handleDeleteFinca(${p.id})">Eliminar</button>`}</div>
                </div>`).join('');
        } else if (pestana === 'nuevo') {
            box.innerHTML = `<form class="form-finca" onsubmit="handleCreateFinca(event)">
                <label>Nombre <input id="f-nombre" required maxlength="80" placeholder="Ej: Cabaña La Montaña"></label>
                <label>Tipo <select id="f-tipo"><option>Glamping</option><option>Cabaña</option><option>Finca</option><option>Domo</option><option>Refugio</option><option>Camping</option></select></label>
                <label>Ubicación <input id="f-ubicacion" required maxlength="80" placeholder="Ej: Jardín, Antioquia"></label>
                <label>Capacidad <input id="f-capacidad" type="number" min="1" max="50" value="4" required></label>
                <label>Precio por noche (COP) <input id="f-precio" type="number" min="10000" max="5000000" step="1000" value="250000" required></label>
                <label class="ancho">Descripción <textarea id="f-descripcion" maxlength="1000" rows="3" placeholder="Qué incluye, qué hay cerca…"></textarea></label>
                <label class="ancho">Enlace de una foto (https://…) <input id="f-foto" type="url" placeholder="Opcional"></label>
                <div class="form-error ancho" id="f-error"></div>
                <button class="btn-arrendar ancho" type="submit">Guardar lugar</button>
              </form>`;
        } else {
            const lista = await apiFetch('/notificaciones');
            box.innerHTML = lista.length ? `<ul class="actividad">${lista.map(n =>
                `<li><span>${new Date(n.fechaEnvio).toLocaleString('es-CO', { dateStyle: 'medium', timeStyle: 'short' })}</span>${esc(n.mensaje)}</li>`).join('')}</ul>`
                : '<p class="vacio">Sin actividad todavía.</p>';
        }
    } catch (e) {
        box.innerHTML = `<p class="form-error">${esc(e.message)}</p>`;
    }
}

async function handleCreateFinca(ev) {
    ev.preventDefault();
    const v = id => document.getElementById(id).value.trim();
    const foto = v('f-foto');
    const finca = {
        nombre: v('f-nombre'), tipo: v('f-tipo'), ubicacion: v('f-ubicacion'),
        capacidad: parseInt(v('f-capacidad'), 10), precioNoche: parseFloat(v('f-precio')),
        descripcion: v('f-descripcion'), imagenes: foto ? [foto] : []
    };
    try {
        await apiFetch('/fincas', { method: 'POST', body: JSON.stringify(finca) });
        toast(`“${finca.nombre}” ya está publicado.`);
        openAdminPanel('lugares');
    } catch (e) {
        document.getElementById('f-error').textContent = e.message;
    }
}

async function handleDeleteFinca(id) {
    if (!confirmarAccion('¿Eliminar este lugar y sus reservas?')) return;
    try {
        await apiFetch(`/fincas/${id}`, { method: 'DELETE' });
        toast('Lugar eliminado.');
        openAdminPanel('lugares');
    } catch (e) { toast(e.message, 'error'); }
}

// ---------------- inicio ----------------

async function initializeApp() {
    updateUIForUser();
    const f = document.getElementById('fecha'), s = document.getElementById('fecha-salida');
    f.min = HOY; s.min = sumarDias(HOY, 1);
    f.addEventListener('change', () => { if (f.value) { s.min = sumarDias(f.value, 1); if (!s.value || s.value <= f.value) s.value = sumarDias(f.value, 2); } });
    document.getElementById('destino').addEventListener('keydown', e => { if (e.key === 'Enter') buscar(); });
    document.getElementById('login-password').addEventListener('keydown', e => { if (e.key === 'Enter') handleLogin(); });
    document.getElementById('register-password').addEventListener('keydown', e => { if (e.key === 'Enter') handleRegister(); });
    if (await cargarFincas()) renderProperties();
    // Si había una sesión guardada, comprobar que siga vigente
    if (USUARIO_ACTIVO) {
        try { await apiFetch('/usuarios/yo'); } catch (e) { if (e.status === 401) { guardarSesion(null); updateUIForUser(); } }
    }
}

document.addEventListener('DOMContentLoaded', initializeApp);

// Funciones usadas desde el HTML
Object.assign(window, {
    openModal, closeModal, handleLogin, handleRegister, handleLogout, entrarDemo, buscar, limpiarBusqueda,
    openMisReservas, openAdminPanel, accionReserva, handleCreateFinca, handleDeleteFinca, initializeApp
});
