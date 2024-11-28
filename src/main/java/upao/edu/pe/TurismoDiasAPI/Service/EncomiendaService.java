package upao.edu.pe.TurismoDiasAPI.Service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import upao.edu.pe.TurismoDiasAPI.DTO.EncomiendaDTO;
import upao.edu.pe.TurismoDiasAPI.DTO.HistorialEncomiendaDTO;
import upao.edu.pe.TurismoDiasAPI.Entity.Encomienda;
import upao.edu.pe.TurismoDiasAPI.Entity.HistorialEncomienda;
import upao.edu.pe.TurismoDiasAPI.Repository.EncomiendaRepository;
import upao.edu.pe.TurismoDiasAPI.Repository.HistorialEncomiendaRepository;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class EncomiendaService {

    private final EncomiendaRepository encomiendaRepository;
    private final HistorialEncomiendaRepository historialEncomiendaRepository;
    private final HistorialEncomiendaService historialEncomiendaService;
    SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy");
    private static final Logger logger = LoggerFactory.getLogger(EncomiendaService.class);

    @Value("${infobip.api.key}")
    private String INFOBIP_API_KEY;

    @Value("${infobip.sender.name}")
    private String INFOBIP_SENDER_NAME;

    // Listar las encomiendas por ID
    public Optional<Encomienda> obtenerEncomiendaPorId(Integer id_encomienda) {
        return encomiendaRepository.findById(id_encomienda);
    }

    // Método programado para actualizar el estado de las encomiendas cada 2 minutos
    @Scheduled(fixedDelay = 2, timeUnit = TimeUnit.MINUTES, initialDelay = 2) // Cada 2 minutos
    public void actualizarEstadosEncomiendas() {
        logger.info("Actualizando estado de las encomiendas");
        // Envolver "En tránsito" en una lista
        List<Encomienda> encomiendasPendientes = encomiendaRepository.findByEstadoIn(Collections.singletonList("Pendiente"));
        List<Encomienda> encomiendasEnTransito = encomiendaRepository.findByEstadoIn(Collections.singletonList("En tránsito"));
        for (Encomienda encomienda : encomiendasPendientes) {
            iniciarEnvioEncomienda(encomienda);
        }
        for (Encomienda encomienda : encomiendasEnTransito) {
            actualizarProgresoEnvio(encomienda);
        }
    }

    // Iniciar el envío para encomiendas pendientes
    private void iniciarEnvioEncomienda(Encomienda encomienda) {
        // Crear el primer historial de envío y cambiar estado a "En tránsito"
        agregarHistorial(encomienda, "Inicio de envío", encomienda.getCiudad_origen(), "En tránsito",
                "El paquete acaba de comenzar su viaje.");
        encomienda.setEstado("En tránsito");
        encomienda.setFecha_envio(new Date());
        encomiendaRepository.save(encomienda);
    }

    // Actualizar el progreso del envío cada 10 minutos
    private void actualizarProgresoEnvio(Encomienda encomienda) {
        long minutosTranscurridos = calcularMinutosDesdeUltimoEvento(encomienda.getFecha_envio());
        int cantidadHorasViaje = encomienda.getCant_horas_viaje();
        //int minutosViaje = cantidadHorasViaje;
        int minutosViaje = cantidadHorasViaje * 60;
        List<HistorialEncomiendaDTO> ultimosHistoriales = historialEncomiendaService.listarHistorialPorEncomiendaId(encomienda.getIdEncomienda());
        if (minutosTranscurridos >= minutosViaje - 10) {
            // Cerca de llegar al destino
            boolean validacionViajeCerca = false;
            for(HistorialEncomiendaDTO historial : ultimosHistoriales){
                if(historial.getDescripcion_evento().equals("El paquete está cerca de llegar a la ciudad de destino.")){
                    validacionViajeCerca = true;
                    break;
                }
            }
            if(!validacionViajeCerca){
                agregarHistorial(encomienda, "Cerca de destino", encomienda.getCiudad_destino(), "En tránsito",
                        "El paquete está cerca de llegar a la ciudad de destino.");
            }
        }
        if (minutosTranscurridos >= minutosViaje) {
            // Si el tiempo de viaje terminó, agregar historial de llegada
            boolean validacion = false;
            for(HistorialEncomiendaDTO historial : ultimosHistoriales){
                if(historial.getDescripcion_evento().equals("El paquete está llegando a la ciudad de destino.")){
                    validacion = true;
                    break;
                }
            }
            if(!validacion){
                agregarHistorial(encomienda, "Llegada a ciudad destino", encomienda.getCiudad_destino(), "En tránsito",
                        "El paquete está llegando a la ciudad de destino.");
            }
            if(validacion){
                esperarAlmacenaje(encomienda);
            }
        }
    }

    // Esperar en almacén antes de la entrega final
    private void esperarAlmacenaje(Encomienda encomienda) {
        List<HistorialEncomiendaDTO> ultimoHistorial = historialEncomiendaService.listarHistorialPorEncomiendaId(encomienda.getIdEncomienda());
        boolean validacion = false;

        // Verificar si ya existe el historial de "llegó al almacén"
        for (HistorialEncomiendaDTO historial : ultimoHistorial) {
            if (historial.getDescripcion_evento().equals("El paquete ha llegado al almacén de la sucursal.")) {
                validacion = true;
                break;
            }
        }
        // Si no existe, agregarlo
        if (!validacion) {
            agregarHistorial(encomienda, "Almacén de sucursal", encomienda.getCiudad_destino(), "En tránsito",
                    "El paquete ha llegado al almacén de la sucursal.");
        }
    }

    // Entrega a domicilio
    public void prepararEntregaADomicilio(Encomienda encomienda, String claveIngresada) {
        List<HistorialEncomiendaDTO> ultimoHistorial = historialEncomiendaService.listarHistorialPorEncomiendaId(encomienda.getIdEncomienda());
        boolean validacion = false;

        // Validar si el historial ya contiene el evento de "Camino a entrega a domicilio"
        for (HistorialEncomiendaDTO historial : ultimoHistorial) {
            if (historial.getDescripcion_evento().equals("El encargado está llevando el paquete a la dirección registrada.")) {
                validacion = true;
                break;
            }
        }

        // Si no existe, agregar el historial de "Camino a entrega a domicilio"
        if (!validacion) {
            agregarHistorial(encomienda, "Camino a entrega a domicilio", encomienda.getCiudad_destino(), "En tránsito",
                    "El encargado está llevando el paquete a la dirección registrada.");
        }

        // Revalidar después de agregar
        validacion = false;
        for (HistorialEncomiendaDTO historial : ultimoHistorial) {
            if (historial.getDescripcion_evento().equals("El encargado está llevando el paquete a la dirección registrada.")) {
                validacion = true;
                break;
            }
        }

        // Si el paquete está listo para entregarse, validar la clave antes de finalizar la entrega
        if (validacion) {
            if (encomienda.getClave_secreta().equals(claveIngresada)) {
                finalizarEntrega(encomienda, "Se entregó el paquete al cliente.");
            } else {
                throw new IllegalArgumentException("La clave secreta no coincide.");
            }
        }
    }

    // Entrega en tienda
    public void prepararEntregaRecojoEnTienda(Encomienda encomienda, String claveIngresada) {
        List<HistorialEncomiendaDTO> ultimoHistorial = historialEncomiendaService.listarHistorialPorEncomiendaId(encomienda.getIdEncomienda());
        boolean validacion = false;

        // Validar si el historial ya contiene el evento de "Listo para recojo en tienda"
        for (HistorialEncomiendaDTO historial : ultimoHistorial) {
            if (historial.getDescripcion_evento().equals("El paquete está listo para que el cliente lo recoja en la tienda.")) {
                validacion = true;
                break;
            }
        }

        // Si no existe, agregar el historial de "Listo para recojo en tienda"
        if (!validacion) {
            agregarHistorial(encomienda, "Listo para recojo en tienda", encomienda.getCiudad_destino(), "En tránsito",
                    "El paquete está listo para que el cliente lo recoja en la tienda.");
        }

        // Revalidar después de agregar
        validacion = false;
        for (HistorialEncomiendaDTO historial : ultimoHistorial) {
            if (historial.getDescripcion_evento().equals("El paquete está listo para que el cliente lo recoja en la tienda.")) {
                validacion = true;
                break;
            }
        }

        // Si el paquete está listo para entregarse, validar la clave antes de finalizar la entrega
        if (validacion) {
            if (encomienda.getClave_secreta().equals(claveIngresada)) {
                finalizarEntrega(encomienda, "El cliente recogió el paquete en la tienda.");
            } else {
                throw new IllegalArgumentException("La clave secreta no coincide.");
            }
        }
    }

    // Método para finalizar la entrega y cambiar el estado a "Entregado"
    private void finalizarEntrega(Encomienda encomienda, String descripcion) {
        agregarHistorial(encomienda, "Entrega finalizada", encomienda.getCiudad_destino(), "Entregado", descripcion);
        encomienda.setEstado("Entregado");
        encomienda.setFecha_entrega(new Date());
        encomiendaRepository.save(encomienda);
    }

    // Método auxiliar para calcular minutos transcurridos desde el último evento
    private long calcularMinutosDesdeUltimoEvento(Date fechaEvento) {
        long diferenciaMilisegundos = new Date().getTime() - fechaEvento.getTime();
        return diferenciaMilisegundos / (1000 * 60); // Convertir de ms a minutos
    }

    // Método auxiliar para agregar historial de eventos
    private void agregarHistorial(Encomienda encomienda, String tipoEvento, String lugarActual, String estadoActual, String descripcionEvento) {
        HistorialEncomienda historial = new HistorialEncomienda();
        historial.setFecha_evento(new Date());
        historial.setTipo_evento(tipoEvento);
        historial.setLugar_actual(lugarActual);
        historial.setEstado_actual(estadoActual);
        historial.setDescripcion_evento(descripcionEvento);
        historial.setEncomienda(encomienda);
        historialEncomiendaRepository.save(historial);
    }

    public void omitirViajeYActualizarEstado(Encomienda encomienda) {
        if (encomienda.getEstado().equals("Pendiente")) {
            // Inicio del viaje
            agregarHistorial(encomienda, "Aceptación", encomienda.getCiudad_origen(), "Pendiente", "Se aceptó el paquete");
            encomienda.setEstado("En tránsito");
            encomienda.setFecha_envio(new Date());
            agregarHistorial(encomienda, "Envío", encomienda.getCiudad_origen(), "En tránsito", "El paquete salió de " + encomienda.getCiudad_origen());
        }

        if (encomienda.getEstado().equals("En tránsito")) {
            // Llegada a la ciudad de destino
            agregarHistorial(encomienda, "Recepción", encomienda.getCiudad_destino(), "En tránsito", "Llegó a la sucursal de " + encomienda.getCiudad_destino());
            encomienda.setEstado("En almacén");
        }

        if (encomienda.getEstado().equals("En almacén")) {
            if (encomienda.getTipo_entrega().equals("Domicilio")) {
                // Entrega a domicilio
                agregarHistorial(encomienda, "Entrega", encomienda.getCiudad_destino(), "En tránsito", "El encargado está llevando los paquetes respectivos");
                encomienda.setEstado("Entregado");
                encomienda.setFecha_entrega(new Date());
                agregarHistorial(encomienda, "Entrega finalizada", encomienda.getCiudad_destino(), "Entregado", "El paquete fue entregado al cliente en su domicilio");
            } else if (encomienda.getTipo_entrega().equals("Recojo en tienda")) {
                // Recojo en tienda
                agregarHistorial(encomienda, "Disponibilidad", encomienda.getCiudad_destino(), "En tránsito", "El cliente puede recoger su paquete en la sucursal");
                encomienda.setEstado("Entregado");
                encomienda.setFecha_entrega(new Date());
                agregarHistorial(encomienda, "Entrega finalizada", encomienda.getCiudad_destino(), "Entregado", "El cliente recogió su encomienda");
            }
        }

        encomiendaRepository.save(encomienda);
    }

    public EncomiendaDTO retornarEncomiendaDTO(Encomienda encomienda){
        String fecha_envio = encomienda.getFecha_envio() == null ? "Encomienda pendiente" : dateFormat.format(encomienda.getFecha_envio());
        String fecha_entrega = "";
        if(fecha_envio.equals("Encomienda pendiente")){
            fecha_entrega = encomienda.getFecha_entrega() == null ? "Encomienda pendiente" : dateFormat.format(encomienda.getFecha_entrega());
        } else{
            fecha_entrega = encomienda.getFecha_entrega() == null ? "Encomienda en camino" : dateFormat.format(encomienda.getFecha_entrega());
        }
        List<HistorialEncomiendaDTO> historialEncomiendaDTOS = new ArrayList<>(List.of());
        for(HistorialEncomienda historialEncomienda : encomienda.getHistoriales()){
            HistorialEncomiendaDTO historialEncomiendaDTO = historialEncomiendaService.retornarHistorialEncomiendaDTO(historialEncomienda);
            historialEncomiendaDTOS.add(historialEncomiendaDTO);
        }
        return new EncomiendaDTO(encomienda.getIdEncomienda(), encomienda.getDescripcion(), encomienda.getCiudad_origen(), encomienda.getCiudad_destino(), encomienda.getDireccion_destino(), encomienda.getTipo_entrega(), encomienda.getCant_paquetes(), encomienda.getEstado(), encomienda.getCant_horas_viaje(),  encomienda.getClave_secreta(), encomienda.getUrl(), fecha_envio, fecha_entrega, encomienda.getNombre_emisor(), encomienda.getApellido_emisor(), encomienda.getDni_emisor(), encomienda.getTelefono_emisor(), encomienda.getNombre_receptor(), encomienda.getApellido_receptor(), encomienda.getDni_receptor(), encomienda.getTelefono_receptor(), encomienda.getRazon_social_emisor(), encomienda.getRuc_emisor(), encomienda.getTelefono_empresa_emisor(), encomienda.getRazon_social_receptor(), encomienda.getRuc_receptor(), encomienda.getTelefono_empresa_receptor(), historialEncomiendaDTOS);
    }

    //Registrar encomienda
    public Encomienda registrarEncomienda(EncomiendaDTO encomiendaDTO) {
        // Generar clave secreta aleatoria
        String claveSecreta = generarClaveSecreta();

        // Crear la entidad Encomienda
        Encomienda nuevaEncomienda = new Encomienda();
        nuevaEncomienda.setDescripcion(encomiendaDTO.getDescripcion());
        nuevaEncomienda.setCiudad_origen(encomiendaDTO.getCiudad_origen());
        nuevaEncomienda.setCiudad_destino(encomiendaDTO.getCiudad_destino());
        nuevaEncomienda.setDireccion_destino(encomiendaDTO.getDireccion_destino());
        nuevaEncomienda.setTipo_entrega(encomiendaDTO.getTipo_entrega());
        nuevaEncomienda.setCant_paquetes(encomiendaDTO.getCant_paquetes());
        nuevaEncomienda.setEstado("Pendiente");
        nuevaEncomienda.setCant_horas_viaje(encomiendaDTO.getCant_horas_viaje());
        nuevaEncomienda.setUrl(encomiendaDTO.getUrl());

        nuevaEncomienda.setFecha_envio(new Date());

        nuevaEncomienda.setNombre_emisor(encomiendaDTO.getNombre_emisor());
        nuevaEncomienda.setApellido_emisor(encomiendaDTO.getApellido_emisor());
        nuevaEncomienda.setDni_emisor(encomiendaDTO.getDni_emisor());
        nuevaEncomienda.setTelefono_emisor(encomiendaDTO.getTelefono_emisor());

        nuevaEncomienda.setNombre_receptor(encomiendaDTO.getNombre_receptor());
        nuevaEncomienda.setApellido_receptor(encomiendaDTO.getApellido_receptor());
        nuevaEncomienda.setDni_receptor(encomiendaDTO.getDni_receptor());
        nuevaEncomienda.setTelefono_receptor(encomiendaDTO.getTelefono_receptor());

        nuevaEncomienda.setRazon_social_emisor(encomiendaDTO.getRazon_social_emisor());
        nuevaEncomienda.setRuc_emisor(encomiendaDTO.getRuc_emisor());
        nuevaEncomienda.setTelefono_empresa_emisor(encomiendaDTO.getTelefono_empresa_emisor());

        nuevaEncomienda.setRazon_social_receptor(encomiendaDTO.getRazon_social_receptor());
        nuevaEncomienda.setRuc_receptor(encomiendaDTO.getRuc_receptor());
        nuevaEncomienda.setTelefono_empresa_receptor(encomiendaDTO.getTelefono_empresa_receptor());

        nuevaEncomienda.setClave_secreta(claveSecreta);

        // Guardar en la base de datos
        Encomienda encomiendaGuardada = encomiendaRepository.save(nuevaEncomienda);
        return encomiendaGuardada;
    }

    // Generar mensaje SMS con el formato solicitado
    public String generarMensaje(Encomienda encomienda, boolean esParaReceptor) {
        String nombreEmisor = encomienda.getNombre_emisor() + " " + encomienda.getApellido_emisor();
        String nombreReceptor = encomienda.getNombre_receptor() + " " + encomienda.getApellido_receptor();

        if (esParaReceptor) {
            // Mensaje para el receptor
            return String.format(
                    "Hemos recibido una encomienda de %s para usted, %s. El código de la encomienda es: %d. La clave para recoger su encomienda es: %s.",
                    nombreEmisor, nombreReceptor, encomienda.getIdEncomienda(), encomienda.getClave_secreta()
            );
        } else {
            // Mensaje para el emisor
            return String.format(
                    "Hola %s, su encomienda con el código %d se ha registrado exitosamente y está lista para ser enviada a %s.",
                    nombreEmisor, encomienda.getIdEncomienda(), encomienda.getCiudad_destino()
            );
        }
    }

    public void enviarMensajeSMS(Long telefonoEmisor, String mensajeEmisor, Long telefonoReceptor, String mensajeReceptor) {
        try {
            String apiUrl = "https://m38x92.api.infobip.com/sms/2/text/advanced";

            // Crear objeto JSON con Jackson
            ObjectMapper mapper = new ObjectMapper();
            ObjectNode rootNode = mapper.createObjectNode();
            ArrayNode messagesNode = rootNode.putArray("messages");

            // Mensaje para el emisor
            if (telefonoEmisor != null && mensajeEmisor != null && !mensajeEmisor.isEmpty()) {
                ObjectNode message1 = messagesNode.addObject();
                ArrayNode destinations1 = message1.putArray("destinations");
                destinations1.addObject().put("to", "+" + telefonoEmisor); // Número del emisor
                message1.put("from", INFOBIP_SENDER_NAME);                // Remitente
                message1.put("text", mensajeEmisor);                     // Mensaje para el emisor
            }

            // Mensaje para el receptor
            if (telefonoReceptor != null && mensajeReceptor != null && !mensajeReceptor.isEmpty()) {
                ObjectNode message2 = messagesNode.addObject();
                ArrayNode destinations2 = message2.putArray("destinations");
                destinations2.addObject().put("to", "+" + telefonoReceptor); // Número del receptor
                message2.put("from", INFOBIP_SENDER_NAME);                  // Remitente
                message2.put("text", mensajeReceptor);                     // Mensaje para el receptor
            }

            // Validar si se generaron mensajes
            if (messagesNode.isEmpty()) {
                logger.warn("No se generaron mensajes SMS porque faltaban datos.");
                return;
            }

            // Convertir a JSON string
            String requestPayload = mapper.writeValueAsString(rootNode);

            // Configurar cliente HTTP
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(apiUrl))
                    .header("Authorization", "App " + INFOBIP_API_KEY)
                    .header("Content-Type", "application/json")
                    .header("Accept", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(requestPayload))
                    .build();

            // Enviar solicitud
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            logger.info("Respuesta de Infobip: " + response.body());
        } catch (Exception e) {
            logger.error("Error al enviar mensajes SMS con Infobip: ", e);
        }
    }

    // Generar clave secreta con caracteres especiales
    private String generarClaveSecreta() {
        String caracteres = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789!&@#$%";
        Random random = new Random();
        StringBuilder clave = new StringBuilder();
        for (int i = 0; i < 6; i++) {
            clave.append(caracteres.charAt(random.nextInt(caracteres.length())));
        }
        return clave.toString();
    }

    // Método para validar la clave secreta
    public boolean validarClaveSecreta(Integer idEncomienda, String claveIngresada) {
        Encomienda encomienda = encomiendaRepository.findById(idEncomienda)
                .orElseThrow(() -> new RuntimeException("Encomienda no encontrada con ID: " + idEncomienda));
        return encomienda.getClave_secreta().equals(claveIngresada);
    }
}
