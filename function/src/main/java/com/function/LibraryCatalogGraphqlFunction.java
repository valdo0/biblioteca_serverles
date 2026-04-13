package com.function;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.microsoft.azure.functions.ExecutionContext;
import com.microsoft.azure.functions.HttpMethod;
import com.microsoft.azure.functions.HttpRequestMessage;
import com.microsoft.azure.functions.HttpResponseMessage;
import com.microsoft.azure.functions.HttpStatus;
import com.microsoft.azure.functions.annotation.AuthorizationLevel;
import com.microsoft.azure.functions.annotation.FunctionName;
import com.microsoft.azure.functions.annotation.HttpTrigger;
import graphql.ExecutionInput;
import graphql.ExecutionResult;
import graphql.GraphQL;
import graphql.schema.GraphQLSchema;
import graphql.schema.idl.RuntimeWiring;
import graphql.schema.idl.SchemaGenerator;
import graphql.schema.idl.SchemaParser;
import graphql.schema.idl.TypeDefinitionRegistry;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

public class LibraryCatalogGraphqlFunction {

    private final GraphQL graphQL;
    private final ObjectMapper mapper = new ObjectMapper();

    public LibraryCatalogGraphqlFunction() {
        String schema =
                "type Query {\n" +
                "  books(disponible: Boolean, autor: String): [Book]\n" +
                "  book(id: ID!): Book\n" +
                "}\n" +
                "type Book {\n" +
                "  id: ID!\n" +
                "  titulo: String\n" +
                "  autor: String\n" +
                "  isbn: String\n" +
                "  cantidad: Int\n" +
                "  disponible: Boolean\n" +
                "}";

        SchemaParser schemaParser = new SchemaParser();
        TypeDefinitionRegistry typeDefinitionRegistry = schemaParser.parse(schema);

        RuntimeWiring runtimeWiring = RuntimeWiring.newRuntimeWiring()
                .type("Query", builder -> builder
                    .dataFetcher("books", env -> {
                        Boolean disponible = env.getArgument("disponible");
                        String autor = env.getArgument("autor");
                        String baseUrl = System.getenv("MS_BIBLIOTECA_URL") != null ?
                                         System.getenv("MS_BIBLIOTECA_URL") : "http://localhost:8083";

                        try {
                            java.net.http.HttpClient client = java.net.http.HttpClient.newHttpClient();
                            java.net.http.HttpRequest httpRequest = java.net.http.HttpRequest.newBuilder()
                                    .uri(java.net.URI.create(baseUrl + "/libros"))
                                    .GET()
                                    .build();

                            java.net.http.HttpResponse<String> response =
                                    client.send(httpRequest, java.net.http.HttpResponse.BodyHandlers.ofString());

                            if (response.statusCode() == 200) {
                                List<Map<String, Object>> catalog = mapper.readValue(
                                    response.body(),
                                    new TypeReference<List<Map<String, Object>>>() {}
                                );

                                // Filtrar por disponible si se especifica
                                if (disponible != null) {
                                    catalog = catalog.stream()
                                            .filter(b -> {
                                                Object dispObj = b.get("disponible");
                                                return dispObj != null && dispObj.equals(disponible);
                                            })
                                            .collect(Collectors.toList());
                                }

                                // Filtrar por autor si se especifica
                                if (autor != null && !autor.isEmpty()) {
                                    catalog = catalog.stream()
                                            .filter(b -> {
                                                Object autorObj = b.get("autor");
                                                return autorObj != null &&
                                                       autorObj.toString().toLowerCase().contains(autor.toLowerCase());
                                            })
                                            .collect(Collectors.toList());
                                }

                                return catalog;
                            } else {
                                return List.of();
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                            return List.of();
                        }
                    })
                    .dataFetcher("book", env -> {
                        String id = env.getArgument("id");
                        String baseUrl = System.getenv("MS_BIBLIOTECA_URL") != null ?
                                         System.getenv("MS_BIBLIOTECA_URL") : "http://localhost:8083";

                        try {
                            java.net.http.HttpClient client = java.net.http.HttpClient.newHttpClient();
                            java.net.http.HttpRequest httpRequest = java.net.http.HttpRequest.newBuilder()
                                    .uri(java.net.URI.create(baseUrl + "/libros/" + id))
                                    .GET()
                                    .build();

                            java.net.http.HttpResponse<String> response =
                                    client.send(httpRequest, java.net.http.HttpResponse.BodyHandlers.ofString());

                            if (response.statusCode() == 200) {
                                return mapper.readValue(
                                    response.body(),
                                    new TypeReference<Map<String, Object>>() {}
                                );
                            } else {
                                return null;
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                            return null;
                        }
                    })
                )
                .build();

        SchemaGenerator schemaGenerator = new SchemaGenerator();
        GraphQLSchema graphQLSchema = schemaGenerator.makeExecutableSchema(typeDefinitionRegistry, runtimeWiring);
        
        this.graphQL = GraphQL.newGraphQL(graphQLSchema).build();
    }

    @FunctionName("LibraryCatalogGraphql")
    public HttpResponseMessage run(
            @HttpTrigger(name = "req", methods = {HttpMethod.POST}, authLevel = AuthorizationLevel.ANONYMOUS, route = "graphql/catalog") HttpRequestMessage<Optional<String>> request,
            final ExecutionContext context) {

        context.getLogger().info("Ejecutando consulta GraphQL de Catálogo de Libros (Endpoint 2)");

        if (!request.getBody().isPresent()) {
            return request.createResponseBuilder(HttpStatus.BAD_REQUEST).body("Body cannot be empty").build();
        }

        try {
            String bodyString = request.getBody().get();
            Map<String, Object> body = mapper.readValue(bodyString, new TypeReference<Map<String, Object>>() {});
            
            String query = (String) body.get("query");
            Map<String, Object> variables = (Map<String, Object>) body.get("variables");

            ExecutionInput.Builder executionInputBuilder = ExecutionInput.newExecutionInput().query(query);
            if (variables != null) {
                executionInputBuilder.variables(variables);
            }

            ExecutionResult result = graphQL.execute(executionInputBuilder.build());
            String jsonResult = mapper.writeValueAsString(result.toSpecification());

            return request.createResponseBuilder(HttpStatus.OK)
                    .header("Content-Type", "application/json")
                    .body(jsonResult)
                    .build();

        } catch (Exception e) {
            context.getLogger().severe("Error procesando GraphQL de catálogo: " + e.getMessage());
            return request.createResponseBuilder(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error parsing GraphQL").build();
        }
    }
}
