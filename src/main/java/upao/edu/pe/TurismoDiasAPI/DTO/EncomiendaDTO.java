package upao.edu.pe.TurismoDiasAPI.DTO;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class EncomiendaDTO {
    private Integer id_encomienda;

    private String descripcion;
    private String ciudad_origen;
    private String ciudad_destino;
    private String direccion_destino;
    private String tipo_entrega; // ENUM -> delivery o recojo en tienda
    private Integer cant_paquetes;
    private String estado; // ENUM -> pendiente, en tránsito y entregado
    private Integer cant_horas_viaje;
    private String fecha_envio;

    private String fecha_entrega;

    private String nombre_emisor;
    private String apellido_emisor;
    private Integer dni_emisor;
    private String nombre_receptor;
    private String apellido_receptor;
    private Integer dni_receptor;
    private String razon_social_emisor;
    private Long ruc_emisor;
    private String razon_social_receptor;
    private Long ruc_receptor;

    private List<HistorialEncomiendaDTO> historiales;
}
