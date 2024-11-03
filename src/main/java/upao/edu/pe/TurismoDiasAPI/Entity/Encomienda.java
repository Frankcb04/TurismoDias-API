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
@Table (name= "Encomienda")
public class Encomienda {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer idEncomienda;

    private String descripcion;
    private String ciudadOrigen;
    private String ciudadDestino;
    private String direccionDestino;
    private String tipoEntrega;
    private Integer cantPaquetes;
    private String estado;

    private Date fechaEnvio;
    private Date fechaEntrega;

    private String nombreEmisor;
    private String apellidoEmisor;
    private String dniEmisor;
    private String nombreReceptor;
    private String apellidoReceptor;
    private String dniReceptor;
    private String razonSocialEmisor;
    private String rucEmisor;
    private String razonSocialReceptor;
    private String rucReceptor;
}
