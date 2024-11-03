package upao.edu.pe.TurismoDiasAPI.Entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name= "HistorialEncomienda")
public class HistorialEncomienda {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer idHistorialEncomienda;

    private Date fechaEvento;

    private String tipoEvento;
    private String lugarActual;
    private String estadoActual;
    private String descripcionEvento;

    @ManyToOne
    @JoinColumn(name = "idEncomienda", nullable = false)
    private Encomienda encomienda;
}

