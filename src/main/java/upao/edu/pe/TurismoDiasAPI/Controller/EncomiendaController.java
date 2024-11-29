package upao.edu.pe.TurismoDiasAPI.Controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import lombok.AllArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import upao.edu.pe.TurismoDiasAPI.DTO.EncomiendaDTO;
import upao.edu.pe.TurismoDiasAPI.Entity.Encomienda;
import upao.edu.pe.TurismoDiasAPI.Service.EncomiendaService;

import java.util.Optional;

@RestController
@RequestMapping("/api/encomienda")
@AllArgsConstructor
public class EncomiendaController {

    @Autowired
    private final EncomiendaService encomiendaService;
    private static final Logger logger = LoggerFactory.getLogger(EncomiendaController.class);

    // Método para listar la encomienda
    @GetMapping("/listarEncomienda/{id_encomienda}")
    public ResponseEntity<EncomiendaDTO> obtenerEncomiendaPorId(@PathVariable("id_encomienda") Integer id_encomienda) {
        // Intentamos obtener la encomienda
        Optional<Encomienda> encomiendaOpt = encomiendaService.obtenerEncomiendaPorId(id_encomienda);

        // Verificamos si no se encuentra
        if (encomiendaOpt.isEmpty()) {
            // Lanzamos RuntimeException con mensaje
            throw new RuntimeException("No se encontró la encomienda con ID: " + id_encomienda);
        }

        // Si la encontramos, convertimos a DTO y la retornamos
        EncomiendaDTO encomiendaDTO = encomiendaService.retornarEncomiendaDTO(encomiendaOpt.get());
        return ResponseEntity.ok(encomiendaDTO);
    }

    // Actualizar todos los estados manualmente
    @PostMapping("/actualizar-estados")
    public ResponseEntity<Void> actualizarEstados() {
        encomiendaService.actualizarEstadosEncomiendas();
        return ResponseEntity.ok().build();
    }

    // Endpoint para omitir el tiempo del viaje
    @PostMapping("/{id}/omitir-viaje")
    public ResponseEntity<String> omitirViaje(@PathVariable Integer id) {
        Optional<Encomienda> encomiendaOpt = encomiendaService.obtenerEncomiendaPorId(id);
        if (encomiendaOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Encomienda no encontrada.");
        }

        Encomienda encomienda = encomiendaOpt.get();
        encomiendaService.omitirViaje(encomienda);
        return ResponseEntity.ok("El viaje de la encomienda fue omitido");
    }

    //Registrar encomienda
    @PostMapping("/registrar")
    public ResponseEntity<?> registrarEncomienda(@RequestBody EncomiendaDTO encomiendaDTO) {
        try {
            // Registrar la encomienda
            Encomienda nuevaEncomienda = encomiendaService.registrarEncomienda(encomiendaDTO);

            // Generar los mensajes
            String mensajeEmisor = encomiendaService.generarMensaje(nuevaEncomienda, false);
            String mensajeReceptor = encomiendaService.generarMensaje(nuevaEncomienda, true);

            // Enviar SMS
            encomiendaService.enviarMensajeSMS(
                    encomiendaDTO.getTelefono_emisor(), mensajeEmisor,
                    encomiendaDTO.getTelefono_receptor(), mensajeReceptor
            );

            // Retornar la respuesta exitosa con la encomienda creada
            return ResponseEntity.status(HttpStatus.CREATED).body(nuevaEncomienda);
        } catch (IllegalArgumentException e) {
            logger.error("Datos inválidos al registrar la encomienda: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Error: Datos invalidos para registrar la encomienda.");
        } catch (Exception e) {
            logger.error("Error al registrar encomienda: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error: No se pudo registrar la encomienda.");
        }
    }


    // Validar clave secreta y finalizar entrega
    @PostMapping("/validar-clave/{idEncomienda}")
    public ResponseEntity<String> validarClaveSecreta(
            @PathVariable Integer idEncomienda,
            @RequestParam String claveIngresada) {
        try {
            // Obtener la encomienda desde el repositorio
            Encomienda encomienda = encomiendaService.obtenerEncomiendaPorId(idEncomienda)
                    .orElseThrow(() -> new RuntimeException("La encomienda no existe."));

            // Determinar el tipo de entrega
            if ("Delivery".equalsIgnoreCase(encomienda.getTipo_entrega())) {
                encomiendaService.prepararEntregaADomicilio(encomienda, claveIngresada);
            } else if ("Recojo en tienda".equalsIgnoreCase(encomienda.getTipo_entrega())) {
                encomiendaService.prepararEntregaRecojoEnTienda(encomienda, claveIngresada);
            } else {
                throw new IllegalArgumentException("Tipo de entrega desconocido.");
            }

            return ResponseEntity.ok("Clave secreta validada correctamente. La encomienda ha sido entregada.");
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(e.getMessage());
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(e.getMessage());
        }
    }
}