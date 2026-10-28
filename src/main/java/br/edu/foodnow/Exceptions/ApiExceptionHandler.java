package br.edu.foodnow.Exceptions;

import br.edu.foodnow.controller.ApiDtos;
import br.edu.foodnow.controller.ApiDtos.ErroResponse;
import br.edu.foodnow.service.RecursoNaoEncontradoException;
import br.edu.foodnow.service.RegraNegocioException;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(RecursoNaoEncontradoException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ApiDtos.ErroResponse naoEncontrado(RecursoNaoEncontradoException excecao) {
        return new ApiDtos.ErroResponse(404, "NOT_FOUND", excecao.getMessage());
    }

    @ExceptionHandler({RegraNegocioException.class, IllegalArgumentException.class, IllegalStateException.class})
    @ResponseStatus(HttpStatus.UNPROCESSABLE_ENTITY)
    public ApiDtos.ErroResponse regraNegocio(RuntimeException excecao) {
        return new ApiDtos.ErroResponse(422, "REGRA_NEGOCIO", excecao.getMessage());
    }

    @ExceptionHandler(ServicoLocalizacaoIndisponivelException.class)
    @ResponseStatus(HttpStatus.SERVICE_UNAVAILABLE)
    public ApiDtos.ErroResponse servicoIndisponivel(ServicoLocalizacaoIndisponivelException excecao) {
        return new ApiDtos.ErroResponse(503, "SERVICO_INDISPONIVEL", excecao.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiDtos.ErroResponse validacao(MethodArgumentNotValidException excecao) {
        String mensagem = excecao.getBindingResult().getFieldErrors().stream().findFirst()
                .map(erro -> erro.getField() + ": " + erro.getDefaultMessage())
                .orElse("Requisição inválida");
        return new ApiDtos.ErroResponse(400, "VALIDATION_ERROR", mensagem);
    }
}
