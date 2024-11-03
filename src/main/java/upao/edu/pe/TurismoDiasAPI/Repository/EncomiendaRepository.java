package upao.edu.pe.TurismoDiasAPI.Repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import upao.edu.pe.TurismoDiasAPI.Entity.Encomienda;

import java.util.List;

@Repository
public interface EncomiendaRepository extends JpaRepository<Encomienda, Long> {
}