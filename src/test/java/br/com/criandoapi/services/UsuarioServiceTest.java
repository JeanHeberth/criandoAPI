package br.com.criandoapi.services;

import br.com.criandoapi.entity.Usuario;
import br.com.criandoapi.entity.UsuarioRole;
import br.com.criandoapi.record.PageResponse;
import br.com.criandoapi.record.UsuarioAdminRequest;
import br.com.criandoapi.record.UsuarioResponse;
import br.com.criandoapi.record.UsuarioUpdateRequest;
import br.com.criandoapi.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UsuarioServiceTest {

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UsuarioService usuarioService;

    private Usuario usuario;

    @BeforeEach
    void setUp() {
        usuario = new Usuario("Maria", "maria@email.com", "hash-salva");
        usuario.setId(1L);
        usuario.setRole(UsuarioRole.USER);
    }

    @Test
    void deveCriarUsuarioAdminComSenhaCriptografada() {
        UsuarioAdminRequest request = new UsuarioAdminRequest("Maria", "maria@email.com", "123456", UsuarioRole.ADMIN);

        when(usuarioRepository.findByEmail("maria@email.com")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("123456")).thenReturn("hash-gerado");
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(invocation -> {
            Usuario salvo = invocation.getArgument(0);
            salvo.setId(1L);
            return salvo;
        });

        UsuarioResponse response = usuarioService.criarUsuarioAdmin(request);

        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.email()).isEqualTo("maria@email.com");

        ArgumentCaptor<Usuario> captor = ArgumentCaptor.forClass(Usuario.class);
        verify(usuarioRepository).save(captor.capture());
        assertThat(captor.getValue().getSenha()).isEqualTo("hash-gerado");
        assertThat(captor.getValue().getRole()).isEqualTo(UsuarioRole.ADMIN);
    }

    @Test
    void deveLancarConflitoAoCriarUsuarioComEmailDuplicado() {
        when(usuarioRepository.findByEmail("maria@email.com")).thenReturn(Optional.of(usuario));

        assertThatThrownBy(() -> usuarioService.criarUsuario("Maria", "maria@email.com", "123456", UsuarioRole.USER))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Email ja cadastrado");
    }

    @Test
    void deveListarUsuariosPaginados() {
        when(usuarioRepository.findAll(any(Pageable.class))).thenReturn(new PageImpl<>(List.of(usuario)));

        PageResponse<UsuarioResponse> response = usuarioService.listar(0, 20);

        assertThat(response.conteudo()).hasSize(1);
        assertThat(response.totalElementos()).isEqualTo(1);
    }

    @Test
    void deveAtualizarUsuarioSemAlterarSenhaQuandoNaoInformada() {
        UsuarioUpdateRequest request = new UsuarioUpdateRequest("Maria Silva", "maria@email.com", null);

        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuario));
        when(usuarioRepository.findByEmail("maria@email.com")).thenReturn(Optional.of(usuario));
        when(usuarioRepository.save(usuario)).thenReturn(usuario);

        UsuarioResponse response = usuarioService.atualizarUsuario(1L, request);

        assertThat(response.nome()).isEqualTo("Maria Silva");
        verify(passwordEncoder, never()).encode(anyString());
    }

    @Test
    void deveDeletarUsuarioExistente() {
        when(usuarioRepository.existsById(1L)).thenReturn(true);

        usuarioService.deletarUsuario(1L);

        verify(usuarioRepository).deleteById(1L);
    }
}
