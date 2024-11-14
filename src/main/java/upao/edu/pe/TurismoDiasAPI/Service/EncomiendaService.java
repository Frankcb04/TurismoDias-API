package upao.edu.pe.TurismoDiasAPI.Service;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import upao.edu.pe.TurismoDiasAPI.Entity.Encomienda;
import upao.edu.pe.TurismoDiasAPI.Entity.HistorialEncomienda;
import upao.edu.pe.TurismoDiasAPI.Repository.EncomiendaRepository;
import upao.edu.pe.TurismoDiasAPI.Repository.HistorialEncomiendaRepository;

import java.util.*;

@Service
@RequiredArgsConstructor
public class EncomiendaService {

    private final EncomiendaRepository encomiendaRepository;
    private final HistorialEncomiendaRepository historialEncomiendaRepository;

    // Listar las encomiendas por ID
    public Optional<Encomienda> obtenerEncomiendaPorId(Integer id_encomienda) {
        return encomiendaRepository.findById(id_encomienda);
    }

    private static final Map<String, Integer> tiempoViaje = new HashMap<>();

    static {
        tiempoViaje.put("Trujillo-Chiclayo", 4);
        tiempoViaje.put("Lima-Chimbote", 7);
        tiempoViaje.put("Chiclayo-Piura", 3);
        tiempoViaje.put("Piura-Jaén", 7);
        tiempoViaje.put("Jaén-Pacasmayo", 9);
        tiempoViaje.put("Pacasmayo-Nueva Cajamarca", 14);
        tiempoViaje.put("Cajamarca-Cajabamba", 3);
        tiempoViaje.put("Cajabamba-Piura", 12);
        tiempoViaje.put("Piura-Lima", 16);
        tiempoViaje.put("Lima-Trujillo", 9);
        tiempoViaje.put("Trujillo-Chepén", 3);
        tiempoViaje.put("Cajamarca-Chimbote", 8);
        tiempoViaje.put("Cajabamba-Moyobamba", 15);
        tiempoViaje.put("Chiclayo-Chilete", 4);
        tiempoViaje.put("Lima-Nueva Cajamarca", 23);
        tiempoViaje.put("Nueva Cajamarca-San Marcos", 13);
        tiempoViaje.put("Tarapoto-Moyobamba", 2);
        tiempoViaje.put("Cajamarca-Tarapoto", 16);
        tiempoViaje.put("Chilete-Chiclayo", 4);
        tiempoViaje.put("Chimbote-Chiclayo", 6);
        tiempoViaje.put("San Marcos-Cajabamba", 2);
    }

    @Scheduled(fixedRate = 600000) // Cada 10 minutos
    public void actualizarEstadosAutomatica() {
        List<Encomienda> encomiendasPendientes = encomiendaRepository.findByEstadoIn(List.of("Pendiente", "En tránsito"));
        for (Encomienda encomienda : encomiendasPendientes) {
            actualizarEstadoEncomienda(encomienda);
        }
    }

    @PostConstruct
    public void inicializarVerificacionAutomatica() {
        actualizarEstadosAutomatica();
    }

    public void actualizarEstadoEncomienda(Encomienda encomienda) {
        HistorialEncomienda ultimoHistorial = obtenerUltimoHistorial(encomienda);

        if (encomienda.getEstado().equals("Pendiente") && ultimoHistorial == null) {
            agregarHistorial(encomienda, "Aceptación", encomienda.getCiudad_origen(), "Pendiente", "Se aceptó el paquete");
            encomienda.setEstado("En tránsito");
            encomienda.setFecha_envio(new Date());
            agregarHistorial(encomienda, "Envío", encomienda.getCiudad_origen(), "En tránsito", "El paquete salió de " + encomienda.getCiudad_origen());
        }

        if (ultimoHistorial != null) {
            String descripcionEvento = ultimoHistorial.getDescripcion_evento();
            String estadoActual = encomienda.getEstado();

            if (estadoActual.equals("En tránsito") && descripcionEvento.contains("Salió de")) {
                long horasTranscurridas = calcularHorasDesdeUltimoEvento(ultimoHistorial.getFecha_evento());
                String ruta = encomienda.getCiudad_origen() + "-" + encomienda.getCiudad_destino();
                Integer horasViaje = tiempoViaje.get(ruta); // encomienda.getHoraRuta();

                if (horasViaje != null && horasTranscurridas >= horasViaje) {
                    agregarHistorial(encomienda, "Recepción", encomienda.getCiudad_destino(), "En tránsito", "Llegó a la sucursal de " + encomienda.getCiudad_destino());
                }
            } else if (estadoActual.equals("En tránsito") && descripcionEvento.contains("Llegó a la sucursal")) {
                if (encomienda.getTipo_entrega().equals("Domicilio")) {
                    agregarHistorial(encomienda, "Entrega", encomienda.getCiudad_destino(), "Entregado", "Se entregó la encomienda en la dirección registrada");
                    encomienda.setEstado("Entregado");
                    encomienda.setFecha_entrega(new Date());
                } else if (encomienda.getTipo_entrega().equals("Recojo en tienda")) {
                    agregarHistorial(encomienda, "Entrega", encomienda.getCiudad_destino(), "Entregado", "El cliente recogió su encomienda");
                    encomienda.setEstado("Entregado");
                    encomienda.setFecha_entrega(new Date());
                }
            }
        }
        encomiendaRepository.save(encomienda);
    }

    private HistorialEncomienda obtenerUltimoHistorial(Encomienda encomienda) {
        List<HistorialEncomienda> historiales = historialEncomiendaRepository.findByEncomiendaOrderByFechaEventoDesc(encomienda);
        return historiales.isEmpty() ? null : historiales.getFirst(); // Obtiene el más reciente o devuelve null si no hay registros
    }

    private void agregarHistorial(Encomienda encomienda, String tipo_evento, String lugar_actual, String estado_actual, String descripcion_evento) {
        HistorialEncomienda historial = new HistorialEncomienda();
        historial.setFecha_evento(new Date());
        historial.setTipo_evento(tipo_evento);
        historial.setLugar_actual(lugar_actual);
        historial.setEstado_actual(estado_actual);
        historial.setDescripcion_evento(descripcion_evento);
        historial.setEncomienda(encomienda);
        historialEncomiendaRepository.save(historial);
    }

    private long calcularHorasDesdeUltimoEvento(Date fecha_evento) {
        long diferenciaMilisegundos = new Date().getTime() - fecha_evento.getTime();
        return diferenciaMilisegundos / (1000 * 60 * 60); // Convierte de ms a horas
    }
}

