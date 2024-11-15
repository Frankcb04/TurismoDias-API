package upao.edu.pe.TurismoDiasAPI.DTO;


import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class HistorialEncomiendaDTO {

    private Integer id_historial_encomienda;

    private String fecha_evento;

    private String tipo_evento; // ENUM -> Recepción - Despacho - En tránsito y Entrega
    private String lugar_actual;
    private String estado_actual; // ENUM -> En tránsito - Entregado
    private String descripcion_evento;

}
