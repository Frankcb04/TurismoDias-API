package upao.edu.pe.TurismoDiasAPI.Service;

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

import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.TimeUnit;

import com.twilio.Twilio;
import com.twilio.rest.api.v2010.account.Message;
import com.twilio.type.PhoneNumber;

@Service
@RequiredArgsConstructor
public class EncomiendaService {

    private final EncomiendaRepository encomiendaRepository;
    private final HistorialEncomiendaRepository historialEncomiendaRepository;
    private final HistorialEncomiendaService historialEncomiendaService;
    SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy");
    private static final Logger logger = LoggerFactory.getLogger(EncomiendaService.class);

    @Value("${twilio.account.sid}")
    private String ACCOUNT_SID;

    @Value("${twilio.auth.token}")
    private String AUTH_TOKEN;

    @Value("${twilio.phone.number}")
    private String TWILIO_PHONE_NUMBER;

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
        for(HistorialEncomiendaDTO historial : ultimoHistorial){
            if(historial.getDescripcion_evento().equals("El paquete ha llegado al almacén de la sucursal.")){
                validacion = true;
                break;
            }
        }
        if(!validacion){
            agregarHistorial(encomienda, "Almacén de sucursal", encomienda.getCiudad_destino(), "En tránsito",
                    "El paquete ha llegado al almacén de la sucursal.");
        }
        validacion = false;
        for(HistorialEncomiendaDTO historial : ultimoHistorial){
            if(historial.getDescripcion_evento().equals("El paquete ha llegado al almacén de la sucursal.")){
                validacion = true;
                break;
            }
        }
        if(validacion){
            if (encomienda.getTipo_entrega().equals("Delivery")) {
                prepararEntregaADomicilio(encomienda);
            } else if (encomienda.getTipo_entrega().equals("Recojo en tienda")) {
                prepararEntregaRecojoEnTienda(encomienda);
            }
        }
    }

    // Entrega a domicilio
    private void prepararEntregaADomicilio(Encomienda encomienda) {
        List<HistorialEncomiendaDTO> ultimoHistorial = historialEncomiendaService.listarHistorialPorEncomiendaId(encomienda.getIdEncomienda());
        boolean validacion = false;
        for(HistorialEncomiendaDTO historial : ultimoHistorial){
            if(historial.getDescripcion_evento().equals("El encargado está llevando el paquete a la dirección registrada.")){
                validacion = true;
                break;
            }
        }
        if(!validacion){
            agregarHistorial(encomienda, "Camino a entrega a domicilio", encomienda.getCiudad_destino(), "En tránsito",
                    "El encargado está llevando el paquete a la dirección registrada.");
        }
        validacion = false;
        for(HistorialEncomiendaDTO historial : ultimoHistorial){
            if(historial.getDescripcion_evento().equals("El encargado está llevando el paquete a la dirección registrada.")){
                validacion = true;
                break;
            }
        }
        if(validacion){
            finalizarEntrega(encomienda, "Se entregó el paquete al cliente.");
        }
    }

    // Entrega en tienda
    private void prepararEntregaRecojoEnTienda(Encomienda encomienda) {
        List<HistorialEncomiendaDTO> ultimoHistorial = historialEncomiendaService.listarHistorialPorEncomiendaId(encomienda.getIdEncomienda());
        boolean validacion = false;
        for(HistorialEncomiendaDTO historial : ultimoHistorial){
            if(historial.getDescripcion_evento().equals("El paquete está listo para que el cliente lo recoja en la tienda.")){
                validacion = true;
                break;
            }
        }
        if(!validacion){
            agregarHistorial(encomienda, "Listo para recojo en tienda", encomienda.getCiudad_destino(), "En tránsito",
                    "El paquete está listo para que el cliente lo recoja en la tienda.");
        }
        validacion = false;
        for(HistorialEncomiendaDTO historial : ultimoHistorial){
            if(historial.getDescripcion_evento().equals("El paquete está listo para que el cliente lo recoja en la tienda.")){
                validacion = true;
                break;
            }
        }
        if(validacion){
            finalizarEntrega(encomienda, "El cliente recogió el paquete en la tienda.");
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

        // Determinar los números de teléfono para enviar mensajes
        enviarMensajesSegunTipoCliente(encomiendaGuardada);

        return encomiendaGuardada;
    }

    // Método para determinar y enviar mensajes según el tipo de cliente
    private void enviarMensajesSegunTipoCliente(Encomienda encomienda) {
        if (esClienteJuridico(encomienda)) {
            // Clientes jurídicos
            enviarMensajeSMS(encomienda.getTelefono_empresa_emisor(), generarMensaje(encomienda));
            enviarMensajeSMS(encomienda.getTelefono_empresa_receptor(), generarMensaje(encomienda));
        } else {
            // Clientes naturales
            enviarMensajeSMS(encomienda.getTelefono_emisor(), generarMensaje(encomienda));
            enviarMensajeSMS(encomienda.getTelefono_receptor(), generarMensaje(encomienda));
        }
    }

    // Verificar si los clientes son jurídicos
    private boolean esClienteJuridico(Encomienda encomienda) {
        return encomienda.getRazon_social_emisor() != null && !encomienda.getRazon_social_emisor().isEmpty()
                && encomienda.getRazon_social_receptor() != null && !encomienda.getRazon_social_receptor().isEmpty();
    }

    // Generar mensaje SMS con el formato solicitado
    private String generarMensaje(Encomienda encomienda) {
        String nombreEmisor = esClienteJuridico(encomienda)
                ? encomienda.getRazon_social_emisor()
                : encomienda.getNombre_emisor() + " " + encomienda.getApellido_emisor();

        String nombreReceptor = esClienteJuridico(encomienda)
                ? encomienda.getRazon_social_receptor()
                : encomienda.getNombre_receptor() + " " + encomienda.getApellido_receptor();

        return String.format(
                "Hemos recibido una encomienda de %s para %s.\nEl código de la encomienda es: %d\nLa clave para recoger es: %s",
                nombreEmisor, nombreReceptor, encomienda.getIdEncomienda(), encomienda.getClave_secreta()
        );
    }

    // Enviar mensaje SMS
    private void enviarMensajeSMS(Integer numeroTelefono, String mensaje) {
        
            Twilio.init(ACCOUNT_SID, AUTH_TOKEN);
        try {
            Message.creator(
                    new PhoneNumber("+51" + numeroTelefono), // Número destino
                    new PhoneNumber(TWILIO_PHONE_NUMBER),   // Número de Twilio
                    mensaje                                 // Contenido del mensaje
            ).create();
            System.out.println("SMS enviado correctamente a: +51" + numeroTelefono);
        } catch (Exception e) {
            System.err.println("Error al enviar SMS a +51" + numeroTelefono + ": " + e.getMessage());
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

    // Validar clave secreta
    public boolean validarClaveSecreta(Integer idEncomienda, String claveIngresada) {
        Encomienda encomienda = encomiendaRepository.findById(idEncomienda)
                .orElseThrow(() -> new RuntimeException("Encomienda no encontrada"));

        if (encomienda.getClave_secreta().equals(claveIngresada)) {
            // Finalizar entrega
            if (encomienda.getTipo_entrega().equals("Delivery")) {
                finalizarEntrega(encomienda, "El cliente validó la clave y recibió el paquete en la dirección asignada.");
            } else if (encomienda.getTipo_entrega().equals("Recojo en tienda")) {
                finalizarEntrega(encomienda, "El cliente validó la clave y recogió el paquete en la tienda.");
            }
            return true;
        } else {
            throw new RuntimeException("Clave secreta incorrecta.");
        }
    }
}
