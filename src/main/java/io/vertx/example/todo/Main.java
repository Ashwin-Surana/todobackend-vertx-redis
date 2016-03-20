package io.vertx.example.todo;

import io.vertx.core.Vertx;
import io.vertx.example.todo.verticles.ToDoVerticle;

/**
 * Created by ashwin on 21/03/16.
 */
public class Main {
    public static  void  main(String[] args){
        Vertx vertx = Vertx.vertx();
        vertx.deployVerticle(new ToDoVerticle());
    }

}
