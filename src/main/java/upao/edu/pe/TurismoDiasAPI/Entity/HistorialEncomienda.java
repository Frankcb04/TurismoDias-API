package upao.edu.pe.TurismoDiasAPI.Entity;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonFormat;
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
@Table(name= "historial_encomienda")
public class HistorialEncomienda {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id_historial_encomienda;

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

