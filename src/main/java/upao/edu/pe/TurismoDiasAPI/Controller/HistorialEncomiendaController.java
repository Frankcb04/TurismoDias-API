package upao.edu.pe.TurismoDiasAPI.Controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import upao.edu.pe.TurismoDiasAPI.Entity.HistorialEncomienda;
import upao.edu.pe.TurismoDiasAPI.Service.HistorialEncomiendaService;

import java.util.List;

@RestController
@RequestMapping("/api/historialEncomienda")
public class HistorialEncomiendaController {

    @Autowired
    HistorialEncomiendaService historialEncomiendaService;

    // Método para listar el historial de una encomienda específica
    @GetMapping("/listarHistorialEncomienda/{id_encomienda}")
    public List<HistorialEncomienda> listarHistorialPorEncomiendaId(@PathVariable("id_encomienda") Integer id_encomienda) {
        return historialEncomiendaService.listarHistorialPorEncomiendaId(id_encomienda);
    }

    @GetMapping("/obtenerUltimoHistorial/{id_encomienda}")
    public HistorialEncomienda obtenerUltimoHistorial(@PathVariable("id_encomienda") Integer id_encomienda) {
        return historialEncomiendaService.listarHistorialPorEncomiendaId(id_encomienda).getFirst();
    }
}
