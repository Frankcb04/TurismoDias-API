package upao.edu.pe.TurismoDiasAPI.Controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import upao.edu.pe.TurismoDiasAPI.Entity.Encomienda;
import upao.edu.pe.TurismoDiasAPI.Service.EncomiendaService;

import java.util.List;
import java.util.Optional;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/encomienda")
public class EncomiendaController {

    private final EncomiendaService encomiendaService;

    // Método para listar la encomienda
    @GetMapping("/listarEncomienda/{id_encomienda}")
    public Optional<Encomienda> obtenerEncomiendaPorId(@PathVariable("id_encomienda") Integer id_encomienda) {
        return encomiendaService.obtenerEncomiendaPorId(id_encomienda);
    }

    // Actualizar todos los estados manualmente
    @PostMapping("/actualizar-estados")
    public ResponseEntity<Void> actualizarEstadosManual() {
        encomiendaService.actualizarEstadosAutomatica();
        return ResponseEntity.ok().build();
    }
}