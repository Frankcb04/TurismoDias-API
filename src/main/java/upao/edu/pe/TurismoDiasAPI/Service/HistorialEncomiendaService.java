package upao.edu.pe.TurismoDiasAPI.Service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import upao.edu.pe.TurismoDiasAPI.Entity.HistorialEncomienda;
import upao.edu.pe.TurismoDiasAPI.Repository.HistorialEncomiendaRepository;

import java.util.List;

@Service
public class HistorialEncomiendaService {

    @Autowired
    HistorialEncomiendaRepository historialEncomiendaRepository;

    // Listar el historial de la encomienda por ID
    public List<HistorialEncomienda> listarHistorialPorEncomiendaId(Integer id_encomienda) {
        return historialEncomiendaRepository.findByEncomiendaIdEncomiendaOrderByFechaEventoDesc(id_encomienda);
    }
}
