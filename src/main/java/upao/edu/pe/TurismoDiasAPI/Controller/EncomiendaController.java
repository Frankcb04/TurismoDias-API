package upao.edu.pe.TurismoDiasAPI.Controller;

import lombok.AllArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import upao.edu.pe.TurismoDiasAPI.DTO.EncomiendaDTO;
import upao.edu.pe.TurismoDiasAPI.Entity.Encomienda;
import upao.edu.pe.TurismoDiasAPI.Repository.EncomiendaRepository;
import upao.edu.pe.TurismoDiasAPI.Service.EncomiendaService;

import java.util.Optional;

@RestController
@RequestMapping("/api/encomienda")
@AllArgsConstructor
public class EncomiendaController {

    EncomiendaService encomiendaService;
    private EncomiendaRepository encomiendaRepository;

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

    // Endpoint para omitir el tiempo del viaje y actualizar el estado
    @PostMapping("/{id}/omitir-viaje")
    public ResponseEntity<String> omitirViaje(@PathVariable Integer id) {
        Optional<Encomienda> encomiendaOpt = encomiendaService.obtenerEncomiendaPorId(id);
        if (encomiendaOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Encomienda no encontrada");
        }

        Encomienda encomienda = encomiendaOpt.get();
        encomiendaService.omitirViajeYActualizarEstado(encomienda);
        return ResponseEntity.ok("El viaje de la encomienda fue omitido y el estado actualizado");
    }

    //Registrar encomienda
    @PostMapping("/registrar")
    public ResponseEntity<Encomienda> registrarEncomienda(@RequestBody EncomiendaDTO encomiendaDTO) {
        try {
            Encomienda nuevaEncomienda = encomiendaService.registrarEncomienda(encomiendaDTO);
            return ResponseEntity.status(HttpStatus.CREATED).body(nuevaEncomienda);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(null);
        }
    }

    //Validar clave secreta
    @PostMapping("/validar-clave/{idEncomienda}")
    public ResponseEntity<String> validarClaveSecreta(
            @PathVariable Integer idEncomienda,
            @RequestParam String claveIngresada) {
        try {
            boolean esValida = encomiendaService.validarClaveSecreta(idEncomienda, claveIngresada);
            if (esValida) {
                return ResponseEntity.ok("Clave secreta validada correctamente. El paquete ha sido entregado.");
            } else {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body("La clave secreta no coincide.");
            }
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(e.getMessage());
        }
    }
}