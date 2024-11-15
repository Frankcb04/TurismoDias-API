package upao.edu.pe.TurismoDiasAPI.Entity;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name= "historial_encomienda")
public class HistorialEncomienda {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_historial_encomienda")
    private Integer idHistorialEncomienda;

    @Column(name = "fecha_evento")
    private Date fecha_evento;

    private String tipo_evento; // ENUM -> Recepción - Despacho - En tránsito y Entrega
    private String lugar_actual;
    private String estado_actual; // ENUM -> En tránsito - Entregado
    private String descripcion_evento;

    @ManyToOne
    @JoinColumn(name = "id_encomienda", nullable = false)
    @JsonBackReference
    private Encomienda encomienda;
}

