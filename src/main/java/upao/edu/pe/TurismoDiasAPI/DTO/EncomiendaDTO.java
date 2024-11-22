package upao.edu.pe.TurismoDiasAPI.DTO;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class EncomiendaDTO {
    private Integer id_encomienda;

    //Datos de la encomienda
    private String descripcion;
    private String ciudad_origen;
    private String ciudad_destino;
    private String direccion_destino;
    private String tipo_entrega; // ENUM -> delivery o recojo en tienda
    private Integer cant_paquetes;
    private String estado; // ENUM -> pendiente, en tránsito y entregado
    private Integer cant_horas_viaje;
    private String clave_secreta;
    private String url;

    //Fechas de la encomienda
    private String fecha_envio;
    private String fecha_entrega;

    //Datos del emisor - cliente natural
    private String nombre_emisor;
    private String apellido_emisor;
    private Integer dni_emisor;
    private Integer telefono_emisor;

    //Datos del receptor - cliente natural
    private String nombre_receptor;
    private String apellido_receptor;
    private Integer dni_receptor;
    private Integer telefono_receptor;

    //Datos del emisor - cliente jurídico
    private String razon_social_emisor;
    private Long ruc_emisor;
    private Integer telefono_empresa_emisor;

    //Datos del receptor - cliente jurídico
    private String razon_social_receptor;
    private Long ruc_receptor;
    private Integer telefono_empresa_receptor;

    private List<HistorialEncomiendaDTO> historiales;
}
