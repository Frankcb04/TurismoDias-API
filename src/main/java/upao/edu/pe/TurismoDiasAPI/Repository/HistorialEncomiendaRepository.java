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

    @Query("SELECT h FROM HistorialEncomienda h WHERE h.encomienda.id_encomienda = :id_encomienda ORDER BY h.fecha_evento DESC")
    List<HistorialEncomienda> findByEncomiendaIdEncomiendaOrderByFechaEventoDesc(@Param("id_encomienda") Integer id_encomienda);

    @Query("SELECT h FROM HistorialEncomienda h WHERE h.encomienda = :encomienda ORDER BY h.fecha_evento DESC")
    List<HistorialEncomienda> findByEncomiendaOrderByFechaEventoDesc(@Param("encomienda") Encomienda encomienda);
}
