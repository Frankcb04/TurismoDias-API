package upao.edu.pe.TurismoDiasAPI.Entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;
import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table (name= "encomienda")
public class Encomienda {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id_encomienda;

    private String descripcion;
    private String ciudad_origen;
    private String ciudad_destino;
    private String direccion_destino;
    private String tipo_entrega; // ENUM -> delivery o recojo en tienda
    private Integer cant_paquetes;
    private String estado; // ENUM -> pendiente, en tránsito y entregado

    private Date fecha_envio;

    private Date fecha_entrega;

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

    @OneToMany(mappedBy = "encomienda", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonManagedReference
    private List<HistorialEncomienda> historiales;
}
