package br.com.criandoapi.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;

import java.io.IOException;
import java.util.Map;

public final class JsonErrorWriter {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private JsonErrorWriter() {
    }

    public static void write(HttpServletResponse response, int status, String mensagem) throws IOException {
        response.setStatus(status);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        OBJECT_MAPPER.writeValue(response.getWriter(), Map.of("mensagem", mensagem));
    }
}
