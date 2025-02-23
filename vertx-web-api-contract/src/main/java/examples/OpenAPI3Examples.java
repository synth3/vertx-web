package examples;

import io.swagger.v3.parser.core.models.AuthorizationValue;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.auth.jwt.JWTAuth;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.api.RequestParameter;
import io.vertx.ext.web.api.RequestParameters;
import io.vertx.ext.web.api.contract.RouterFactoryOptions;
import io.vertx.ext.web.api.contract.openapi3.OpenAPI3RouterFactory;
import io.vertx.ext.web.api.validation.ValidationException;
import io.vertx.ext.web.handler.JWTAuthHandler;

import java.util.Collections;
import java.util.List;

public class OpenAPI3Examples {

  public void constructRouterFactory(Vertx vertx) {
    OpenAPI3RouterFactory.create(vertx, "src/main/resources/petstore.yaml").onComplete(ar -> {
      if (ar.succeeded()) {
        // Spec loaded with success
        OpenAPI3RouterFactory routerFactory = ar.result();
      } else {
        // Something went wrong during router factory initialization
        Throwable exception = ar.cause();
      }
    });
  }

  public void constructRouterFactoryFromUrl(Vertx vertx) {
    OpenAPI3RouterFactory.create(
      vertx,
      "https://raw.githubusercontent.com/OAI/OpenAPI-Specification/master/examples/v3.0/petstore.yaml").onComplete(
      ar -> {
        if (ar.succeeded()) {
          // Spec loaded with success
          OpenAPI3RouterFactory routerFactory = ar.result();
        } else {
          // Something went wrong during router factory initialization
          Throwable exception = ar.cause();
        }
      });
  }

  public void constructRouterFactoryFromUrlWithAuthenticationHeader(Vertx vertx) {
    AuthorizationValue authorizationValue = new AuthorizationValue()
      .type("header")
      .keyName("Authorization")
      .value("Bearer xx.yy.zz");
    List<JsonObject> authorizations = Collections.singletonList(JsonObject.mapFrom(authorizationValue));
    OpenAPI3RouterFactory.create(
      vertx,
      "https://raw.githubusercontent.com/OAI/OpenAPI-Specification/master/examples/v3.0/petstore.yaml",
      authorizations).onComplete(
      ar -> {
        if (ar.succeeded()) {
          // Spec loaded with success
          OpenAPI3RouterFactory routerFactory = ar.result();
        } else {
          // Something went wrong during router factory initialization
          Throwable exception = ar.cause();
        }
      });
  }

  public void addRoute(Vertx vertx, OpenAPI3RouterFactory routerFactory) {
    routerFactory.addHandlerByOperationId("awesomeOperation", routingContext -> {
      RequestParameters params = routingContext.get("parsedParameters");
      RequestParameter body = params.body();
      JsonObject jsonBody = body.getJsonObject();
      // Do something with body
    });
    routerFactory.addFailureHandlerByOperationId("awesomeOperation", routingContext -> {
      // Handle failure
    });
  }

  public void addSecurityHandler(OpenAPI3RouterFactory routerFactory, Handler securityHandler) {
    routerFactory.addSecurityHandler("security_scheme_name", securityHandler);
  }

  public void addJWT(OpenAPI3RouterFactory routerFactory, JWTAuth jwtAuthProvider) {
    routerFactory.addSecurityHandler("jwt_auth", JWTAuthHandler.create(jwtAuthProvider));
  }

  public void addOperationModelKey(OpenAPI3RouterFactory routerFactory, RouterFactoryOptions options) {
    // Configure the operation model key and set options in router factory
    options.setOperationModelKey("operationPOJO");
    routerFactory.setOptions(options);

    // Add an handler that uses the operation model
    routerFactory.addHandlerByOperationId("listPets", routingContext -> {
      io.swagger.v3.oas.models.Operation operation = routingContext.get("operationPOJO");

      routingContext
        .response()
        .setStatusCode(200)
        .setStatusMessage("OK")
        // Write the response with operation id "listPets"
        .end(operation.getOperationId());
    });
  }

  public void generateRouter(Vertx vertx, OpenAPI3RouterFactory routerFactory) {
    Router router = routerFactory.getRouter();

    HttpServer server = vertx.createHttpServer(new HttpServerOptions().setPort(8080).setHost("localhost"));
    server.requestHandler(router).listen();
  }

  public void mainExample(Vertx vertx) {
    // Load the api spec. This operation is asynchronous
    OpenAPI3RouterFactory.create(vertx, "src/main/resources/petstore.yaml").onComplete(
      openAPI3RouterFactoryAsyncResult -> {
      if (openAPI3RouterFactoryAsyncResult.succeeded()) {
        // Spec loaded with success, retrieve the router
        OpenAPI3RouterFactory routerFactory = openAPI3RouterFactoryAsyncResult.result();
        // You can enable or disable different features of router factory through mounting RouterFactoryOptions
        // For example you can enable or disable the default failure handler for ValidationException
        RouterFactoryOptions options = new RouterFactoryOptions()
          .setMountValidationFailureHandler(false);
        // Mount the options
        routerFactory.setOptions(options);
        // Add an handler with operationId
        routerFactory.addHandlerByOperationId("listPets", routingContext -> {
          // Handle listPets operation
          routingContext.response().setStatusMessage("Called listPets").end();
        });
        // Add a failure handler to the same operationId
        routerFactory.addFailureHandlerByOperationId("listPets", routingContext -> {
          // This is the failure handler
          Throwable failure = routingContext.failure();
          if (failure instanceof ValidationException)
            // Handle Validation Exception
            routingContext.response().setStatusCode(400).setStatusMessage("ValidationException thrown! " + (
              (ValidationException) failure).type().name()).end();
        });

        // Add a security handler
        // Handle security here
        routerFactory.addSecurityHandler("api_key", RoutingContext::next);

        // Now you have to generate the router
        Router router = routerFactory.getRouter();

        // Now you can use your Router instance
        HttpServer server = vertx.createHttpServer(new HttpServerOptions().setPort(8080).setHost("localhost"));
        server.requestHandler(router).listen();

      } else {
        // Something went wrong during router factory initialization
        Throwable exception = openAPI3RouterFactoryAsyncResult.cause();
      }
    });
  }
}
