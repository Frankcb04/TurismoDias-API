package upao.edu.pe.TurismoDiasAPI.Repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import upao.edu.pe.TurismoDiasAPI.Entity.HistorialEncomienda;

import java.util.List;

@Repository
public interface HistorialEncomiendaRepository extends JpaRepository<HistorialEncomienda, Long> {
    List<HistorialEncomienda> findByIdEncomienda(Long idEncomienda);
}
