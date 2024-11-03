package upao.edu.pe.TurismoDiasAPI.Service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import upao.edu.pe.TurismoDiasAPI.Entity.Encomienda;
import upao.edu.pe.TurismoDiasAPI.Repository.EncomiendaRepository;

import java.util.NoSuchElementException;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class EncomiendaService {

    private final EncomiendaRepository encomiendaRepository;

    //Listar las encomiendas por ID
    public Optional<Encomienda> obtenerEncomiendaPorId(Long idEncomienda) {
        return encomiendaRepository.findById(idEncomienda);
    }
}
