package io.vertx.example.todo.domain;

import io.vertx.core.json.JsonObject;

import java.util.HashMap;
import java.util.Map;

/**
 * POJO class to store to-do item information
 */
public class ToDoItem {
    private String title;
    private boolean completed;
    private String url;
    private Integer order;

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public boolean getCompleted() {
        return completed;
    }

    public void setCompleted(boolean completed) {
        this.completed = completed;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public Integer getOrder() {
        return order;
    }

    public void setOrder(Integer order) {
        this.order = order;
    }

    public static JsonObject toJsonObject(ToDoItem item) {
        return new JsonObject()
                .put("title", item.getTitle())
                .put("completed", item.completed)
                .put("url", item.getUrl())
                .put("order", item.getOrder());

    }
}
