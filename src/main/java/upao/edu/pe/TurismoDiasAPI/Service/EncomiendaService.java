package upao.edu.pe.TurismoDiasAPI.Service;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

@Service
@RequiredArgsConstructor
public class EncomiendaService {

    private final EncomiendaRepository encomiendaRepository;
    private final HistorialEncomiendaRepository historialEncomiendaRepository;
    private final HistorialEncomiendaService historialEncomiendaService;
    SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy");
    private static final Logger logger = LoggerFactory.getLogger(EncomiendaService.class);

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
        return new EncomiendaDTO(encomienda.getIdEncomienda(), encomienda.getDescripcion(), encomienda.getCiudad_origen(), encomienda.getCiudad_destino(), encomienda.getDireccion_destino(), encomienda.getTipo_entrega(), encomienda.getCant_paquetes(), encomienda.getEstado(), encomienda.getCant_horas_viaje(), fecha_envio, fecha_entrega, encomienda.getNombre_emisor(), encomienda.getApellido_emisor(), encomienda.getDni_emisor(), encomienda.getNombre_receptor(), encomienda.getApellido_receptor(), encomienda.getDni_receptor(), encomienda.getRazon_social_emisor(), encomienda.getRuc_emisor(), encomienda.getRazon_social_receptor(), encomienda.getRuc_receptor(), encomienda.getUrl(), historialEncomiendaDTOS);
    }

}

