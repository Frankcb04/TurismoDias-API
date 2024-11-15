package upao.edu.pe.TurismoDiasAPI.Repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import upao.edu.pe.TurismoDiasAPI.Entity.Encomienda;
import upao.edu.pe.TurismoDiasAPI.Entity.HistorialEncomienda;

import java.util.List;

@Repository
public interface HistorialEncomiendaRepository extends JpaRepository<HistorialEncomienda, Integer> {

    List<HistorialEncomienda> findByEncomiendaIdEncomiendaOrderByIdHistorialEncomiendaDesc(Integer id_encomienda);
}
