package upao.edu.pe.TurismoDiasAPI.Repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import upao.edu.pe.TurismoDiasAPI.Entity.Encomienda;

import java.util.List;
import java.util.Optional;

@Repository
public interface EncomiendaRepository extends JpaRepository<Encomienda, Integer> {
    Optional<Encomienda> findById(Integer id_encomienda);
    List<Encomienda> findByEstadoIn(List<String> estados);
}
