package upao.edu.pe.TurismoDiasAPI.Service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import upao.edu.pe.TurismoDiasAPI.Entity.HistorialEncomienda;
import upao.edu.pe.TurismoDiasAPI.Repository.HistorialEncomiendaRepository;

import java.util.List;

@Service
@RequiredArgsConstructor
public class HistorialEncomiendaService {

    private final HistorialEncomiendaRepository historialEncomiendaRepository;

    // Listar el historial de la encomienda por ID
    public List<HistorialEncomienda> listarHistorialPorEncomiendaId(Integer id_encomienda) {
        return historialEncomiendaRepository.findByEncomiendaIdEncomiendaOrderByFechaEventoDesc(id_encomienda);
    }
}
