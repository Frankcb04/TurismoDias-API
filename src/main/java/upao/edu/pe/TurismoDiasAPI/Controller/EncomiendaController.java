package upao.edu.pe.TurismoDiasAPI.Controller;

import lombok.RequiredArgsConstructor;
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
    @GetMapping("/listarEncomienda/{id}")
    public Optional<Encomienda> obtenerEncomiendaPorId(@PathVariable Long idEncomienda) {
        return encomiendaService.obtenerEncomiendaPorId(idEncomienda);
    }
}