package io.vertx.example.todo.verticles;

import io.netty.handler.codec.http.HttpResponseStatus;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.example.todo.domain.ToDoItem;
import io.vertx.example.todo.utils.RedisUtils;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.CorsHandler;
import io.vertx.redis.RedisClient;
import io.vertx.redis.RedisOptions;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

public class ToDoVerticle extends AbstractVerticle {
	private static final Logger LOGGER = LoggerFactory.getLogger(ToDoVerticle.class);

	private final String TODO_URL = "/todo";
	private final String TODO_ID_URL = "/todo/:id";

	private static final String KEYS = "keys";
	private static final String INDEX = "index";

	RedisClient client;
	private Router router;

	@Override
	public void start() throws Exception {
		init();
		setRoutes();
		startServer();
	}

	private void init() {
		router = Router.router(vertx);
		client = RedisClient.create(vertx, new RedisOptions().setAddress("127.0.0.1").setPort(6379));
		setupCORS();
	}

	private void setRoutes() {
		/*
		 *  HttpMethod is defined for route and a handler is assigned
         */

		router.get(TODO_URL).handler(this::getAllToDo);
		router.delete(TODO_URL).handler(this::clearAllToDo);
		router.post(TODO_URL).handler(this::createToDo);

		router.get(TODO_ID_URL).handler(this::getToDo);
		router.delete(TODO_ID_URL).handler(this::deleteToDo);
		router.put(TODO_ID_URL).handler(this::updateToDo);
		router.patch(TODO_ID_URL).handler(this::updateToDo);
	}

	private void startServer() {
		Integer port = Integer.getInteger("http.port");
		port = (port == null) ? 8000 : port;
		vertx.createHttpServer()
				.requestHandler(router::accept)
				.listen(port, System.getProperty("http.address", "0.0.0.0"));
	}


	private void setupCORS() {
		Set<HttpMethod> toDoUrlMethodSet = new HashSet<>(Arrays.asList(HttpMethod.GET,
				HttpMethod.DELETE, HttpMethod.POST, HttpMethod.PATCH, HttpMethod.OPTIONS));

		Set<HttpMethod> toDoIdUrlMethodSet = new HashSet<>(Arrays.asList(HttpMethod.GET,
				HttpMethod.DELETE, HttpMethod.PATCH, HttpMethod.OPTIONS, HttpMethod.PUT));

		router.route(TODO_URL).handler(CorsHandler.create("*")
				.allowedMethods(toDoUrlMethodSet)
				.allowedHeader("Content-Type"));

		router.route(TODO_ID_URL).handler(CorsHandler.create("*")
				.allowedMethods(toDoIdUrlMethodSet)
				.allowedHeader("Content-Type"));
	}

	/*
	 * createToDo's implementation is invoked when the application receives a Http post
	 * on the relative route "/todo"
	 */
	private void createToDo(RoutingContext context) {
		HttpServerRequest req = context.request();
		HttpServerResponse response = context.response();

		req.bodyHandler(
				buffer -> {
					ToDoItem item = Json.decodeValue(buffer.getString(0, buffer.length()), ToDoItem.class);
					item.setUrl(context.request().absoluteURI());

					// Maintain and increment an index, and use it to uniquely identify a todo item
					client.incr(INDEX, incrEvent -> {
								if (incrEvent.succeeded()) {
									String index = incrEvent.result().toString();
									item.setUrl(item.getUrl() + "/" + index);
									client.multi(multiEvent ->
											//Insert the todoItem
											client.hmset(index, ToDoItem.toJsonObject(item), hmsetEvent ->
													//Maintain a list of index
													client.rpush(KEYS, index, rpushEvent ->
															client.exec(event -> {
																if (event.succeeded())
																	response.setStatusCode(HttpResponseStatus.CREATED.code())
																			.putHeader("content-type", "application/json; charset=utf-8")
																			.end(Json.encode(item));
																else {
																	response.setStatusCode(HttpResponseStatus.NO_CONTENT.code())
																			.end();
																	logError("Todo creation failed.", event.cause());
																}
															})
													)
											)
									);
								} else {
									response.setStatusCode(HttpResponseStatus.NO_CONTENT.code())
											.end();
								}
							}
					);
				}
		);
	}


    /*
	 * Echos JsonArray of todolist items
     */

	private void getAllToDo(RoutingContext context) {
		client.lrange(KEYS, 0, -1, lrangeEvent ->
				RedisUtils.getHashes(client, lrangeEvent.result().getList(), jsonArray ->
						context.response()
								.setStatusCode(HttpResponseStatus.OK.code())
								.putHeader("content-type", "application/json; charset=utf-8")
								/*
								  Apparently boolean and integer values in jsonArray are as strings, needs type conversion, hence we
                                  deserialize it to List<ToDoItem> and encode it as JSON! They get type casted automatically.
                                 */

								.end(Json.encode(jsonArray.getList()
										.stream()
										.map(element -> Json.decodeValue(element.toString(), ToDoItem.class))
										.collect(Collectors.toList()))
								)
				)
		);

	}


	private void getToDo(RoutingContext context) {
		HttpServerResponse response = context.response();
		String toDoId = context.request().getParam("id");
		client.hgetall(toDoId, event -> {
			if (event.succeeded() && event.result().size() > 0) {
				ToDoItem toDoItem = Json.decodeValue(event.result().encode(), ToDoItem.class);
				response.setStatusCode(HttpResponseStatus.OK.code())
						.putHeader("content-type", "application/json; charset=utf-8")
						.end(Json.encode(toDoItem));
			} else {
				response.setStatusCode(HttpResponseStatus.NOT_FOUND.code())
						.end();
				logError("Todo for id: " + toDoId + " not found. Read failed.", event.cause());
			}
		});
	}


	private void clearAllToDo(RoutingContext context) {
		client.del(KEYS, delEvent -> {
			if (delEvent.succeeded()) {
				client.flushdb(flushdbEvent -> {
					if (flushdbEvent.succeeded() && flushdbEvent.result().equals("OK")) {
						context.response().setStatusCode(HttpResponseStatus.NO_CONTENT.code())
								.end();
					} else {
						context.response().setStatusCode(HttpResponseStatus.INTERNAL_SERVER_ERROR.code())
								.end();
						logError("FLUSH DB failed.", flushdbEvent.cause());
					}
				});
			}
		});

	}

	/*
	 * Deletes todo for the requested url
	 */
	private void deleteToDo(RoutingContext context) {
		String toDoId = context.request().getParam("id");
		client.multi(multiEvent ->
				client.lrem(KEYS, 1, toDoId, lremEvent ->
						client.del(toDoId, event ->
								client.exec(execEvent -> {
									JsonArray result = execEvent.result();
									if (execEvent.succeeded() && result.getInteger(0) == 1 && result.getInteger(1) == 1)
										context.response().setStatusCode(HttpResponseStatus.NO_CONTENT.code())
												.end();
									else {
										context.response().setStatusCode(HttpResponseStatus.NOT_FOUND.code())
												.end();
										logError("Todo for id: " + toDoId + " not found. Delete failed.", execEvent.cause());

									}
								})
						)
				)
		);
	}

	/*
	 * Updates to do for the requested url
	 */
	private void updateToDo(RoutingContext context) {

		HttpServerRequest req = context.request();
		req.bodyHandler(buffer -> {
			String toDoId = context.request().getParam("id");
			JsonObject jsonObject = new JsonObject(buffer.getString(0, buffer.length()));
			client.hmset(toDoId, jsonObject, hmsetEvent -> {
				if (hmsetEvent.succeeded() && hmsetEvent.result().equals("OK")) {
					client.hgetall(toDoId, hgetAllEvent -> {
						if (hgetAllEvent.succeeded() && hgetAllEvent.result().size() > 0) {
							ToDoItem toDo = Json.decodeValue(hgetAllEvent.result().toString(), ToDoItem.class);
							context.response().setStatusCode(HttpResponseStatus.OK.code())
									.putHeader("content-type", "application/json; charset=utf-8")
									.end(Json.encode(toDo));
						} else {
							context.response().setStatusCode(HttpResponseStatus.NO_CONTENT.code())
									.end();
							logError("Todo for id: " + toDoId + " not found. Retrieval after Update failed.", hgetAllEvent.cause());

						}
					});
				} else {
					context.response().setStatusCode(HttpResponseStatus.NOT_FOUND.code()).end();
					logError("Todo for id: " + toDoId + " not found. Update failed.", hmsetEvent.cause());
				}
			});
		});
	}

	private void logError(String msg, Throwable throwable) {
		LOGGER.error(msg, throwable);
	}

}
