package some.sample;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static java.util.stream.Collectors.toList;

import java.util.ArrayList;

/**
 * Ein Service ist ein Singleton auf Control Layer, der in der Boundary von mehreren (REST) Controllern gemeinsam genutzt werden kann.
 * Dieser hat keinen Bezug mehr zu HTTP.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TodosService {

//    private final TodoEntityMapper mapper;
//    private final TodosRepository repo;
    
    private final static Map<Long, Todo> todos= new HashMap<>();
    
    static {
        todos.put(1L, new Todo(1L, "one"));
        todos.put(2L, new Todo(2L, "two"));
        todos.put(3L, new Todo(3L, "three"));
    }

    /**
     * Gibt die Anzahl an Datensätzen zurück.
     * @return die Anzahl an Datensätzen
     */
    long count() {
        return todos.size();// repo.count();
    }

    /**
     * Gibt alle Todos zurück.
     *
     * @return eine unveränderliche Collection
     */
    public Collection<Todo> findAll() {
        log.info("api=service.findAll");
        return todos.values();
    }

    /**
     * Durchsucht die Todos nach einer ID.
     *
     * @param id die ID
     * @return das Suchergebnis
     */
    public Optional<Todo> findById(long id) {
        return Optional.ofNullable(todos.get(id));
    }

    /**
     * Fügt ein Item in den Datenbestand hinzu. Dabei wird eine ID generiert.
     *
     * @param item das anzulegende Item (ohne ID)
     * @return das gespeicherte Item (mit ID)
     * @throws IllegalArgumentException wenn das Item null oder die ID bereits belegt ist
     */
    public Todo create(Todo item) {
        if (null == item || null != item.id()) {
            throw new IllegalArgumentException("item must exist without any id");
        }
        todos.put(item.id(), item);
        return item;
    }

    /**
     * Aktualisiert ein Item im Datenbestand.
     *
     * @param item das zu ändernde Item mit ID
     * @throws IllegalArgumentException
     * wenn das Item oder dessen ID nicht belegt ist
     * @throws NotFoundException
     * wenn das Element mit der ID nicht gefunden wird
     */
    public void update(Todo item) {
        if (null == item || null == item.id()) {
            throw new IllegalArgumentException("item must exist with an id");
        }
        // remove separat, um nicht neue Einträge hinzuzufügen (put allein würde auch ersetzen)
        if (todos.containsKey(item.id())) {
            todos.put(item.id(), item);
        } else {
            throw new NotFoundException();
        }
    }

    /**
     * Entfernt ein Item aus dem Datenbestand.
     *
     * @param id die ID des zu löschenden Items
     * @throws NotFoundException
     * wenn das Element mit der ID nicht gefunden wird
     */
    public void delete(long id) {
        if (todos.containsKey(id)) {
            todos.remove(id);
        } else {
            throw new NotFoundException();
        }
    }

}
