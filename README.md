This project is created to show case diff between Otel and Brave tracing implementation.

With Otel impl, it seems to have problem when we add Baggages. It has trouble in clearing TracerContext for the thread, that means traceId and baggages for same thread will always be same even if this thread carries new HttpRequest. 

Steps for otel
- Uncomment `micrometer-tracing-bridge-otel` dependnecy in pom.xml
- Comment out `micrometer-tracing-bridge-brave` dependency in pom.xml
- Run app `mvn clean install spring-boot:run `
- Hit below requests one by one and observe logs : -
<br> `curl -v http://localhost:8080/api/v1/todos`
<br> `curl -v http://localhost:8080/api/v1/todos/1`
- TraceId for both the requests would be same (it should be different for each req)
- Also for the log event `LogBeforeAddingBaggage`, first request wont print any baggage (which is expected), but for second request it will print baggages of previous req (look for uri baggage)


Steps for Brave
- Comment out `micrometer-tracing-bridge-otel` dependnecy in pom.xml
- Uncomment `micrometer-tracing-bridge-brave` dependency in pom.xml
- Run app `mvn clean install spring-boot:run `
- Hit below requests one by one and observe logs : -
<br> `curl -v http://localhost:8080/api/v1/todos`
<br> `curl -v http://localhost:8080/api/v1/todos/1`
- Observe traceId in logs its changing with every new request (which is expected).
- Baggages are also cleared well for log event `LogBeforeAddingBaggage`


Note : for the sake of easy testing, we have reduced the number of Netty threads to 1, so that we can easily observer the behavior with one thread. If anybody wants to test with regular number of threads then turn off below flag <br>
`netty.threadConfig.enabled=false`


