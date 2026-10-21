package br.com.criandoapi.config;

import br.com.criandoapi.entity.Usuario;
import br.com.criandoapi.entity.UsuarioRole;
import br.com.criandoapi.repository.UsuarioRepository;
import br.com.criandoapi.security.PasswordUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/** Cria o primeiro administrador a partir de API_ADMIN_EMAIL / API_ADMIN_SENHA, se ainda nao existir. */
@Component
public class AdminInicialInitializer implements ApplicationRunner {

    private final UsuarioRepository usuarioRepository;
    private final String email;
    private final String senha;
    private final String nome;

    public AdminInicialInitializer(UsuarioRepository usuarioRepository,
                                   @Value("${api.admin.email:}") String email,
                                   @Value("${api.admin.senha:}") String senha,
                                   @Value("${api.admin.nome:Administrador}") String nome) {
        this.usuarioRepository = usuarioRepository;
        this.email = email;
        this.senha = senha;
        this.nome = nome;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (email.isBlank() || senha.isBlank() || usuarioRepository.findByEmail(email).isPresent()) {
            return;
        }
        usuarioRepository.save(new Usuario(nome, email, PasswordUtils.hash(senha), UsuarioRole.ADMIN));
    }
}
