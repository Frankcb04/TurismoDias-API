package upao.edu.pe.TurismoDiasAPI.Service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import upao.edu.pe.TurismoDiasAPI.DTO.HistorialEncomiendaDTO;
import upao.edu.pe.TurismoDiasAPI.Entity.HistorialEncomienda;
import upao.edu.pe.TurismoDiasAPI.Repository.HistorialEncomiendaRepository;

import java.text.SimpleDateFormat;
import java.util.List;

@Service
@RequiredArgsConstructor
public class HistorialEncomiendaService {

    private final HistorialEncomiendaRepository historialEncomiendaRepository;
    SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy");

    // Listar el historial de la encomienda por ID
    public List<HistorialEncomiendaDTO> listarHistorialPorEncomiendaId(Integer id_encomienda) {
        return historialEncomiendaRepository.findByEncomiendaIdEncomiendaOrderByFechaEventoDesc(id_encomienda).stream().map(this::retornarHistorialEncomiendaDTO).toList();
    }

    protected HistorialEncomiendaDTO retornarHistorialEncomiendaDTO(HistorialEncomienda historialEncomienda) {
        String fecha_evento = dateFormat.format(historialEncomienda.getFecha_evento());
        return new HistorialEncomiendaDTO(historialEncomienda.getId_historial_encomienda(), fecha_evento, historialEncomienda.getTipo_evento(), historialEncomienda.getLugar_actual(), historialEncomienda.getEstado_actual(), historialEncomienda.getDescripcion_evento());
    }
}
