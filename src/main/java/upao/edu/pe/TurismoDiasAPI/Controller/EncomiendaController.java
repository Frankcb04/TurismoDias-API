package upao.edu.pe.TurismoDiasAPI.Controller;

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
public class EncomiendaController {

    @Autowired
    EncomiendaService encomiendaService;

    // Método para listar la encomienda
    @GetMapping("/listarEncomienda/{id_encomienda}")
    public ResponseEntity<EncomiendaDTO> obtenerEncomiendaPorId(@PathVariable("id_encomienda") Integer id_encomienda) {
        Optional<Encomienda> encomiendaOpt = encomiendaService.obtenerEncomiendaPorId(id_encomienda);
        return encomiendaOpt.map(encomienda -> ResponseEntity.ok(encomiendaService.retornarEncomiendaDTO(encomienda))).orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND).body(null));
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
}