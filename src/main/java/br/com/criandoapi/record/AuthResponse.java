package br.com.criandoapi.record;

public record AuthResponse(String token, String tipo, UsuarioResponse usuario) {
}

