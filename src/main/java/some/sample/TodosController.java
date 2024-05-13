
package some.sample;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;
import io.micrometer.tracing.Tracer;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import some.sample.NotFoundException;
import some.sample.Todo;
import some.sample.TodosService;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import static org.springframework.http.HttpStatus.NO_CONTENT;

@RestController
@RequestMapping("/api/v1/todos")
@RequiredArgsConstructor
@Slf4j
public class TodosController {

    @Autowired
    Tracer tracer;

    private static final String DEFAULT_MEDIA_TYPE =
        MediaType.APPLICATION_JSON_VALUE;

    private final TodosService service;


//    @GetMapping(value = { "/name" }, produces = DEFAULT_MEDIA_TYPE)
//    public List<String> findAllName() {
//        log.info("api=findAllName");
//        return List.of("Hello", "World");
//    }


    @GetMapping(produces = DEFAULT_MEDIA_TYPE)
    public Mono<Collection<Todo>> findAll() {
        return Mono.fromSupplier(() -> {
            log.info("api=controller.findAll 123");
            return service.findAll()
                .stream()
                .collect(Collectors.toList());
        });
    }

    @GetMapping(value = "/{id}", produces = DEFAULT_MEDIA_TYPE)
    public Todo findById(@PathVariable("id") final Long id) {
        log.info("api=findById");
        // Action
        return service.findById(id)
            .orElseThrow(NotFoundException::new);
    }

    @PostMapping(consumes = DEFAULT_MEDIA_TYPE)
    public ResponseEntity<Todo> create(final @Valid @RequestBody Todo item) {
        log.info("api=create");
        final var newTodo = service.create(item);
        return ResponseEntity.ok(newTodo);
    }

    @PutMapping
    @ResponseStatus(NO_CONTENT)
    public void update(@Valid @RequestBody final Todo item) {
        log.info("api=update");
        service.update(item);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(NO_CONTENT)
    public void delete(@PathVariable("id") final Long id) {
        log.info("api=delete");
        service.delete(id);
    }

}
