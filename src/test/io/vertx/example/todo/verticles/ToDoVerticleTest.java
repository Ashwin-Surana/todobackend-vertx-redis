package io.vertx.example.todo.verticles;

import io.netty.handler.codec.http.HttpResponseStatus;
import io.vertx.core.Vertx;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.example.todo.domain.ToDoItem;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import redis.embedded.RedisServer;

@RunWith(VertxUnitRunner.class)
public class ToDoVerticleTest {
	private Vertx vertx;
	private static RedisServer redisServer;
	private final static int PORT = 8000;

	@Before
	public void setUp(TestContext context) throws Exception {
		redisServer = new RedisServer(6379);
		redisServer.start();
		vertx = Vertx.vertx();
		vertx.deployVerticle(new ToDoVerticle(), context.asyncAssertSuccess());
	}

	@After
	public void tearDown(TestContext context) {
		redisServer.stop();
		vertx.close(context.asyncAssertSuccess());
	}

	@Test
	public void testCreateToDo(TestContext context) {
		final Async async = context.async();
		ToDoItem todo = new ToDoItem("Test entry", false);
		vertx.createHttpClient().post(PORT, "localhost", "/todo",
				postResponse -> postResponse.handler(postResponseBody -> {
					ToDoItem createdTodo = toToDoItem(postResponseBody.toString());
					context.assertEquals(HttpResponseStatus.CREATED.code(), postResponse.statusCode());
					context.assertEquals(todo.getTitle(), createdTodo.getTitle());
					context.assertEquals(todo.getCompleted(), createdTodo.getCompleted());
					context.assertNotNull(createdTodo.getUrl());
					context.assertFalse(createdTodo.getUrl().isEmpty());
					async.complete();
				})
		).end(Json.encode(todo));
	}

	@Test
	public void testGetAllTodo(TestContext context) {
		final Async async = context.async();
		vertx.createHttpClient().getNow(PORT, "localhost", "/todo",
				response -> {
					context.assertEquals(HttpResponseStatus.OK.code(), response.statusCode());
					response.handler(body -> {
						context.assertEquals(body.toJsonArray(), new JsonArray());
						async.complete();
					});
				}
		);
	}

	@Test
	public void testGetToDo(TestContext context) {
		final Async async = context.async();
		ToDoItem todo = new ToDoItem("Test entry", false);
		vertx.createHttpClient().post(PORT, "localhost", "/todo",
				postResponse -> postResponse.handler(postResponseBody -> {
					ToDoItem createdTodo = toToDoItem(postResponseBody.toString());
					vertx.createHttpClient().getNow(PORT, "localhost", getRequestUri(createdTodo.getUrl()),
							getResponse -> getResponse.handler(getResponseBody -> {
								ToDoItem responseToDo = toToDoItem(getResponseBody.toString());
								context.assertEquals(HttpResponseStatus.OK.code(), getResponse.statusCode());
								context.assertEquals(createdTodo, responseToDo);
								async.complete();
							})
					);
				})
		).end(Json.encode(todo));
	}

	@Test
	public void testClearAllToDo(TestContext context) {
		final Async async = context.async();
		ToDoItem todo = new ToDoItem("Test entry", false);
		vertx.createHttpClient().post(PORT, "localhost", "/todo",
				postResponse -> postResponse.handler(postResponseBody -> {
					vertx.createHttpClient().delete(PORT, "localhost", "/todo", deleteResponse -> {
						context.assertEquals(HttpResponseStatus.NO_CONTENT.code(), deleteResponse.statusCode());
						vertx.createHttpClient().getNow(PORT, "localhost", "/todo",
								getResponse -> getResponse.handler(getResponseBody -> {
									context.assertEquals(getResponseBody.toJsonArray(), new JsonArray());
									async.complete();
								})
						);
					}).end();
				})
		).end(Json.encode(todo));
	}

	@Test
	public void testDeleteToDo(TestContext context) {
		final Async async = context.async();
		ToDoItem todo = new ToDoItem("Test entry", false);
		vertx.createHttpClient().post(PORT, "localhost", "/todo",
				postResponse -> postResponse.handler(postResponseBody -> {
					ToDoItem createdTodo = toToDoItem(postResponseBody.toString());
					String url = createdTodo.getUrl();
					vertx.createHttpClient().delete(PORT, "localhost", getRequestUri(url), deleteResponse -> {
						context.assertEquals(HttpResponseStatus.NO_CONTENT.code(), deleteResponse.statusCode());
						vertx.createHttpClient().getNow(PORT, "localhost", getRequestUri(url), getResponse -> {
							context.assertEquals(getResponse.statusCode(), HttpResponseStatus.NOT_FOUND.code());
							async.complete();
						});
					}).end();
				})
		).end(Json.encode(todo));
	}

	@Test
	public void testUpdateToDo(TestContext context) {
		final Async async = context.async();
		ToDoItem todo = new ToDoItem("Test entry", false);
		vertx.createHttpClient().post(PORT, "localhost", "/todo",
				postResponse -> postResponse.handler(postResponseBody -> {
					ToDoItem createdTodo = toToDoItem(postResponseBody.toString());
					String url = createdTodo.getUrl();
					vertx.createHttpClient().put(PORT, "localhost", getRequestUri(url), putResponse -> {
						context.assertEquals(putResponse.statusCode(), HttpResponseStatus.OK.code());
						putResponse.bodyHandler(putResponseBody -> {
							JsonObject updatedToDo = putResponseBody.toJsonObject();
							context.assertEquals("Title changed", updatedToDo.getString("title"));
							async.complete();
						});
					}).end(new JsonObject().put("title", "Title changed").toString());
				})
		).end(Json.encode(todo));
	}


	private ToDoItem toToDoItem(String json) {
		return Json.decodeValue(json, ToDoItem.class);
	}


	private String getRequestUri(String url) {
		//Url is in the form http://HOST:PORT/todos/{id}
		return "/" + url.split("/", 4)[3];
	}


}