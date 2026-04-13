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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

public class UserProfileGraphqlFunction {

    private final GraphQL graphQL;
    private final ObjectMapper mapper = new ObjectMapper();

    public UserProfileGraphqlFunction() {
        String schema = 
                "type Query {\n" +
                "  user(id: ID!): User\n" +
                "}\n" +
                "type User {\n" +
                "  id: ID!\n" +
                "  nombre: String\n" +
                "  email: String\n" +
                "  rol: Role\n" +
                "  librosPrestados: [BorrowedBook]\n" +
                "}\n" +
                "type Role {\n" +
                "  nombre: String\n" +
                "  permisos: [String]\n" +
                "}\n" +
                "type BorrowedBook {\n" +
                "  titulo: String\n" +
                "  fechaDevolucion: String\n" +
                "}";

        SchemaParser schemaParser = new SchemaParser();
        TypeDefinitionRegistry typeDefinitionRegistry = schemaParser.parse(schema);

        RuntimeWiring runtimeWiring = RuntimeWiring.newRuntimeWiring()
                .type("Query", builder -> builder.dataFetcher("user", env -> {
                    String userId = env.getArgument("id");
                    String msAuthUrl = System.getenv("MS_AUTH_URL") != null ?
                                       System.getenv("MS_AUTH_URL") : "http://localhost:8082";

                    try {
                        java.net.http.HttpClient client = java.net.http.HttpClient.newHttpClient();
                        java.net.http.HttpRequest httpRequest = java.net.http.HttpRequest.newBuilder()
                                .uri(java.net.URI.create(msAuthUrl + "/auth/" + userId))
                                .GET()
                                .build();

                        java.net.http.HttpResponse<String> response =
                                client.send(httpRequest, java.net.http.HttpResponse.BodyHandlers.ofString());

                        if (response.statusCode() == 200) {
                            return mapper.readValue(response.body(), new TypeReference<Map<String, Object>>() {});
                        } else {
                            return Map.of("id", userId, "nombre", "Usuario no encontrado", "email", "N/A");
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                        return Map.of("id", userId, "nombre", "Error al consultar", "email", "N/A");
                    }
                }))
                .type("User", builder -> builder
                        .dataFetcher("rol", env -> {
                            Map<String, Object> usuario = env.getSource();
                            String rolNombre = (String) usuario.getOrDefault("rol", "LECTOR");

                            // Mapeo simple de permisos según el rol
                            List<String> permisos = new ArrayList<>();
                            switch (rolNombre) {
                                case "ADMINISTRADOR":
                                    permisos.addAll(List.of("READ_BOOK", "BORROW_BOOK", "MANAGE_BOOKS", "MANAGE_USERS"));
                                    break;
                                case "BIBLIOTECARIO":
                                    permisos.addAll(List.of("READ_BOOK", "BORROW_BOOK", "MANAGE_BOOKS"));
                                    break;
                                default: // LECTOR
                                    permisos.addAll(List.of("READ_BOOK", "BORROW_BOOK"));
                            }

                            return Map.of("nombre", rolNombre, "permisos", permisos);
                        })
                        .dataFetcher("librosPrestados", env -> {
                            Map<String, Object> usuario = env.getSource();
                            Object idObj = usuario.get("id");
                            int usuarioId = idObj instanceof Integer ? (Integer) idObj : Integer.parseInt(idObj.toString());

                            String msBibliotecaUrl = System.getenv("MS_BIBLIOTECA_URL") != null ?
                                                     System.getenv("MS_BIBLIOTECA_URL") : "http://localhost:8083";

                            try {
                                java.net.http.HttpClient client = java.net.http.HttpClient.newHttpClient();
                                java.net.http.HttpRequest httpRequest = java.net.http.HttpRequest.newBuilder()
                                        .uri(java.net.URI.create(msBibliotecaUrl + "/prestamos"))
                                        .GET()
                                        .build();

                                java.net.http.HttpResponse<String> response =
                                        client.send(httpRequest, java.net.http.HttpResponse.BodyHandlers.ofString());

                                if (response.statusCode() == 200) {
                                    List<Map<String, Object>> todosPrestamos = mapper.readValue(
                                        response.body(),
                                        new TypeReference<List<Map<String, Object>>>() {}
                                    );

                                    // Filtrar solo los préstamos de este usuario
                                    return todosPrestamos.stream()
                                            .filter(p -> {
                                                Object pUsuarioId = p.get("usuarioId");
                                                int prestamoUsuarioId = pUsuarioId instanceof Integer ?
                                                    (Integer) pUsuarioId :
                                                    Integer.parseInt(pUsuarioId.toString());
                                                return prestamoUsuarioId == usuarioId;
                                            })
                                            .map(p -> Map.of(
                                                "titulo", p.getOrDefault("libroTitulo", "Sin título"),
                                                "fechaDevolucion", p.getOrDefault("fechaDevolucion", "N/A")
                                            ))
                                            .collect(Collectors.toList());
                                }
                                return List.of();
                            } catch (Exception e) {
                                e.printStackTrace();
                                return List.of();
                            }
                        })
                )
                .build();

        SchemaGenerator schemaGenerator = new SchemaGenerator();
        GraphQLSchema graphQLSchema = schemaGenerator.makeExecutableSchema(typeDefinitionRegistry, runtimeWiring);
        
        this.graphQL = GraphQL.newGraphQL(graphQLSchema).build();
    }

    @FunctionName("UserProfileGraphql")
    public HttpResponseMessage run(
            @HttpTrigger(name = "req", methods = {HttpMethod.POST}, authLevel = AuthorizationLevel.ANONYMOUS, route = "graphql/profile") HttpRequestMessage<Optional<String>> request,
            final ExecutionContext context) {

        context.getLogger().info("Ejecutando consulta GraphQL de Perfil (Endpoint 1)");

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
            context.getLogger().severe("Error procesando GraphQL: " + e.getMessage());
            return request.createResponseBuilder(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error parsing GraphQL").build();
        }
    }
}
