package upao.edu.pe.TurismoDiasAPI.Controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import upao.edu.pe.TurismoDiasAPI.Entity.HistorialEncomienda;
import upao.edu.pe.TurismoDiasAPI.Service.HistorialEncomiendaService;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/historialEncomiendas")
public class HistorialEncomiendaController {

    private final HistorialEncomiendaService historialEncomiendaService;

    // Método para listar el historial de una encomienda específica
    @GetMapping("/historialEncomienda/{idEncomienda}")
    public List<HistorialEncomienda> listarHistorialPorEncomiendaId(@PathVariable Long idEncomienda) {
        return historialEncomiendaService.listarHistorialPorEncomiendaId(idEncomienda);
    }
}
