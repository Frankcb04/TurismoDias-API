package upao.edu.pe.TurismoDiasAPI.Entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name= "Paquete")
public class Paquete {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer idPaquete;
    private Double peso;
    private String dimensiones;

    @ManyToOne
    @JoinColumn(name = "idEncomienda", nullable = false)
    private Encomienda encomienda;
}
