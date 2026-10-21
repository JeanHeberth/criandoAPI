package br.com.criandoapi.config;

import java.util.Set;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

public final class Paginacao {
    private Paginacao() {}

    public static Pageable criar(int page, int size, String sort, String campoPadrao,
                                  Sort.Direction direcaoPadrao, Set<String> camposPermitidos) {
        if (page < 0 || size < 1 || size > 100) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "page deve ser >= 0 e size deve estar entre 1 e 100");
        }
        String valor = sort == null || sort.isBlank() ? campoPadrao + "," + direcaoPadrao : sort;
        String[] partes = valor.split(",", -1);
        if (partes.length != 2 || !camposPermitidos.contains(partes[0].trim())
                || !(partes[1].trim().equalsIgnoreCase("asc") || partes[1].trim().equalsIgnoreCase("desc"))) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "sort deve usar campo permitido e direcao asc ou desc. Campos: " + String.join(", ", new java.util.TreeSet<>(camposPermitidos)));
        }
        return PageRequest.of(page, size, Sort.by(Sort.Direction.fromString(partes[1].trim()), partes[0].trim()));
    }
}
